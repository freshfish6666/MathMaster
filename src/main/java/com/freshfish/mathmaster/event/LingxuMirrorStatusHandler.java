package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.intelligence.IntelligenceData;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = MathMaster.MODID)
public final class LingxuMirrorStatusHandler {
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private LingxuMirrorStatusHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.tickCount % REFRESH_INTERVAL_TICKS == 0) {
            refreshIfHeld(player);
        }
    }

    public static void refreshIfHeld(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !isHoldingMirror(serverPlayer)) {
            return;
        }
        if (serverPlayer.isUsingItem()
                && serverPlayer.getUseItem().is(ModItems.LINGXU_MIRROR.get())) {
            return;
        }

        IntelligenceData intelligence = serverPlayer.getData(ModAttachments.INTELLIGENCE);
        int effectiveIq = IntelligenceManager.getEffectiveIq(serverPlayer);
        Component status = intelligence.getIq() >= IntelligenceData.MAX_IQ
                ? Component.translatable(
                        "message.mathmaster.lingxu_mirror.max",
                        effectiveIq
                )
                : Component.translatable(
                        "message.mathmaster.lingxu_mirror.status",
                        effectiveIq,
                        intelligence.getExperience(),
                        intelligence.getXpNeededForNextIq()
                );
        serverPlayer.displayClientMessage(status, true);
    }

    private static boolean isHoldingMirror(Player player) {
        return player.getMainHandItem().is(ModItems.LINGXU_MIRROR.get())
                || player.getOffhandItem().is(ModItems.LINGXU_MIRROR.get());
    }
}
