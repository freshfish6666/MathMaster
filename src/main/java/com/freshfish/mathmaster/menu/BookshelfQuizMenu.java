package com.freshfish.mathmaster.menu;

import com.freshfish.mathmaster.antiaddiction.AntiAddictionManager;
import com.freshfish.mathmaster.event.BookshelfInteractionHandler;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMenuTypes;
import com.freshfish.mathmaster.intelligence.IntelligenceData;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.quiz.QuizBank;
import com.freshfish.mathmaster.quiz.QuizProgressManager;
import com.freshfish.mathmaster.quiz.QuizQuestion;
import com.freshfish.mathmaster.quiz.QuizLauncher;
import com.freshfish.mathmaster.reward.RewardManager;
import com.freshfish.mathmaster.reward.HighestTierRewardLimit;
import com.freshfish.mathmaster.reward.QuizPenaltyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;

public class BookshelfQuizMenu extends AbstractContainerMenu {
    public static final int ANSWER_BUTTON_COUNT = 4;
    public static final int NEXT_QUESTION_BUTTON_ID = 4;
    public static final int EXIT_BUTTON_ID = 5;

    private static final double MAX_DISTANCE_SQUARED = 4.0D * 4.0D;
    private static final int UNANSWERED_STATE = 0;
    private static final int CORRECT_STATE_OFFSET = ANSWER_BUTTON_COUNT + 1;

    private final BlockPos bookshelfPos;
    private final boolean requiresBookshelf;
    private final QuizBank quizBank;
    private final ResourceLocation questionId;
    private final DataSlot answerState = DataSlot.standalone();
    private final int questionNumber;
    private final String questionText;
    private final String correctAnswer;
    private final String[] wrongAnswers;
    private final String englishQuestionText;
    private final String englishCorrectAnswer;
    private final String[] englishWrongAnswers;
    private final int correctOptionIndex;
    private final int difficulty;
    private final int intelligenceLevel;
    private final int intelligenceExperience;
    private final int intelligenceRequiredXp;

