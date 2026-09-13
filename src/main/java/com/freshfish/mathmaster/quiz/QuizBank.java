package com.freshfish.mathmaster.quiz;

import com.freshfish.mathmaster.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.Optional;
import java.util.function.Supplier;

public enum QuizBank {
    ADVANCED_MATH("advanced", () -> ModItems.ADVANCED_MATH_WORKBOOK.get(),
            () -> ModItems.ADVANCED_MATH_STUDY_NOTE.get(), 20),
    GRADE_2_MATH("grade_2", () -> ModItems.ELEMENTARY_GRADE_2_MATH.get(),
            () -> ModItems.GRADE_2_MATH_STUDY_NOTE.get(), 1),
    JUNIOR_HIGH_MATH("junior_high", () -> ModItems.JUNIOR_HIGH_MATH.get(),
            () -> ModItems.JUNIOR_HIGH_MATH_STUDY_NOTE.get(), 5),
    SENIOR_HIGH_MATH("senior_high", () -> ModItems.SENIOR_HIGH_MATH.get(),
            () -> ModItems.SENIOR_HIGH_MATH_STUDY_NOTE.get(), 10),
    MILLENNIUM_PROBLEMS_MATH("millennium_problems", () -> ModItems.MILLENNIUM_PROBLEMS_MATH.get(),
            () -> ModItems.MILLENNIUM_PROBLEMS_STUDY_NOTE.get(), 30);

    private final String dataId;
    private final Supplier<Item> bookItem;
    private final Supplier<Item> studyNoteItem;
    private final int difficulty;

    QuizBank(String dataId, Supplier<Item> bookItem, Supplier<Item> studyNoteItem, int difficulty) {
        this.dataId = dataId;
        this.bookItem = bookItem;
        this.studyNoteItem = studyNoteItem;
        this.difficulty = difficulty;
    }

    public String dataId() {
        return this.dataId;
    }

    public Item bookItem() {
        return this.bookItem.get();
    }

    public Item studyNoteItem() {
        return this.studyNoteItem.get();
    }

    public int difficulty() {
        return this.difficulty;
    }

    public boolean hasQuestions() {
        return QuizQuestionManager.hasQuestions(this);
    }

    public Optional<SelectedQuiz> randomQuestion(ServerPlayer player) {
        return QuizProgressManager.selectQuestion(player, this);
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

    public static QuizBank byBookId(ResourceLocation bookId) {
        for (QuizBank bank : values()) {
            if (BuiltInRegistries.ITEM.getKey(bank.bookItem()).equals(bookId)) {
                return bank;
            }
        }
        return null;
    }

    public record SelectedQuiz(int number, QuizQuestion question) {
    }
}
