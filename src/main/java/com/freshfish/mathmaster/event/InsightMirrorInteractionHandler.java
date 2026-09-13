package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.intellect.InsightTargeting;
import com.freshfish.mathmaster.item.LingxuMirrorItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class InsightMirrorInteractionHandler {
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ItemStack stack = event.getEntity().getMainHandItem();
        if (!(stack.getItem() instanceof LingxuMirrorItem mirror)) {
            return;
        }

        LivingEntity livingTarget = InsightTargeting.resolveLivingTarget(event.getTarget());
        InteractionResult result = livingTarget == null
                ? mirror.tryStartSelfInsight(event.getEntity(), InteractionHand.MAIN_HAND)
                : stack.interactLivingEntity(
                        event.getEntity(),
                        livingTarget,
                        InteractionHand.MAIN_HAND
                );
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ItemStack stack = event.getEntity().getMainHandItem();
        if (!(stack.getItem() instanceof LingxuMirrorItem mirror)) {
            return;
        }

        InteractionResult result = mirror.tryStartSelfInsight(
                event.getEntity(),
                InteractionHand.MAIN_HAND
        );
        event.setCancellationResult(result);
        event.setCanceled(true);
    }
}
