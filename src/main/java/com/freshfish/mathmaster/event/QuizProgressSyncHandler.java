package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.quiz.QuizQuestionManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = MathMaster.MODID)
public final class QuizProgressSyncHandler {
    private static long lastSyncedReloadGeneration = -1;

    private QuizProgressSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(ModAttachments.INTELLIGENCE);
            player.syncData(ModAttachments.INTELLIGENCE);
            player.getData(ModAttachments.QUIZ_PROGRESS);
            player.syncData(ModAttachments.QUIZ_PROGRESS);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(ModAttachments.INTELLIGENCE);
            player.syncData(ModAttachments.INTELLIGENCE);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long reloadGeneration = QuizQuestionManager.getReloadGeneration();
        if (reloadGeneration == lastSyncedReloadGeneration) {
            return;
        }

        lastSyncedReloadGeneration = reloadGeneration;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            player.getData(ModAttachments.QUIZ_PROGRESS);
            player.syncData(ModAttachments.QUIZ_PROGRESS);
        }
    }
}
