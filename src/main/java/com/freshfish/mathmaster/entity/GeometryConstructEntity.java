package com.freshfish.mathmaster.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** A small floating sentry: tracks during warning, then fires a stationary, dodgeable ray. */
public final class GeometryConstructEntity extends PathfinderMob implements Enemy {
    public static final float SIZE = 1.375F;
    public static final int IDLE = 0, CHARGING = 1, FIRING = 2;
    public static final int CHARGE_TICKS = 20, FIRE_TICKS = 20, COOLDOWN_TICKS = 40;
    public static final double ACQUIRE_RANGE = 16, LOSE_RANGE = 24, BEAM_RANGE = 20, BEAM_RADIUS = .12;
    public static final float BEAM_DAMAGE = 4;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(GeometryConstructEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TICKS = SynchedEntityData.defineId(GeometryConstructEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DIRECTION = SynchedEntityData.defineId(GeometryConstructEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(GeometryConstructEntity.class, EntityDataSerializers.FLOAT);
    private int cooldown = 20;

    public GeometryConstructEntity(EntityType<? extends GeometryConstructEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 5;
        lookControl = new net.minecraft.world.entity.ai.control.LookControl(this) {
            @Override protected boolean resetXRotOnTick() { return false; }
        };
    }
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.ARMOR, 4).add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.FOLLOW_RANGE, ACQUIRE_RANGE);
    }
    @Override protected void registerGoals() { }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, IDLE); builder.define(TICKS, 0);
        builder.define(DIRECTION, new Vector3f(0, 0, 1)); builder.define(LENGTH, 0F);
    }
    public int attackPhase() { return entityData.get(PHASE); }
    public int attackTicks() { return entityData.get(TICKS); }
    public Vec3 beamDirection() { return new Vec3(entityData.get(DIRECTION)); }
    public float beamLength() { return entityData.get(LENGTH); }
    public Vec3 beamStart() { return position().add(0, SIZE / 2D, 0).add(beamDirection().scale(7 / 16D)); }
    public float chargeGlow(float partial) {
        return attackPhase() == FIRING ? 1 : attackPhase() == CHARGING
                ? Mth.clamp((CHARGE_TICKS - attackTicks() + partial) / CHARGE_TICKS, 0, 1) : .15F;
    }
    private boolean eligible(Player player) {
        return player.level() == level() && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }
    private void phase(int phase, int ticks) { entityData.set(PHASE, phase); entityData.set(TICKS, ticks); }
    private void stopAttack() { phase(IDLE, 0); entityData.set(LENGTH, 0F); cooldown = COOLDOWN_TICKS; }

    @Override protected void customServerAiStep() {
        Player target = getTarget() instanceof Player player && eligible(player)
                && distanceToSqr(player) <= LOSE_RANGE * LOSE_RANGE ? player : null;
        if (target == null && tickCount % 10 == 0) target = level().players().stream()
                .filter(this::eligible).filter(p -> distanceToSqr(p) <= ACQUIRE_RANGE * ACQUIRE_RANGE)
                .filter(this::hasLineOfSight).min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        setTarget(target);
        if (target == null) {
            if (attackPhase() != IDLE) stopAttack();
            setDeltaMovement(getDeltaMovement().scale(.8));
            return;
        }
        if (attackPhase() != IDLE) {
            setDeltaMovement(Vec3.ZERO);
            if (attackPhase() == CHARGING) {
                if (!hasLineOfSight(target)) { stopAttack(); return; }
                aim(target);
                int remaining = attackTicks() - 1;
                if (remaining == 0) {
                    phase(FIRING, FIRE_TICKS); updateBeam(true);
                    playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, .7F, 1.3F);
                } else {
                    phase(CHARGING, remaining); updateBeam(false);
                    if (remaining % 4 == 0 && level() instanceof ServerLevel server) {
                        Vec3 center = beamStart();
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                                1 + (CHARGE_TICKS - remaining) / 5, .12, .12, .12, .01);
                    }
                }
            } else if (attackTicks() <= 1) stopAttack();
            else { phase(FIRING, attackTicks() - 1); updateBeam(true); }
            return;
        }
        aim(target);
        moveToward(target);
        if (cooldown > 0) cooldown--;
        if (cooldown == 0 && distanceToSqr(target) <= ACQUIRE_RANGE * ACQUIRE_RANGE && hasLineOfSight(target)) {
            setDeltaMovement(Vec3.ZERO);
            phase(CHARGING, CHARGE_TICKS); updateBeam(false);
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, .6F, 1.1F);
        }
    }

    private void aim(Player target) {
        Vec3 direction = target.getBoundingBox().getCenter().subtract(position().add(0, SIZE / 2D, 0)).normalize();
        setYRot((float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90F);
        setXRot((float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z))));
        entityData.set(DIRECTION, direction.toVector3f());
    }
    private void moveToward(Player target) {
        Vec3 offset = target.position().subtract(position());
        Vec3 horizontal = new Vec3(offset.x, 0, offset.z);
        double distance = horizontal.length();
        Vec3 desired = distance > 8 ? horizontal.normalize().scale(.1)
                : distance < 4 ? horizontal.normalize().scale(-.06) : Vec3.ZERO;
        double height = target.getBoundingBox().getCenter().y - SIZE / 2D - getY();
        desired = desired.add(0, Mth.clamp(height * .1, -.07, .07), 0);
        setDeltaMovement(getDeltaMovement().lerp(desired, .2));
    }

    private void updateBeam(boolean damage) {
        Vec3 start = beamStart(), direction = beamDirection();
        // Stop at loaded-chunk boundaries before ray tracing; never load terrain to fire.
        double range = 0;
        for (double step = .25; step <= BEAM_RANGE; step += .25) {
            Vec3 next = start.add(direction.scale(step));
            if (!level().hasChunkAt(BlockPos.containing(next))) break;
            range = step;
        }
        Vec3 end = level().clip(new ClipContext(start, start.add(direction.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
        entityData.set(LENGTH, (float) start.distanceTo(end));
        if (!damage) return;
        for (Player player : level().getEntitiesOfClass(Player.class, new AABB(start, end).inflate(BEAM_RADIUS), this::eligible)) {
            AABB bounds = player.getBoundingBox().inflate(BEAM_RADIUS);
            if (bounds.contains(start) || bounds.clip(start, end).isPresent())
                player.hurt(damageSources().mobAttack(this), BEAM_DAMAGE);
        }
    }

    @Override public void travel(Vec3 input) {
        if (isEffectiveAi()) move(MoverType.SELF, getDeltaMovement());
    }
    @Override public void tick() {
        super.tick();
        yBodyRot = yHeadRot = getYRot();
        if (!level().isClientSide && !isAlive()) { phase(IDLE, 0); entityData.set(LENGTH, 0F); }
    }
    @Override public AABB getBoundingBoxForCulling() {
        return attackPhase() == IDLE ? getBoundingBox().inflate(.08)
                : getBoundingBox().minmax(new AABB(beamStart(), beamStart().add(beamDirection().scale(beamLength())))).inflate(BEAM_RADIUS);
    }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return true; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.AMETHYST_BLOCK_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.AMETHYST_BLOCK_BREAK; }
    @Override public SoundSource getSoundSource() { return SoundSource.HOSTILE; }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // An interrupted cast is not resumed or replayed after loading.
        phase(IDLE, 0); entityData.set(LENGTH, 0F); cooldown = 20;
    }
}
