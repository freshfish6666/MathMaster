package com.freshfish.mathmaster.intellect;

import com.freshfish.mathmaster.MathMaster;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class InsightQuestionManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final InsightQuestionManager INSTANCE = new InsightQuestionManager();

    private static volatile Map<ResourceLocation, List<InsightQuestion>> questionsByBank = Map.of();

    private InsightQuestionManager() {
        super(GSON, "insight_quiz_banks");
    }

    public static boolean hasQuestions(ResourceLocation bankId) {
        return !questionsByBank.getOrDefault(bankId, List.of()).isEmpty();
    }

    public static boolean canCreateSession(InsightDifficulty difficulty) {
        return difficulty.weightedBanks().stream()
                .anyMatch(weightedBank -> hasQuestions(weightedBank.bankId()));
    }

    public static List<InsightQuestion> createSession(InsightDifficulty difficulty) {
        List<InsightDifficulty.WeightedBank> availableBanks = difficulty.weightedBanks().stream()
                .filter(weightedBank -> hasQuestions(weightedBank.bankId()))
                .toList();
        if (availableBanks.isEmpty()) {
            return List.of();
        }

        Map<ResourceLocation, List<InsightQuestion>> remainingByBank = new HashMap<>();
        List<InsightQuestion> selected = new ArrayList<>(difficulty.questionCount());
        for (int index = 0; index < difficulty.questionCount(); index++) {
            InsightDifficulty.WeightedBank weightedBank = selectWeightedBank(availableBanks);
            List<InsightQuestion> remaining = remainingByBank.computeIfAbsent(
                    weightedBank.bankId(),
                    InsightQuestionManager::shuffledQuestions
            );
            if (remaining.isEmpty()) {
                remaining.addAll(shuffledQuestions(weightedBank.bankId()));
            }
            selected.add(remaining.remove(remaining.size() - 1));
        }
        return List.copyOf(selected);
    }

    private static InsightDifficulty.WeightedBank selectWeightedBank(
            List<InsightDifficulty.WeightedBank> banks
    ) {
        int totalWeight = banks.stream().mapToInt(InsightDifficulty.WeightedBank::weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (InsightDifficulty.WeightedBank bank : banks) {
            roll -= bank.weight();
            if (roll < 0) {
                return bank;
            }
        }
        return banks.getLast();
    }

    private static List<InsightQuestion> shuffledQuestions(ResourceLocation bankId) {
        List<InsightQuestion> shuffled = new ArrayList<>(
                questionsByBank.getOrDefault(bankId, List.of())
        );
        java.util.Collections.shuffle(shuffled);
        return shuffled;
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<ResourceLocation, List<InsightQuestion>> loaded = new HashMap<>();
        resources.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> loadBank(entry.getKey(), entry.getValue(), loaded));
        questionsByBank = Map.copyOf(loaded);
        MathMaster.LOGGER.info("Loaded {} insight quiz banks", questionsByBank.size());
    }

    private static void loadBank(
            ResourceLocation bankId,
            JsonElement element,
            Map<ResourceLocation, List<InsightQuestion>> loaded
    ) {
        try {
            JsonObject root = GsonHelper.convertToJsonObject(element, bankId.toString());
            JsonArray questionElements = GsonHelper.getAsJsonArray(root, "questions");
            List<InsightQuestion> questions = new ArrayList<>(questionElements.size());
            Set<String> questionTexts = new HashSet<>();

            for (int index = 0; index < questionElements.size(); index++) {
                try {
                    InsightQuestion question = parseQuestion(questionElements.get(index), index);
                    if (!questionTexts.add(question.question())) {
                        throw new IllegalArgumentException("duplicate question text");
                    }
                    questions.add(question);
                } catch (RuntimeException exception) {
                    MathMaster.LOGGER.error(
                            "Skipping invalid insight question {} in {}: {}",
                            index + 1,
                            bankId,
                            exception.getMessage()
                    );
                }
            }

            loaded.put(bankId, List.copyOf(questions));
        } catch (RuntimeException exception) {
            MathMaster.LOGGER.error("Could not load insight quiz bank {}", bankId, exception);
        }
    }

    private static InsightQuestion parseQuestion(JsonElement element, int index) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "question " + (index + 1));
        String question = requireText(GsonHelper.getAsString(object, "question"), "question");
        String correctAnswer = requireText(
                GsonHelper.getAsString(object, "correct_answer"),
                "correct_answer"
        );
        JsonArray wrongElements = GsonHelper.getAsJsonArray(object, "wrong_answers");
        if (wrongElements.size() != 3) {
            throw new IllegalArgumentException("wrong_answers must contain exactly 3 entries");
        }

        Set<String> answers = new HashSet<>();
        answers.add(correctAnswer);
        List<String> wrongAnswers = new ArrayList<>(3);
        for (int answerIndex = 0; answerIndex < wrongElements.size(); answerIndex++) {
            String answer = requireText(
                    GsonHelper.convertToString(wrongElements.get(answerIndex), "wrong answer"),
                    "wrong answer"
            );
            if (!answers.add(answer)) {
                throw new IllegalArgumentException("all four answers must be distinct");
            }
            wrongAnswers.add(answer);
        }
        return new InsightQuestion(question, correctAnswer, List.copyOf(wrongAnswers));
    }

    private static String requireText(String value, String field) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
