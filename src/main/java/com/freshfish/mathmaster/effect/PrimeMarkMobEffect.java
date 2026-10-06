package com.freshfish.mathmaster.effect;

import com.freshfish.mathmaster.init.ModAttachments;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class PrimeMarkMobEffect extends MobEffect {
    public PrimeMarkMobEffect() {
        super(MobEffectCategory.NEUTRAL, 0xA5A0AA);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration == 1;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide() && entity.isAlive()) {
            var mark = entity.getData(ModAttachments.PRIME_MARK);
            int prime = mark.getPrime();
            mark.clear();
            entity.syncData(ModAttachments.PRIME_MARK);
            entity.hurt(entity.damageSources().generic(), prime);
        }
        return true;
    }
}
