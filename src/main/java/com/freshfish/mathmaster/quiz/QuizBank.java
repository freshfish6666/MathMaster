package com.freshfish.mathmaster.quiz;

import com.freshfish.mathmaster.init.ModItems;
import net.minecraft.world.item.Item;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public enum QuizBank {
    ADVANCED_MATH("advanced", () -> ModItems.ADVANCED_MATH_WORKBOOK.get(), 20),
    GRADE_2_MATH("grade_2", () -> ModItems.ELEMENTARY_GRADE_2_MATH.get(), 1),
    JUNIOR_HIGH_MATH("junior_high", () -> ModItems.JUNIOR_HIGH_MATH.get(), 5),
    SENIOR_HIGH_MATH("senior_high", () -> ModItems.SENIOR_HIGH_MATH.get(), 10),
    MILLENNIUM_PROBLEMS_MATH("millennium_problems", () -> ModItems.MILLENNIUM_PROBLEMS_MATH.get(), 30);

    private final String dataId;
    private final Supplier<Item> bookItem;
    private final int difficulty;

    QuizBank(String dataId, Supplier<Item> bookItem, int difficulty) {
        this.dataId = dataId;
        this.bookItem = bookItem;
        this.difficulty = difficulty;
    }

    public String dataId() {
        return this.dataId;
    }

    public Item bookItem() {
        return this.bookItem.get();
    }

    public int difficulty() {
        return this.difficulty;
    }

    public boolean hasQuestions() {
        return QuizQuestionManager.hasQuestions(this);
    }

    public Optional<SelectedQuiz> randomQuestion() {
        var questions = QuizQuestionManager.getQuestions(this);
        if (questions.isEmpty()) {
            return Optional.empty();
        }
        int index = ThreadLocalRandom.current().nextInt(questions.size());
        return Optional.of(new SelectedQuiz(index + 1, questions.get(index)));
    }

    public static QuizBank byItem(Item item) {
        for (QuizBank bank : values()) {
            if (bank.bookItem() == item) {
                return bank;
            }
        }
        return null;
    }

    public static QuizBank byDataId(String dataId) {
        for (QuizBank bank : values()) {
            if (bank.dataId.equals(dataId)) {
                return bank;
            }
        }
        return null;
    }

    public record SelectedQuiz(int number, QuizQuestion question) {
    }
}
