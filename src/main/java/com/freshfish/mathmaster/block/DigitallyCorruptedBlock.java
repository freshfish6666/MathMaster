package com.freshfish.mathmaster.block;

import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.DropExperienceBlock;

public final class DigitallyCorruptedBlock extends DropExperienceBlock {
    public static final int EFFECT_DURATION_TICKS = 10;

    public DigitallyCorruptedBlock(IntProvider experience, Properties properties) {
        super(experience, properties);
    }

    public static boolean isGrantedEffect(MobEffectInstance effect) {
        return effect != null
                && effect.isAmbient()
                && !effect.isVisible()
                && effect.showIcon()
                && effect.getDuration() <= EFFECT_DURATION_TICKS;
    }
}
