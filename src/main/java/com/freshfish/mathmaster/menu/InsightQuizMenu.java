package com.freshfish.mathmaster.menu;

import com.freshfish.mathmaster.init.ModMenuTypes;
import com.freshfish.mathmaster.intellect.EntityIntellectDefinition;
import com.freshfish.mathmaster.intellect.InsightQuestion;
import com.freshfish.mathmaster.intellect.InsightQuestionManager;
import com.freshfish.mathmaster.intellect.InsightResultManager;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.quiz.LocalizedQuestionText;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InsightQuizMenu extends AbstractContainerMenu {
    public static final int ANSWER_COUNT = 4;
    public static final int NEXT_BUTTON_ID = 4;
    public static final int CLOSE_BUTTON_ID = 5;

    private static final int UNANSWERED = 0;
    private static final int CORRECT_OFFSET = ANSWER_COUNT + 1;
    private static final int MAX_SESSION_QUESTIONS = 15;
    private static final Component TITLE = Component.translatable("menu.mathmaster.insight_quiz");

    private final String targetName;
    private final int targetIntellect;
    private final int playerIq;
    private final ResourceLocation targetEntityTypeId;
    private final List<SessionQuestion> questions;
    private final DataSlot currentQuestionIndex = DataSlot.standalone();
    private final DataSlot correctAnswers = DataSlot.standalone();
    private final DataSlot answerState = DataSlot.standalone();
    private boolean resultRecorded;

    public InsightQuizMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, readOpenData(data));
    }

    private InsightQuizMenu(int containerId, OpenData data) {
        this(
                containerId,
                data.targetName(),
                data.targetIntellect(),
                data.playerIq(),
                null,
                data.questions()
        );
    }

    private InsightQuizMenu(
            int containerId,
            String targetName,
            int targetIntellect,
            int playerIq,
            ResourceLocation targetEntityTypeId,
            List<SessionQuestion> questions
    ) {
        super(ModMenuTypes.INSIGHT_QUIZ.get(), containerId);
        if (questions.isEmpty() || questions.size() > MAX_SESSION_QUESTIONS) {
            throw new IllegalArgumentException("Invalid insight session question count");
        }
        this.targetName = targetName;
        this.targetIntellect = targetIntellect;
        this.playerIq = playerIq;
        this.targetEntityTypeId = targetEntityTypeId;
        this.questions = List.copyOf(questions);
        this.currentQuestionIndex.set(0);
        this.correctAnswers.set(0);
        this.answerState.set(UNANSWERED);
        this.addDataSlot(this.currentQuestionIndex);
        this.addDataSlot(this.correctAnswers);
        this.addDataSlot(this.answerState);
    }

    public static void open(
            ServerPlayer player,
            LivingEntity target,
            EntityIntellectDefinition definition
    ) {
        List<SessionQuestion> questions = InsightQuestionManager.createSession(
                definition.difficulty()
        ).stream().map(InsightQuizMenu::prepareQuestion).toList();
        if (questions.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.mathmaster.insight.no_questions"),
                    true
            );
            return;
        }

        String targetName = target.getDisplayName().getString();
        ResourceLocation targetEntityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        int playerIq = IntelligenceManager.getEffectiveIq(player);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new InsightQuizMenu(
                        containerId,
                        targetName,
                        definition.intellect(),
                        playerIq,
                        targetEntityTypeId,
                        questions
                ),
                TITLE
        ), buffer -> writeOpenData(
                buffer,
                targetName,
                definition.intellect(),
                playerIq,
                questions
        ));
    }

    public String getTargetName() {
        return this.targetName;
    }

    public int getTargetIntellect() {
        return this.targetIntellect;
    }

    public int getPlayerIq() {
        return this.playerIq;
    }

    public int getCurrentQuestionNumber() {
        return this.currentQuestionIndex.get() + 1;
    }

    public int getTotalQuestions() {
        return this.questions.size();
    }

    public int getCorrectAnswers() {
        return this.correctAnswers.get();
    }

    public boolean isLastQuestion() {
        return this.currentQuestionIndex.get() >= this.questions.size() - 1;
    }

    public String getQuestion() {
        return currentQuestion().question();
    }

    public String getQuestion(boolean english) {
        return english ? currentQuestion().englishQuestion() : currentQuestion().question();
    }

    public List<String> getAnswers() {
        return currentQuestion().answers();
    }

    public List<String> getAnswers(boolean english) {
        return english ? currentQuestion().englishAnswers() : currentQuestion().answers();
    }

    public int getSelectedAnswerIndex() {
        int state = this.answerState.get();
        if (state == UNANSWERED) {
            return -1;
        }
        return state >= CORRECT_OFFSET ? state - CORRECT_OFFSET : state - 1;
    }

    public boolean isAnswered() {
        return this.answerState.get() != UNANSWERED;
    }

    public boolean wasCorrect() {
        return this.answerState.get() >= CORRECT_OFFSET;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (id == CLOSE_BUTTON_ID) {
            serverPlayer.closeContainer();
            return true;
        }
        if (id == NEXT_BUTTON_ID) {
            if (!isAnswered()) {
                return false;
            }
            if (isLastQuestion()) {
                serverPlayer.closeContainer();
            } else {
                this.currentQuestionIndex.set(this.currentQuestionIndex.get() + 1);
                this.answerState.set(UNANSWERED);
                this.broadcastChanges();
            }
            return true;
        }
        if (id < 0 || id >= ANSWER_COUNT || isAnswered()) {
            return false;
        }

        if (id == currentQuestion().correctAnswerIndex()) {
            this.answerState.set(CORRECT_OFFSET + id);
            this.correctAnswers.set(this.correctAnswers.get() + 1);
            serverPlayer.playNotifySound(
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        } else {
            this.answerState.set(id + 1);
            serverPlayer.playNotifySound(
                    SoundEvents.VILLAGER_NO,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
        this.broadcastChanges();
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!this.resultRecorded
                && this.targetEntityTypeId != null
                && player instanceof ServerPlayer serverPlayer) {
            this.resultRecorded = true;
            InsightResultManager.record(
                    serverPlayer,
                    this.targetEntityTypeId,
                    this.targetIntellect,
                    this.correctAnswers.get(),
                    this.questions.size()
            );
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private SessionQuestion currentQuestion() {
        int index = Math.max(0, Math.min(this.currentQuestionIndex.get(), this.questions.size() - 1));
        return this.questions.get(index);
    }

    private static SessionQuestion prepareQuestion(InsightQuestion question) {
        List<IndexedAnswer> shuffled = new ArrayList<>(ANSWER_COUNT);
        shuffled.add(new IndexedAnswer(-1, true));
        for (int index = 0; index < question.wrongAnswers().size(); index++) {
            shuffled.add(new IndexedAnswer(index, false));
        }
        Collections.shuffle(shuffled);

        LocalizedQuestionText base = question.baseText();
        LocalizedQuestionText english = question.translation("en_us").orElse(base);
        List<String> answers = shuffled.stream()
                .map(answer -> answer.text(base))
                .toList();
        List<String> englishAnswers = shuffled.stream()
                .map(answer -> answer.text(english))
                .toList();
        int correctIndex = -1;
        for (int index = 0; index < shuffled.size(); index++) {
            if (shuffled.get(index).correct()) {
                correctIndex = index;
                break;
            }
        }
        return new SessionQuestion(
                base.question(),
                answers,
                english.question(),
                englishAnswers,
                correctIndex
        );
    }

    private static void writeOpenData(
            RegistryFriendlyByteBuf buffer,
            String targetName,
            int targetIntellect,
            int playerIq,
            List<SessionQuestion> questions
    ) {
        buffer.writeUtf(targetName);
        buffer.writeVarInt(targetIntellect);
        buffer.writeVarInt(playerIq);
        buffer.writeVarInt(questions.size());
        for (SessionQuestion question : questions) {
            buffer.writeUtf(question.question());
            for (String answer : question.answers()) {
                buffer.writeUtf(answer);
            }
            buffer.writeUtf(question.englishQuestion());
            for (String answer : question.englishAnswers()) {
                buffer.writeUtf(answer);
            }
        }
    }

    private static OpenData readOpenData(RegistryFriendlyByteBuf buffer) {
        String targetName = buffer.readUtf();
        int targetIntellect = buffer.readVarInt();
        int playerIq = buffer.readVarInt();
        int questionCount = buffer.readVarInt();
        if (questionCount <= 0 || questionCount > MAX_SESSION_QUESTIONS) {
            throw new IllegalArgumentException("Invalid insight session question count");
        }

        List<SessionQuestion> questions = new ArrayList<>(questionCount);
        for (int index = 0; index < questionCount; index++) {
            String question = buffer.readUtf();
            List<String> answers = List.of(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf()
            );
            String englishQuestion = buffer.readUtf();
            List<String> englishAnswers = List.of(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf()
            );
            questions.add(new SessionQuestion(
                    question,
                    answers,
                    englishQuestion,
                    englishAnswers,
                    -1
            ));
        }
        return new OpenData(targetName, targetIntellect, playerIq, List.copyOf(questions));
    }

    private record IndexedAnswer(int wrongIndex, boolean correct) {
        private String text(LocalizedQuestionText source) {
            return correct ? source.correctAnswer() : source.wrongAnswers().get(wrongIndex);
        }
    }

    private record SessionQuestion(
            String question,
            List<String> answers,
            String englishQuestion,
            List<String> englishAnswers,
            int correctAnswerIndex
    ) {
        private SessionQuestion {
            answers = List.copyOf(answers);
            englishAnswers = List.copyOf(englishAnswers);
            if (answers.size() != ANSWER_COUNT || englishAnswers.size() != ANSWER_COUNT) {
                throw new IllegalArgumentException("Insight question must have four answers");
            }
        }
    }

    private record OpenData(
            String targetName,
            int targetIntellect,
            int playerIq,
            List<SessionQuestion> questions
    ) {
    }
}
