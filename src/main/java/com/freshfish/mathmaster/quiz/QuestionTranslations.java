package com.freshfish.mathmaster.quiz;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestionTranslations {
    private QuestionTranslations() {
    }

    public static Map<String, LocalizedQuestionText> parse(JsonObject question, String label) {
        if (!question.has("translations")) {
            return Map.of();
        }
        JsonObject translations = GsonHelper.getAsJsonObject(question, "translations");
        Map<String, LocalizedQuestionText> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : translations.entrySet()) {
            String language = entry.getKey().trim().toLowerCase(java.util.Locale.ROOT);
            if (!language.matches("[a-z]{2}_[a-z]{2}")) {
                throw new IllegalArgumentException("invalid translation language " + entry.getKey());
            }
            JsonObject object = GsonHelper.convertToJsonObject(
                    entry.getValue(),
                    label + " translation " + language
            );
            LocalizedQuestionText text = parseText(object, label + " translation " + language);
            if (result.putIfAbsent(language, text) != null) {
                throw new IllegalArgumentException("duplicate translation language " + language);
            }
        }
        return Map.copyOf(result);
    }

    private static LocalizedQuestionText parseText(JsonObject object, String label) {
        String question = requireText(GsonHelper.getAsString(object, "question"), label + " question");
        String correct = requireText(
                GsonHelper.getAsString(object, "correct_answer"),
                label + " correct_answer"
        );
        JsonArray wrongElements = GsonHelper.getAsJsonArray(object, "wrong_answers");
        if (wrongElements.size() != 3) {
            throw new IllegalArgumentException(label + " wrong_answers must contain exactly 3 entries");
        }

        List<String> wrong = new ArrayList<>(3);
        Set<String> answers = new HashSet<>();
        answers.add(correct);
        for (int index = 0; index < wrongElements.size(); index++) {
            String answer = requireText(
                    GsonHelper.convertToString(wrongElements.get(index), label + " wrong_answers[" + index + "]"),
                    label + " wrong_answers[" + index + "]"
            );
            if (!answers.add(answer)) {
                throw new IllegalArgumentException(label + " answers must all be distinct");
            }
            wrong.add(answer);
        }
        return new LocalizedQuestionText(question, correct, wrong);
    }

    private static String requireText(String value, String field) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
