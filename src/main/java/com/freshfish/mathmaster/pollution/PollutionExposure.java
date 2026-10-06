package com.freshfish.mathmaster.pollution;

import com.freshfish.mathmaster.block.DigitallyCorruptedBlock;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMobEffects;
import com.freshfish.mathmaster.mixin.PollutionEffectAccess;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.freshfish.mathmaster.init.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

public final class PollutionExposure {
    private PollutionExposure() {}

    public static int protection(LivingEntity entity) {
        return entity instanceof Player
                && entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LINGXU_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.LINGXU_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.LINGXU_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.LINGXU_BOOTS.get()) ? 1 : 0;
    }

    public static int blockLevel(LivingEntity entity) {
        if (!entity.level().getBlockState(entity.getOnPos()).is(ModBlocks.DIGITALLY_CORRUPTED_BLOCK.get())) return 0;
        return Math.max(0,DimensionPollutionLevels.get(entity.level()) - protection(entity));
    }

    /** Actual fluid volume intersection, including shallow flowing water at body edges. */
    public static boolean touchesPollutedWater(LivingEntity entity) {
        AABB body = entity.getBoundingBox().deflate(1.0E-5);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(body.minX); x <= Mth.floor(body.maxX); x++) {
            for (int z = Mth.floor(body.minZ); z <= Mth.floor(body.maxZ); z++) {
                for (int y = Mth.floor(body.minY); y <= Mth.floor(body.maxY); y++) {
                    pos.set(x, y, z);
                    FluidState fluid = entity.level().getFluidState(pos);
                    if (fluid.getFluidType() == ModFluids.DIGITALLY_POLLUTED_WATER_TYPE.get()
                            && y + fluid.getHeight(entity.level(), pos) > body.minY) return true;
                }
            }
        }
        return false;
    }

    public static int environmentLevel(LivingEntity entity) {
        int level = Math.max(0, DimensionPollutionLevels.get(entity.level()) - protection(entity));
        if (level == 0) return 0;
        return entity.level().getBlockState(entity.getOnPos()).is(ModBlocks.DIGITALLY_CORRUPTED_BLOCK.get())
                || touchesPollutedWater(entity) ? level : 0;
    }

    public static MobEffectInstance externalEffect(MobEffectInstance current) {
        while (current != null && (DigitallyCorruptedBlock.isGrantedEffect(current)
                || !current.isInfiniteDuration() && current.getDuration() <= 0)) {
            current = ((PollutionEffectAccess)(Object)current).mathmaster$getHiddenEffect();
        }
        return current;
    }

    public static int effectiveLevel(LivingEntity entity, MobEffectInstance current) {
        MobEffectInstance external = externalEffect(current);
        int externalLevel = external == null ? 0 : Math.max(0,external.getAmplifier()+1-protection(entity));
        return Math.max(externalLevel,environmentLevel(entity));
    }

    public static MobEffectInstance blockEffect(int level, int duration, MobEffectInstance external) {
        return new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION, duration,
                Math.min(256,level)-1,true,false,true,external);
    }

    /** Preserve the original external instance/hidden chain and its normally ticking duration. */
    public static void updateBlockEffect(LivingEntity entity, int level) {
        MobEffectInstance current = entity.getEffect(ModMobEffects.DIGITAL_POLLUTION);
        MobEffectInstance external = externalEffect(current);
        if (external != null && !external.isInfiniteDuration() && external.getDuration() <= 0) external = null;
        if (level <= 0 || external != null && external.getAmplifier()+1 >= level) {
            if (DigitallyCorruptedBlock.isGrantedEffect(current)) {
                if (external == null) entity.removeEffect(ModMobEffects.DIGITAL_POLLUTION);
                else entity.forceAddEffect(external,null);
            }
            return;
        }
        if (!DigitallyCorruptedBlock.isGrantedEffect(current)
                || current.getAmplifier() != Math.min(256,level)-1 || current.getDuration() <= 5) {
            entity.forceAddEffect(blockEffect(level,DigitallyCorruptedBlock.EFFECT_DURATION_TICKS,external),null);
        }
    }
}
