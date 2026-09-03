package com.freshfish.mathmaster.intellect;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.intelligence.IntelligenceData;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class InsightResultManager {
    private static final String COMPLETION_CRITERION = "completed";
    private static final ResourceLocation MIRROR_ALL_BEINGS =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "insight/mirror_all_beings");
    private static final ResourceLocation COUNTLESS_CREATURES =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "insight/countless_creatures");
    private static final ResourceLocation GOTHAM_NIGHTMARE =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "insight/gotham_nightmare");
    private static final ResourceLocation BAT = ResourceLocation.withDefaultNamespace("bat");

    private InsightResultManager() {
    }

    public static void record(
            ServerPlayer player,
            ResourceLocation entityTypeId,
            int targetIntellect,
            int correctAnswers,
            int totalQuestions
    ) {
        if (!MathMasterConfig.isEntityIntellectEnabled()) {
            return;
        }

        int safeTotal = Math.max(0, totalQuestions);
        int safeCorrect = Math.max(0, Math.min(correctAnswers, safeTotal));
        InsightResultData results = player.getData(ModAttachments.INSIGHT_RESULTS);
        results.record(entityTypeId, safeCorrect, safeTotal);
        if (safeTotal > 0 && safeCorrect == safeTotal && entityTypeId.equals(BAT)) {
            awardAdvancement(player, GOTHAM_NIGHTMARE);
        }
        awardSpeciesAdvancements(player, results.getDistinctEntityTypeCount());

        int requestedExperience = safeTotal == 0
                ? 0
                : (int) ((long) Math.max(0, targetIntellect) * safeCorrect / safeTotal);
        int experienceIqCap = (int) Math.min(
                IntelligenceData.MAX_IQ,
                Math.max(0L, (long) targetIntellect + 30L)
        );
        int awardedExperience = IntelligenceManager.addExperienceCappedAtIq(
                player,
                requestedExperience,
                experienceIqCap
        );

        int percent = safeTotal == 0 ? 0 : safeCorrect * 100 / safeTotal;
        player.sendSystemMessage(Component.translatable(
                safeTotal > 0 && safeCorrect == safeTotal
                        ? "message.mathmaster.insight.result.success"
                        : "message.mathmaster.insight.result.failure",
                safeCorrect,
                safeTotal,
                percent,
                results.getDistinctEntityTypeCount(),
                awardedExperience
        ));
    }

    private static void awardSpeciesAdvancements(ServerPlayer player, int distinctEntityTypes) {
        if (distinctEntityTypes >= 10) {
            awardAdvancement(player, MIRROR_ALL_BEINGS);
        }
        if (distinctEntityTypes > 30) {
            awardAdvancement(player, COUNTLESS_CREATURES);
        }
    }

    private static void awardAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        AdvancementHolder advancement = player.serverLevel()
                .getServer()
                .getAdvancements()
                .get(advancementId);
        if (advancement != null) {
            player.getAdvancements().award(advancement, COMPLETION_CRITERION);
        }
    }
}
