package com.freshfish.mathmaster.menu;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModMenuTypes;
import com.freshfish.mathmaster.quiz.QuizBank;
import com.freshfish.mathmaster.quiz.QuizProgressData;
import com.freshfish.mathmaster.quiz.QuizQuestion;
import com.freshfish.mathmaster.quiz.QuizQuestionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MathMasterGuideMenu extends AbstractContainerMenu {
    private static final Component TITLE = Component.translatable("menu.mathmaster.guide");

    private final QuizBank quizBank;
    private final List<QuestionEntry> questions;
    private final int correctCount;

    public MathMasterGuideMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, requireBank(data.readUtf()), readQuestions(data));
    }

    private MathMasterGuideMenu(int containerId, QuizBank quizBank, List<QuestionEntry> questions) {
        super(ModMenuTypes.MATHMASTER_GUIDE.get(), containerId);
        this.quizBank = quizBank;
        this.questions = List.copyOf(questions);
        this.correctCount = (int) questions.stream().filter(QuestionEntry::correct).count();
    }

    public static void open(ServerPlayer player, QuizBank bank) {
        List<QuestionEntry> questions = createSnapshot(player, bank);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) ->
                        new MathMasterGuideMenu(containerId, bank, questions),
                TITLE
        ), buffer -> writeOpeningData(buffer, bank, questions));
    }

    public QuizBank getQuizBank() {
        return this.quizBank;
    }

    public List<QuestionEntry> getQuestions() {
        return this.questions;
    }

    public int getCorrectCount() {
        return this.correctCount;
    }

    public int getTotalCount() {
        return this.questions.size();
    }

    public boolean isComplete() {
        return !this.questions.isEmpty() && this.correctCount == this.questions.size();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static List<QuestionEntry> createSnapshot(ServerPlayer player, QuizBank bank) {
        QuizProgressData progress = player.getData(ModAttachments.QUIZ_PROGRESS);
        Set<net.minecraft.resources.ResourceLocation> correctIds = progress.getCorrectQuestionIds(bank);
        List<QuizQuestion> loadedQuestions = QuizQuestionManager.getQuestions(bank);
        List<QuestionEntry> snapshot = new ArrayList<>(loadedQuestions.size());

        for (int index = 0; index < loadedQuestions.size(); index++) {
            QuizQuestion question = loadedQuestions.get(index);
            boolean correct = correctIds.contains(question.id());
            snapshot.add(new QuestionEntry(
                    index + 1,
                    correct ? question.question() : "",
                    correct ? question.correctAnswer() : "",
                    correct ? question.text("en_us").question() : "",
                    correct ? question.text("en_us").correctAnswer() : "",
                    correct
            ));
        }
        return List.copyOf(snapshot);
    }

    private static void writeOpeningData(
            RegistryFriendlyByteBuf buffer,
            QuizBank bank,
            List<QuestionEntry> questions
    ) {
        buffer.writeUtf(bank.dataId());
        buffer.writeVarInt(questions.size());
        for (QuestionEntry question : questions) {
            buffer.writeBoolean(question.correct());
            if (question.correct()) {
                buffer.writeUtf(question.question());
                buffer.writeUtf(question.correctAnswer());
                buffer.writeUtf(question.englishQuestion());
                buffer.writeUtf(question.englishCorrectAnswer());
            }
        }
    }

    private static List<QuestionEntry> readQuestions(RegistryFriendlyByteBuf buffer) {
        int questionCount = buffer.readVarInt();
        if (questionCount < 0) {
            throw new IllegalArgumentException("Negative guide question count");
        }

        List<QuestionEntry> questions = new ArrayList<>(questionCount);
        for (int index = 0; index < questionCount; index++) {
            boolean correct = buffer.readBoolean();
            String question = correct ? buffer.readUtf() : "";
            String correctAnswer = correct ? buffer.readUtf() : "";
            String englishQuestion = correct ? buffer.readUtf() : "";
            String englishCorrectAnswer = correct ? buffer.readUtf() : "";
            questions.add(new QuestionEntry(
                    index + 1,
                    question,
                    correctAnswer,
                    englishQuestion,
                    englishCorrectAnswer,
                    correct
            ));
        }
        return List.copyOf(questions);
    }

    private static QuizBank requireBank(String dataId) {
        QuizBank bank = QuizBank.byDataId(dataId);
        if (bank == null) {
            throw new IllegalArgumentException("Unknown quiz bank " + dataId);
        }
        return bank;
    }

    public record QuestionEntry(
            int number,
            String question,
            String correctAnswer,
            String englishQuestion,
            String englishCorrectAnswer,
            boolean correct
    ) {
        public String question(boolean english) {
            return english ? englishQuestion : question;
        }

        public String correctAnswer(boolean english) {
            return english ? englishCorrectAnswer : correctAnswer;
        }
    }
}
