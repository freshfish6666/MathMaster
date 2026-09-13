package com.freshfish.mathmaster.quiz;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.integration.IntegrationManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class QuizProgressManager {
    private static final int UNSEEN_WEIGHT = 10;
    private static final int SEEN_ONCE_WEIGHT = 5;
    private static final int MASTERED_WEIGHT = 1;
    private static final String COMPLETION_CRITERION = "completed";
    private static final ResourceLocation ADVANCEMENT_ROOT =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "quiz/root");

    private QuizProgressManager() {
    }

    public static Optional<QuizBank.SelectedQuiz> selectQuestion(ServerPlayer player, QuizBank bank) {
        List<QuizQuestion> allQuestions = QuizQuestionManager.getQuestions(bank);
        if (allQuestions.isEmpty()) {
            return Optional.empty();
        }

        QuizProgressData progress = player.getData(ModAttachments.QUIZ_PROGRESS);
        List<IndexedQuestion> candidates = collectCandidates(allQuestions, bank, progress, true);
        if (candidates.isEmpty()) {
            candidates = collectCandidates(allQuestions, bank, progress, false);
        }

        int totalWeight = candidates.stream().mapToInt(IndexedQuestion::weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (IndexedQuestion candidate : candidates) {
            roll -= candidate.weight();
            if (roll < 0) {
                return Optional.of(new QuizBank.SelectedQuiz(candidate.index() + 1, candidate.question()));
            }
        }

        IndexedQuestion fallback = candidates.get(candidates.size() - 1);
        return Optional.of(new QuizBank.SelectedQuiz(fallback.index() + 1, fallback.question()));
    }

    public static void recordCorrect(ServerPlayer player, QuizBank bank, ResourceLocation questionId) {
        QuizProgressData progress = player.getData(ModAttachments.QUIZ_PROGRESS);
        progress.recordCorrect(bank, questionId);
        awardAdvancement(player, ADVANCEMENT_ROOT, "answered_question");

        Set<ResourceLocation> correctIds = progress.getCorrectQuestionIds(bank);
        List<QuizQuestion> loadedQuestions = QuizQuestionManager.getQuestions(bank);
        if (!loadedQuestions.isEmpty()
                && loadedQuestions.stream().allMatch(question -> correctIds.contains(question.id()))) {
            awardAdvancement(
                    player,
                    ResourceLocation.fromNamespaceAndPath(
                            MathMaster.MODID,
                            "quiz/complete_" + bank.dataId()
                    ),
                    COMPLETION_CRITERION
            );
        }
        syncAuthority(player);
        player.syncData(ModAttachments.QUIZ_PROGRESS);
    }

    public static void syncAuthority(ServerPlayer player) {
        if (!IntegrationManager.isActive()) {
            return;
        }

        OptionalInt rank = OptionalInt.empty();
        if (hasCompleted(player, QuizBank.MILLENNIUM_PROBLEMS_MATH)) {
            rank = OptionalInt.of(7);
        } else if (hasCompleted(player, QuizBank.ADVANCED_MATH)) {
            rank = OptionalInt.of(8);
        } else if (hasCompleted(player, QuizBank.GRADE_2_MATH)) {
            rank = OptionalInt.of(9);
        }
        IntegrationManager.syncAuthority(player, rank);
    }

    private static boolean hasCompleted(ServerPlayer player, QuizBank bank) {
        List<QuizQuestion> loadedQuestions = QuizQuestionManager.getQuestions(bank);
        if (loadedQuestions.isEmpty()) {
            return false;
        }
        Set<ResourceLocation> correctIds = player.getData(ModAttachments.QUIZ_PROGRESS)
                .getCorrectQuestionIds(bank);
        return loadedQuestions.stream().allMatch(question -> correctIds.contains(question.id()));
    }

    private static List<IndexedQuestion> collectCandidates(
            List<QuizQuestion> questions,
            QuizBank bank,
            QuizProgressData progress,
            boolean excludeRecent
    ) {
        List<IndexedQuestion> candidates = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            QuizQuestion question = questions.get(index);
            if (excludeRecent && progress.wasRecentlyCorrect(bank, question.id())) {
                continue;
            }
            candidates.add(new IndexedQuestion(index, question, weightFor(progress.getMastery(bank, question.id()))));
        }
        return candidates;
    }

    private static int weightFor(int mastery) {
        if (mastery <= 0) {
            return UNSEEN_WEIGHT;
        }
        return mastery == 1 ? SEEN_ONCE_WEIGHT : MASTERED_WEIGHT;
    }

    private static void awardAdvancement(ServerPlayer player, ResourceLocation advancementId, String criterion) {
        AdvancementHolder advancement = player.serverLevel().getServer().getAdvancements().get(advancementId);
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private record IndexedQuestion(int index, QuizQuestion question, int weight) {
    }
}
