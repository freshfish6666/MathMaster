package com.freshfish.mathmaster.effect;

import com.freshfish.mathmaster.intellect.EntityIntellectDefinition;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModMobEffects;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import com.freshfish.mathmaster.pollution.PollutionExposure;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class DigitalPollutionMobEffect extends MobEffect {
    private static final int TICKS_PER_SECOND = 20;

    public DigitalPollutionMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xB20B20);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide() || !entity.isAlive()) {
            return true;
        }

        int intellect = getEffectiveIntellect(entity);
        if (intellect < 0) {
            return true;
        }

        int effectLevel = PollutionExposure.effectiveLevel(entity,entity.getEffect(ModMobEffects.DIGITAL_POLLUTION));
        if (effectLevel <= 0) return true;
        int interval = pollutionIntervalTicks(intellect,effectLevel);
        if (entity.getData(ModAttachments.DIGITAL_POLLUTION).tickPollutionEffect(interval)) {
            DigitalPollutionManager.add(entity,pollutionAmount(effectLevel) * (entity instanceof Player ? 1 : 2));
        }
        return true;
    }

    private static int getEffectiveIntellect(LivingEntity entity) {
        if (entity instanceof Player player) {
            return IntelligenceManager.getEffectiveIq(player);
        }
        EntityIntellectDefinition definition = EntityIntellectManager.get(entity);
        return definition == null ? -1 : definition.intellect();
    }

    static int pollutionIntervalTicks(int intellect) {
        int clamped = Math.max(0, intellect);
        if (clamped >= 160) {
            return TICKS_PER_SECOND;
        }
        if (clamped >= 120) {
            return interpolate(clamped, 120, 160, 2 * TICKS_PER_SECOND, TICKS_PER_SECOND);
        }
        if (clamped >= 80) {
            return interpolate(clamped, 80, 120, 5 * TICKS_PER_SECOND, 2 * TICKS_PER_SECOND);
        }
        if (clamped >= 40) {
            return interpolate(clamped, 40, 80, 10 * TICKS_PER_SECOND, 5 * TICKS_PER_SECOND);
        }
        return interpolate(clamped, 0, 40, 20 * TICKS_PER_SECOND, 10 * TICKS_PER_SECOND);
    }

    public static int pollutionIntervalTicks(int intellect,int level) {
        int percent = switch (level) { case 1 -> 100; case 2 -> 90; case 3 -> 80;
            case 4 -> 75; case 5 -> 70; default -> level <= 0 ? 100 : 50; };
        return Math.max(1,(int)Math.round(pollutionIntervalTicks(intellect)*percent/100.0D));
    }

    public static int pollutionAmount(int level) { return level <= 0 ? 0 : level <= 5 ? level : 15; }

    private static int interpolate(int value, int from, int to, int startTicks, int endTicks) {
        double progress = (double) (value - from) / (to - from);
        return Math.max(1, (int) Math.round(startTicks + (endTicks - startTicks) * progress));
    }
}
