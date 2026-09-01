package com.freshfish.mathmaster.menu;

import com.freshfish.mathmaster.antiaddiction.AntiAddictionManager;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMenuTypes;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.quiz.QuizQuestion;
import com.freshfish.mathmaster.reward.RewardManager;
import com.freshfish.mathmaster.reward.HighestTierRewardLimit;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class BookshelfQuizMenu extends AbstractContainerMenu {
    private final int questionNumber;
    private final String questionText;
    private final String correctAnswer;
    private final String[] wrongAnswers;
    private final int correctOptionIndex;
    private final int difficulty;
    private final int intelligenceLevel;
    private final int intelligenceExperience;
    private final int intelligenceRequiredXp;

    public BookshelfQuizMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(
                containerId,
                playerInventory,
                data.readInt(),
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
                questionNumber,
                question.question(),
                question.correctAnswer(),
                question.wrongAnswers().toArray(new String[0]),
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
            int questionNumber,
            String questionText,
            String correctAnswer,
            String[] wrongAnswers,
            int correctOptionIndex,
            int difficulty,
            int intelligenceLevel,
            int intelligenceExperience,
            int intelligenceRequiredXp
    ) {
        super(ModMenuTypes.BOOKSHELF_QUIZ.get(), containerId);
        this.questionNumber = questionNumber;
        this.questionText = questionText;
        this.correctAnswer = correctAnswer;
        this.wrongAnswers = wrongAnswers;
        this.correctOptionIndex = correctOptionIndex;
        this.difficulty = difficulty;
        this.intelligenceLevel = intelligenceLevel;
        this.intelligenceExperience = intelligenceExperience;
        this.intelligenceRequiredXp = intelligenceRequiredXp;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String[] getWrongAnswers() {
        return wrongAnswers;
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

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (id < 0 || id > 3) {
            return false;
        }

        if (id == correctOptionIndex) {
            IntelligenceManager.addExperience(serverPlayer, difficulty);
            int level = IntelligenceManager.get(serverPlayer).getIq();
            giveCorrectReward(serverPlayer, difficulty, level);
            serverPlayer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            applyWrongPenalty(serverPlayer);
        }

        AntiAddictionManager.onQuestionAnswered(serverPlayer, difficulty);
        serverPlayer.closeContainer();
        return true;
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
        double roll = ThreadLocalRandom.current().nextDouble();

        if (roll < 0.001) {
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, player.level());
            bolt.setPos(player.getX(), player.getY(), player.getZ());
            player.level().addFreshEntity(bolt);
        } else if (roll < 0.011) {
            giveItemStack(player, new ItemStack(ModItems.THREE_CAT_MILK_POWDER.get()));
        } else if (roll < 0.111) {
            player.hurt(player.damageSources().generic(), 1.0F);
        } else if (roll < 0.311) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }
    }

    private void giveItemStack(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.spawnAtLocation(stack);
        }
    }
}
