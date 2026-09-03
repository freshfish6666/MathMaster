package com.freshfish.mathmaster.intellect;

import java.util.List;

public record InsightQuestion(String question, String correctAnswer, List<String> wrongAnswers) {
}