    public BookshelfQuizMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(
                containerId,
                playerInventory,
                data.readBlockPos(),
                data.readBoolean(),
                requireBank(data.readUtf()),
                data.readResourceLocation(),
                data.readInt(),
                data.readUtf(),
                data.readUtf(),
                new String[]{data.readUtf(), data.readUtf(), data.readUtf()},
                data.readUtf(),
                data.readUtf(),
                new String[]{data.readUtf(), data.readUtf(), data.readUtf()},
                data.readByte(),
                data.readInt(),
                data.readInt(),
                data.readInt(),
                data.readInt()
        );
    }

    public BookshelfQuizMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos bookshelfPos,
            boolean requiresBookshelf,
            QuizBank quizBank,
            int questionNumber,
            QuizQuestion question,
            int correctOptionIndex,
            int difficulty,
            int intelligenceLevel,
            int intelligenceExperience,
            int intelligenceRequiredXp
    ) {
        this(
                containerId,
                playerInventory,
                bookshelfPos,
                requiresBookshelf,
                quizBank,
                question.id(),
                questionNumber,
                question.question(),
                question.correctAnswer(),
                question.wrongAnswers().toArray(new String[0]),
                question.text("en_us").question(),
                question.text("en_us").correctAnswer(),
                question.text("en_us").wrongAnswers().toArray(new String[0]),
                correctOptionIndex,
                difficulty,
                intelligenceLevel,
                intelligenceExperience,
                intelligenceRequiredXp
        );
    }

    private BookshelfQuizMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos bookshelfPos,
            boolean requiresBookshelf,
            QuizBank quizBank,
            ResourceLocation questionId,
            int questionNumber,
            String questionText,
            String correctAnswer,
            String[] wrongAnswers,
            String englishQuestionText,
            String englishCorrectAnswer,
            String[] englishWrongAnswers,
            int correctOptionIndex,
            int difficulty,
            int intelligenceLevel,
            int intelligenceExperience,
            int intelligenceRequiredXp
    ) {
        super(ModMenuTypes.BOOKSHELF_QUIZ.get(), containerId);
        this.bookshelfPos = bookshelfPos.immutable();
        this.requiresBookshelf = requiresBookshelf;
        this.quizBank = quizBank;
        this.questionId = questionId;
        this.questionNumber = questionNumber;
        this.questionText = questionText;
        this.correctAnswer = correctAnswer;
        this.wrongAnswers = wrongAnswers;
        this.englishQuestionText = englishQuestionText;
        this.englishCorrectAnswer = englishCorrectAnswer;
        this.englishWrongAnswers = englishWrongAnswers;
        this.correctOptionIndex = correctOptionIndex;
        this.difficulty = difficulty;
        this.intelligenceLevel = intelligenceLevel;
        this.intelligenceExperience = intelligenceExperience;
        this.intelligenceRequiredXp = intelligenceRequiredXp;
        this.answerState.set(UNANSWERED_STATE);
        this.addDataSlot(this.answerState);
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getQuestionText(boolean english) {
        return english ? englishQuestionText : questionText;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getCorrectAnswer(boolean english) {
        return english ? englishCorrectAnswer : correctAnswer;
    }

    public String[] getWrongAnswers() {
        return wrongAnswers;
    }

    public String[] getWrongAnswers(boolean english) {
        return english ? englishWrongAnswers : wrongAnswers;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public int getIntelligenceLevel() {
        return intelligenceLevel;
    }

    public int getIntelligenceExperience() {
        return intelligenceExperience;
    }

    public int getIntelligenceRequiredXp() {
        return intelligenceRequiredXp;
    }

    public boolean isAnswered() {
        return this.answerState.get() != UNANSWERED_STATE;
    }

    public boolean wasAnsweredCorrectly() {
        return this.answerState.get() >= CORRECT_STATE_OFFSET;
    }

    public int getSelectedOptionIndex() {
        int state = this.answerState.get();
        if (state >= CORRECT_STATE_OFFSET) {
            return state - CORRECT_STATE_OFFSET;
        }
        return state == UNANSWERED_STATE ? -1 : state - 1;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!this.requiresBookshelf) {
            return player.isAlive();
        }
        if (!player.level().getBlockState(this.bookshelfPos).is(Blocks.BOOKSHELF)) {
            return false;
        }

        double centerX = this.bookshelfPos.getX() + 0.5D;
        double centerY = this.bookshelfPos.getY() + 0.5D;
        double centerZ = this.bookshelfPos.getZ() + 0.5D;
        return player.distanceToSqr(centerX, centerY, centerZ) <= MAX_DISTANCE_SQUARED;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (!this.stillValid(player)) {
            serverPlayer.closeContainer();
            return false;
        }

        if (id == NEXT_QUESTION_BUTTON_ID) {
            if (!this.isAnswered()) {
                return false;
            }
            boolean opened = this.requiresBookshelf
                    ? BookshelfInteractionHandler.openQuiz(serverPlayer, this.bookshelfPos)
                    : QuizLauncher.openRemoteQuiz(serverPlayer, this.quizBank);
            if (!opened) {
                serverPlayer.closeContainer();
            }
            return true;
        }

        if (id == EXIT_BUTTON_ID) {
            serverPlayer.closeContainer();
            return true;
        }

        if (id < 0 || id >= ANSWER_BUTTON_COUNT || this.isAnswered()) {
            return false;
        }

        if (id == correctOptionIndex) {
            this.answerState.set(CORRECT_STATE_OFFSET + id);
            QuizProgressManager.recordCorrect(serverPlayer, this.quizBank, this.questionId);
            int experienceIqCap = (int) Math.min(
                    IntelligenceData.MAX_IQ,
                    10L * difficulty + 50L
            );
            IntelligenceManager.addExperienceCappedAtIq(
                    serverPlayer,
                    difficulty,
                    experienceIqCap
            );
            int level = IntelligenceManager.getEffectiveIq(serverPlayer);
            giveCorrectReward(serverPlayer, difficulty, level);
            serverPlayer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            this.answerState.set(id + 1);
            applyWrongPenalty(serverPlayer);
            serverPlayer.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        AntiAddictionManager.onQuestionAnswered(serverPlayer, difficulty);
        this.broadcastChanges();
        return true;
    }

    private static QuizBank requireBank(String dataId) {
        QuizBank bank = QuizBank.byDataId(dataId);
        if (bank == null) {
            throw new IllegalArgumentException("Unknown quiz bank " + dataId);
        }
        return bank;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void giveCorrectReward(ServerPlayer player, int difficulty, int level) {
        if (RewardManager.qualifiesForHighestTier(difficulty, level)
                && !HighestTierRewardLimit.tryClaim(player)) {
            player.sendSystemMessage(Component.translatable("message.mathmaster.highest_reward_daily_limit"));
            return;
        }

        for (ItemStack reward : RewardManager.getRewards(difficulty, level)) {
            giveItemStack(player, reward);
        }
    }

    private void applyWrongPenalty(ServerPlayer player) {
        switch (QuizPenaltyManager.roll()) {
            case LIGHTNING -> {
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, player.level());
                bolt.setPos(player.getX(), player.getY(), player.getZ());
                player.level().addFreshEntity(bolt);
            }
            case THREE_CAT_MILK_POWDER ->
                    giveItemStack(player, new ItemStack(ModItems.THREE_CAT_MILK_POWDER.get()));
            case DAMAGE -> player.hurt(player.damageSources().generic(), 1.0F);
            case BLINDNESS -> player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            case NONE -> {
            }
        }
    }

    private void giveItemStack(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.spawnAtLocation(stack);
        }
    }
}
