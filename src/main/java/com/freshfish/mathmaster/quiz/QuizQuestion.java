package com.freshfish.mathmaster.quiz;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record QuizQuestion(
        ResourceLocation id,
        String question,
        String correctAnswer,
        List<String> wrongAnswers
) {
}
