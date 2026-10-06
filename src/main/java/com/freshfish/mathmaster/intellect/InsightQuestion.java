package com.freshfish.mathmaster.intellect;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.freshfish.mathmaster.quiz.LocalizedQuestionText;
import net.minecraft.resources.ResourceLocation;

public record InsightQuestion(
        ResourceLocation id,
        String question,
        String correctAnswer,
        List<String> wrongAnswers,
        Map<String, LocalizedQuestionText> translations
) {
    public InsightQuestion {
        wrongAnswers = List.copyOf(wrongAnswers);
        translations = Map.copyOf(translations);
    }

    public LocalizedQuestionText baseText() {
        return new LocalizedQuestionText(question, correctAnswer, wrongAnswers);
    }

    public Optional<LocalizedQuestionText> translation(String language) {
        return Optional.ofNullable(translations.get(language));
    }
}
