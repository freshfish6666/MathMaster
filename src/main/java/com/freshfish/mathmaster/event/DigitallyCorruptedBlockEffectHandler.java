package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.pollution.PollutionExposure;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class DigitallyCorruptedBlockEffectHandler {
    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) {
            return;
        }

        boolean canReceivePollution = entity instanceof Player
                || EntityIntellectManager.get(entity) != null;
        if (!canReceivePollution) {
            PollutionExposure.updateBlockEffect(entity,0);
            return;
        }

        PollutionExposure.updateBlockEffect(entity,PollutionExposure.environmentLevel(entity));
    }
}
