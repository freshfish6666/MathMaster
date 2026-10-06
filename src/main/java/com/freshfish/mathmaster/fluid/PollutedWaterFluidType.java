package com.freshfish.mathmaster.fluid;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/** Slow horizontal swimming, with normal water buoyancy and wall escape. */
public final class PollutedWaterFluidType extends FluidType {
    public PollutedWaterFluidType() {
        super(Properties.create().density(3000).viscosity(6000).motionScale(0.007)
                .canSwim(true).canDrown(true).canExtinguish(true).canConvertToSource(false)
                .fallDistanceModifier(0)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
    }

    @Override
    public boolean move(FluidState state, LivingEntity entity, Vec3 input, double gravity) {
        double oldY = entity.getY();
        boolean falling = entity.getDeltaMovement().y <= 0;
        entity.moveRelative((float)(0.015 * entity.getAttributeValue(NeoForgeMod.SWIM_SPEED)), input);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        Vec3 velocity = entity.getDeltaMovement();
        if (entity.horizontalCollision && entity.onClimbable()) {
            velocity = new Vec3(velocity.x, 0.2, velocity.z);
        }
        velocity = entity.getFluidFallingAdjustedMovement(gravity, falling, velocity.multiply(0.7, 0.8, 0.7));
        entity.setDeltaMovement(velocity);
        if (entity.horizontalCollision && entity.isFree(velocity.x, velocity.y + 0.6 - entity.getY() + oldY, velocity.z)) {
            entity.setDeltaMovement(velocity.x, 0.3, velocity.z);
        }
        return true;
    }
}
