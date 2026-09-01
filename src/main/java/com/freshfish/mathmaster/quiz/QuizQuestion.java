package com.freshfish.mathmaster.quiz;

import java.util.List;

public record QuizQuestion(
        String question,
        String correctAnswer,
        List<String> wrongAnswers
) {
}
