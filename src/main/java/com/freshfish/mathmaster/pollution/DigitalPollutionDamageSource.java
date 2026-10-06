package com.freshfish.mathmaster.pollution;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public final class DigitalPollutionDamageSource extends DamageSource {
    public static final int MESSAGE_VARIANT_COUNT = 12;

    private final int messageVariant;

    public DigitalPollutionDamageSource(Holder<DamageType> type, int messageVariant) {
        super(type);
        this.messageVariant = Math.floorMod(messageVariant, MESSAGE_VARIANT_COUNT);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        return Component.translatable(
                "death.attack.mathmaster.digital_pollution." + messageVariant,
                entity.getDisplayName()
        );
    }
}
