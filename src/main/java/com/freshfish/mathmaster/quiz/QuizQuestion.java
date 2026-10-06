package com.freshfish.mathmaster.quiz;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record QuizQuestion(
        ResourceLocation id,
        String question,
        String correctAnswer,
        List<String> wrongAnswers,
        Map<String, LocalizedQuestionText> translations
) {
    public QuizQuestion {
        wrongAnswers = List.copyOf(wrongAnswers);
        translations = Map.copyOf(translations);
    }

    public LocalizedQuestionText baseText() {
        return new LocalizedQuestionText(question, correctAnswer, wrongAnswers);
    }

    public Optional<LocalizedQuestionText> translation(String language) {
        return Optional.ofNullable(translations.get(language));
    }

    public LocalizedQuestionText text(String language) {
        return translation(language).orElseGet(this::baseText);
    }
}
