package com.freshfish.mathmaster.quiz;

import java.util.List;

public record LocalizedQuestionText(
        String question,
        String correctAnswer,
        List<String> wrongAnswers
) {
    public LocalizedQuestionText {
        wrongAnswers = List.copyOf(wrongAnswers);
        if (question.isBlank() || correctAnswer.isBlank()) {
            throw new IllegalArgumentException("Localized question text must not be blank");
        }
        if (wrongAnswers.size() != 3 || wrongAnswers.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Localized question must have three non-blank wrong answers");
        }
    }
}
