package com.freshfish.mathmaster.quiz;

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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuizQuestionManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final QuizQuestionManager INSTANCE = new QuizQuestionManager();

    private static volatile Map<QuizBank, List<QuizQuestion>> questionsByBank = emptyBanks();
    private static volatile long reloadGeneration;

    private QuizQuestionManager() {
        super(GSON, "quiz_banks");
    }

    public static List<QuizQuestion> getQuestions(QuizBank bank) {
        return questionsByBank.getOrDefault(bank, List.of());
    }

    public static boolean hasQuestions(QuizBank bank) {
        return !getQuestions(bank).isEmpty();
    }

    public static long getReloadGeneration() {
        return reloadGeneration;
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        EnumMap<QuizBank, List<QuizQuestion>> loaded = mutableEmptyBanks();

        resources.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> loadResource(entry.getKey(), entry.getValue(), loaded));

        EnumMap<QuizBank, List<QuizQuestion>> immutable = new EnumMap<>(QuizBank.class);
        int total = 0;
        for (QuizBank bank : QuizBank.values()) {
            List<QuizQuestion> questions = List.copyOf(loaded.get(bank));
            immutable.put(bank, questions);
            total += questions.size();
            if (questions.isEmpty()) {
                MathMaster.LOGGER.warn("Quiz bank {} contains no valid questions", bank.dataId());
            } else {
                MathMaster.LOGGER.info("Loaded {} questions for quiz bank {}", questions.size(), bank.dataId());
            }
        }

        questionsByBank = Collections.unmodifiableMap(immutable);
        reloadGeneration++;
        MathMaster.LOGGER.info("Loaded {} MathMaster quiz questions in total", total);
    }

    private static void loadResource(
            ResourceLocation resourceId,
            JsonElement element,
            EnumMap<QuizBank, List<QuizQuestion>> loaded
    ) {
        QuizBank bank = QuizBank.byDataId(resourceId.getPath());
        if (bank == null) {
            MathMaster.LOGGER.warn("Ignoring unknown quiz bank data file {}", resourceId);
            return;
        }

        try {
            JsonObject root = GsonHelper.convertToJsonObject(element, resourceId.toString());
            boolean replace = GsonHelper.getAsBoolean(root, "replace", false);
            JsonArray questions = GsonHelper.getAsJsonArray(root, "questions");
            List<QuizQuestion> target = loaded.get(bank);
            if (replace) {
                target.clear();
            }

            Set<String> questionTexts = new HashSet<>();
            Set<ResourceLocation> questionIds = new HashSet<>();
            for (QuizQuestion existing : target) {
                questionTexts.add(existing.question());
                questionIds.add(existing.id());
            }

            for (int index = 0; index < questions.size(); index++) {
                try {
                    QuizQuestion question = parseQuestion(questions.get(index), resourceId, bank, index);
                    if (questionIds.contains(question.id())) {
                        throw new IllegalArgumentException("duplicate question id " + question.id());
                    }
                    if (questionTexts.contains(question.question())) {
                        throw new IllegalArgumentException("duplicate question text");
                    }
                    questionIds.add(question.id());
                    questionTexts.add(question.question());
                    target.add(question);
                } catch (RuntimeException exception) {
                    MathMaster.LOGGER.error(
                            "Skipping invalid question {} in {}: {}",
                            index + 1,
                            resourceId,
                            exception.getMessage()
                    );
                }
            }
        } catch (RuntimeException exception) {
            MathMaster.LOGGER.error("Could not load quiz bank {}", resourceId, exception);
        }
    }

    private static QuizQuestion parseQuestion(
            JsonElement element,
            ResourceLocation resourceId,
            QuizBank bank,
            int index
    ) {
        String label = resourceId + " question " + (index + 1);
        JsonObject object = GsonHelper.convertToJsonObject(element, label);
        String question = requireText(GsonHelper.getAsString(object, "question"), "question");
        String correctAnswer = requireText(GsonHelper.getAsString(object, "correct_answer"), "correct_answer");
        JsonArray wrongAnswerElements = GsonHelper.getAsJsonArray(object, "wrong_answers");
        if (wrongAnswerElements.size() != 3) {
            throw new IllegalArgumentException("wrong_answers must contain exactly 3 entries");
        }

        List<String> wrongAnswers = new ArrayList<>(3);
        Set<String> answers = new HashSet<>();
        answers.add(correctAnswer);
        for (int answerIndex = 0; answerIndex < wrongAnswerElements.size(); answerIndex++) {
            String wrongAnswer = requireText(
                    GsonHelper.convertToString(wrongAnswerElements.get(answerIndex), "wrong_answers[" + answerIndex + "]"),
                    "wrong_answers[" + answerIndex + "]"
            );
            if (!answers.add(wrongAnswer)) {
                throw new IllegalArgumentException("correct and wrong answers must all be distinct");
            }
            wrongAnswers.add(wrongAnswer);
        }

        ResourceLocation questionId = parseQuestionId(
                object,
                resourceId,
                bank,
                question,
                correctAnswer,
                wrongAnswers
        );
        return new QuizQuestion(questionId, question, correctAnswer, List.copyOf(wrongAnswers));
    }

    private static ResourceLocation parseQuestionId(
            JsonObject object,
            ResourceLocation resourceId,
            QuizBank bank,
            String question,
            String correctAnswer,
            List<String> wrongAnswers
    ) {
        String rawId = GsonHelper.getAsString(object, "id", "").trim();
        if (rawId.isEmpty()) {
            ResourceLocation generatedId = ResourceLocation.fromNamespaceAndPath(
                    resourceId.getNamespace(),
                    bank.dataId() + "/generated/" + contentHash(question, correctAnswer, wrongAnswers)
            );
            MathMaster.LOGGER.warn(
                    "Question '{}' in {} has no explicit id; generated fallback id {}",
                    question,
                    resourceId,
                    generatedId
            );
            return generatedId;
        }

        ResourceLocation parsed = rawId.indexOf(':') >= 0
                ? ResourceLocation.tryParse(rawId)
                : ResourceLocation.tryParse(
                        resourceId.getNamespace() + ":" + bank.dataId() + "/" + rawId
                );
        if (parsed == null) {
            throw new IllegalArgumentException("invalid question id " + rawId);
        }
        return parsed;
    }

    private static String contentHash(String question, String correctAnswer, List<String> wrongAnswers) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(question.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(correctAnswer.getBytes(StandardCharsets.UTF_8));
            wrongAnswers.stream().sorted().forEach(answer -> {
                digest.update((byte) 0);
                digest.update(answer.getBytes(StandardCharsets.UTF_8));
            });
            return HexFormat.of().formatHex(digest.digest(), 0, 8);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String requireText(String value, String field) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static Map<QuizBank, List<QuizQuestion>> emptyBanks() {
        return Collections.unmodifiableMap(mutableEmptyBanks());
    }

    private static EnumMap<QuizBank, List<QuizQuestion>> mutableEmptyBanks() {
        EnumMap<QuizBank, List<QuizQuestion>> banks = new EnumMap<>(QuizBank.class);
        for (QuizBank bank : QuizBank.values()) {
            banks.put(bank, new ArrayList<>());
        }
        return banks;
    }
}
