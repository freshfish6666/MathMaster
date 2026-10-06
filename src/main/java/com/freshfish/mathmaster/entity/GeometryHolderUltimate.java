package com.freshfish.mathmaster.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Stationary spell cubes, owned by the boss; no entities, blocks or saved projectiles. */
public final class GeometryHolderUltimate {
    public static final int ACTIVE = 11, LIFETIME = 80, MAX_CUBES = 20;
    public static final double SIZE = .5, RANGE = 16;
    public static final float DAMAGE = 10;
    private final GeometryHolderEntity boss;
    private int cooldown = 400;
    private boolean secondPhaseCooldown;

    GeometryHolderUltimate(GeometryHolderEntity boss) { this.boss = boss; }

    static void define(SynchedEntityData.Builder builder) {
        for (var key : GeometryHolderEntity.ULTIMATE_OFFSETS) builder.define(key, new Vector3f());
        builder.define(GeometryHolderEntity.ULTIMATE_MASK, 0);
        for (var key : GeometryHolderEntity.ULTIMATE_EXTRA_OFFSETS) builder.define(key, new Vector3f());
    }
    public boolean cubeActive(int index) {
        return (mask() & (1 << index)) != 0;
    }
    public int cubeCount() { return Integer.bitCount(mask()); }
    private int mask() { return boss.getEntityData().get(GeometryHolderEntity.ULTIMATE_MASK); }
    public Vec3 cubePosition(int index) {
        var offset = boss.getEntityData().get(offsetKey(index));
        return boss.position().add(offset.x, offset.y, offset.z);
    }
    public AABB cubeBounds(int index) { return AABB.ofSize(cubePosition(index), SIZE, SIZE, SIZE); }
    public float brightness(float partial) {
        return net.minecraft.util.Mth.clamp((LIFETIME - ticks() + partial) / LIFETIME, 0, 1);
    }
    public int ticks() { return boss.getEntityData().get(GeometryHolderEntity.ULTIMATE_TICKS); }
    public boolean active() { return ticks()>0; }
    void clear() { boss.getEntityData().set(GeometryHolderEntity.ULTIMATE_MASK, 0); boss.getEntityData().set(GeometryHolderEntity.ULTIMATE_TICKS, 0); }
    void reset() { clear(); cooldown = 400; secondPhaseCooldown = false; }
    boolean ready() { return !active() && cooldown <= 0; }
    void tickCooldown(boolean fighting) {
        // Entering stage two shortens the remaining wait once, without interrupting a cast.
        if (boss.isSecondPhase() && !secondPhaseCooldown) {
            cooldown = Math.round(cooldown * .8F);
            secondPhaseCooldown = true;
        }
        if (fighting && !active() && cooldown > 0) cooldown--;
    }
    private void setCube(int index, Vec3 center) {
        var offset = center.subtract(boss.position());
        boss.getEntityData().set(offsetKey(index),
                new Vector3f((float)offset.x, (float)offset.y, (float)offset.z));
    }
    private static net.minecraft.network.syncher.EntityDataAccessor<Vector3f> offsetKey(int index) {
        return index < 12 ? GeometryHolderEntity.ULTIMATE_OFFSETS.get(index)
                : GeometryHolderEntity.ULTIMATE_EXTRA_OFFSETS.get(index - 12);
    }
    boolean start(LivingEntity target) {
        clear();
        int desired = boss.isSecondPhase() ? MAX_CUBES : 12, placed = 0;
        double rotation = boss.getRandom().nextDouble() * Math.PI * 2;
        for (int i=0; i<desired; i++) {
            for (int attempt=0; attempt<32; attempt++) {
                double angle = rotation + (i + boss.getRandom().nextDouble()) * Math.PI * 2 / desired;
                // Equal angular sectors with uniform area sampling, including the inner circle.
                // Reserve the half-diagonal so every cube corner stays inside the sixteen-block disk.
                double radius = Math.sqrt(boss.getRandom().nextDouble()) * (RANGE - SIZE / Math.sqrt(2));
                Vec3 center = boss.position().add(Math.cos(angle)*radius,
                        target.getY()-boss.getY() + .8 + boss.getRandom().nextDouble()*.8, Math.sin(angle)*radius);
                AABB bounds = AABB.ofSize(center, SIZE, SIZE, SIZE);
                if (!loaded(bounds) || !boss.level().noCollision(boss, bounds)) continue;
                if (!boss.level().getEntitiesOfClass(Player.class, bounds.inflate(.5), boss::eligible).isEmpty()) continue;
                boolean overlaps = false;
                for (int j=0; j<placed; j++) if (cubeBounds(j).inflate(.5).intersects(bounds)) { overlaps=true; break; }
                if (overlaps) continue;
                setCube(placed++, center);
                break;
            }
        }
        cooldown = boss.isSecondPhase() ? 320 : 400;
        if (placed == 0) return false;
        boss.getEntityData().set(GeometryHolderEntity.ULTIMATE_MASK, (1 << placed) - 1);
        boss.getEntityData().set(GeometryHolderEntity.ULTIMATE_TICKS, LIFETIME);
        GeometryHolderAudio.begin(boss,ACTIVE);
        boss.setDeltaMovement(Vec3.ZERO);
        return true;
    }
    private boolean loaded(AABB bounds) {
        for (int x=BlockPos.containing(bounds.minX,0,0).getX()>>4; x<=BlockPos.containing(bounds.maxX,0,0).getX()>>4; x++)
            for (int z=BlockPos.containing(0,0,bounds.minZ).getZ()>>4; z<=BlockPos.containing(0,0,bounds.maxZ).getZ()>>4; z++)
                if (!boss.level().hasChunkAt(new BlockPos(x<<4,0,z<<4))) return false;
        return true;
    }
    boolean advance(LivingEntity target) {
        if (!active()) return false;
        boss.setDeltaMovement(Vec3.ZERO);
        if (target == null) { boss.finishUltimate(); return true; }
        int remaining = ticks() - 1;
        boss.getEntityData().set(GeometryHolderEntity.ULTIMATE_TICKS, remaining);
        int detonate = 0;
        for (int i=0; i<MAX_CUBES; i++) if (cubeActive(i)) {
            if (!loaded(cubeBounds(i))) {
                boss.getEntityData().set(GeometryHolderEntity.ULTIMATE_MASK, mask() & ~(1<<i));
                continue;
            }
            if (remaining <= 0 || !boss.level().getEntitiesOfClass(Player.class, cubeBounds(i), boss::eligible).isEmpty())
                detonate |= 1<<i;
            else if (remaining % 10 == 0 && boss.level() instanceof ServerLevel server) {
                Vec3 center = cubePosition(i);
                server.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                        remaining <= 20 ? 3 : 1, .2, .2, .2, .005);
            }
        }
        // Simultaneous detonations keep their spatial cues without stacking full-volume tails.
        float volume = .8F / (float)Math.sqrt(Math.max(1, Integer.bitCount(detonate)));
        for (int i=0; i<MAX_CUBES; i++) if ((detonate & (1<<i)) != 0) {
            boss.getEntityData().set(GeometryHolderEntity.ULTIMATE_MASK, mask() & ~(1<<i));
            boss.rangedAttacks().explodeAt(cubePosition(i), 1, volume, DAMAGE);
        }
        if (cubeCount() == 0 || remaining <= 0) boss.finishUltimate();
        return true;
    }
}
