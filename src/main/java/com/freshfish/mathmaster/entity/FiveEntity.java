package com.freshfish.mathmaster.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A stationary, raft-sized amalgam whose broad back acts as a living platform. */
public final class FiveEntity extends Monster {
    public static final float HITBOX_WIDTH = 50.0F;
    public static final float HITBOX_HEIGHT = 3.0F;
    public static final double PLATFORM_HALF_SIZE = 23.5D;
    public static final double PLATFORM_HEIGHT = 2.4D;

    private double anchorX;
    private double anchorZ;
    private boolean anchorReady;

    public FiveEntity(EntityType<? extends FiveEntity> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // Number 5 is terrain-like: it neither wanders nor chooses a target.
    }

    @Override
    public void tick() {
        if (!anchorReady) {
            anchorX = getX();
            anchorZ = getZ();
            anchorReady = true;
        }
        super.tick();
        setPos(anchorX, getY(), anchorZ);
        Vec3 motion = getDeltaMovement();
        double vertical = motion.y;
        if (isInWater() && vertical < 0.08D) {
            vertical = Math.min(0.08D, vertical + 0.035D);
        }
        setDeltaMovement(0.0D, vertical, 0.0D);
        setYRot(0.0F);
        yBodyRot = 0.0F;
        yHeadRot = 0.0F;
        supportRidersOnBack();
    }

    private void supportRidersOnBack() {
        double top = platformTopY();
        AABB supportArea = new AABB(
                getX() - PLATFORM_HALF_SIZE, top - 0.8D, getZ() - PLATFORM_HALF_SIZE,
                getX() + PLATFORM_HALF_SIZE, top + 1.1D, getZ() + PLATFORM_HALF_SIZE);
        for (Entity entity : level().getEntities(this, supportArea, this::canSupport)) {
            Vec3 movement = entity.getDeltaMovement();
            double previousFeet = entity.getY() - movement.y;
            if (movement.y <= 0.0D && entity.getY() <= top + 0.45D && previousFeet >= top - 0.45D) {
                entity.setPos(entity.getX(), top, entity.getZ());
                entity.setDeltaMovement(movement.x, 0.0D, movement.z);
                entity.setOnGround(true);
                entity.resetFallDistance();
            }
        }
    }

    private boolean canSupport(Entity entity) {
        return entity.isAlive() && !entity.isSpectator() && !(entity instanceof FiveEntity)
                && isOverPlatform(entity.getX(), entity.getZ());
    }

    public double platformTopY() {
        return getY() + PLATFORM_HEIGHT;
    }

    public boolean isOverPlatform(double x, double z) {
        double dx = Math.abs(x - getX());
        double dz = Math.abs(z - getZ());
        if (dx > PLATFORM_HALF_SIZE || dz > PLATFORM_HALF_SIZE) {
            return false;
        }
        // Clip the square corners to match the irregular flesh silhouette.
        return dx + dz <= PLATFORM_HALF_SIZE * 1.72D;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }
}
