package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModMobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

public final class PrimeMarkRenderer {
    private PrimeMarkRenderer() {
    }

    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        var mark = entity.getData(ModAttachments.PRIME_MARK);
        if (!mark.isActive(entity.level().getGameTime()) && !entity.hasEffect(ModMobEffects.PRIME_MARK)) {
            return;
        }

        Component prime = Component.literal(Integer.toString(mark.getPrime()))
                .withStyle(style -> style.withColor(0xE8DDF2).withBold(true));
        event.setContent(entity.hasCustomName()
                ? event.getContent().copy().append(Component.literal("  ")).append(prime)
                : prime);
        event.setCanRender(TriState.TRUE);
    }
}
