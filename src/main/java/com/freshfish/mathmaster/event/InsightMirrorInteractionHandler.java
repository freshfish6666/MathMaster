package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.intellect.InsightTargeting;
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

        LivingEntity livingTarget = InsightTargeting.resolveLivingTarget(event.getTarget());
        if (livingTarget == null) {
            return;
        }

        ItemStack stack = event.getEntity().getMainHandItem();
        if (!stack.is(ModItems.LINGXU_MIRROR.get())) {
            return;
        }

        InteractionResult result = stack.interactLivingEntity(
                event.getEntity(),
                livingTarget,
                InteractionHand.MAIN_HAND
        );
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}
