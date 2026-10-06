package com.freshfish.mathmaster.compat.touhoulittlemaid;

import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.intelligence.IntelligenceData;
import com.freshfish.mathmaster.quiz.QuizBank;
import com.freshfish.mathmaster.quiz.QuizQuestion;
import com.freshfish.mathmaster.quiz.QuizQuestionManager;
import com.freshfish.mathmaster.reward.HighestTierRewardLimit;
import com.freshfish.mathmaster.reward.QuizPenaltyManager;
import com.freshfish.mathmaster.reward.RewardManager;
import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.WaitingChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

final class MaidStudyHandler {
    private static final int SEARCH_INTERVAL_TICKS = 100;
    private static final int SEARCH_RADIUS = 12;
    private static final int VERTICAL_SEARCH_RADIUS = 4;
    private static final int RESULT_DISPLAY_TICKS = 60;
    private static final int MAX_TRAVEL_TICKS = 20 * 30;
    private static final int MIN_ANSWER_TICKS = 20 * 2;
    private static final int MAX_ANSWER_TICKS = 20 * 600;
    private static final double LECTERN_REACH_DISTANCE_SQUARED = 1.75D * 1.75D;

    @SubscribeEvent
    public void onMaidTick(MaidTickEvent event) {
        EntityMaid maid = event.getMaid();
        if (maid.level().isClientSide) {
            return;
        }

        TaskDataKey<MaidStudyData> key = MathMasterLittleMaidExtension.getStudyDataKey();
        if (key == null) {
            return;
        }

        MaidStudyData existingData = maid.getData(key);
        boolean canStudy = MathMasterConfig.isTouhouLittleMaidIntegrationEnabled()
                && maid.isAlive()
                && MaidStudyTask.ID.equals(maid.getTask().getUid())
                && !maid.isMaidInSittingPose()
                && maid.getScheduleDetail() == Activity.WORK;
        if (!canStudy) {
            if (existingData != null && existingData.active) {
                clearRuntime(maid, existingData);
            }
            return;
        }

        MaidStudyData data = existingData != null
                ? existingData
                : maid.getOrCreateData(key, new MaidStudyData());
        data.active = true;
        tickStudy(maid, data);
    }

    private static void tickStudy(EntityMaid maid, MaidStudyData data) {
        ServerLevel level = (ServerLevel) maid.level();

        if (data.resultTicks > 0) {
            data.resultTicks--;
            faceLectern(maid, data.lecternPos);
            if (data.resultTicks == 0) {
                data.question = null;
                data.quizBank = null;
                removeBubble(maid, data);
            }
            return;
        }

        QuizBank lecternBank = getLecternBank(level, data.lecternPos);
        if (lecternBank == null) {
            data.lecternPos = null;
            data.question = null;
            data.quizBank = null;
            data.answerTicks = 0;
            data.travelTicks = 0;
            removeBubble(maid, data);

            if (data.searchCooldown-- > 0) {
                return;
            }
            data.searchCooldown = SEARCH_INTERVAL_TICKS;
            data.lecternPos = findNearestLectern(maid, level);
            lecternBank = getLecternBank(level, data.lecternPos);
            if (lecternBank == null) {
                return;
            }
        }

        double distanceSquared = maid.distanceToSqr(
                data.lecternPos.getX() + 0.5D,
                data.lecternPos.getY() + 0.75D,
                data.lecternPos.getZ() + 0.5D
        );
        if (distanceSquared > LECTERN_REACH_DISTANCE_SQUARED) {
            data.travelTicks++;
            if (data.travelTicks > MAX_TRAVEL_TICKS) {
                data.lecternPos = null;
                data.travelTicks = 0;
                data.searchCooldown = SEARCH_INTERVAL_TICKS;
                maid.getNavigation().stop();
                return;
            }
            if (maid.tickCount % 20 == 0 || maid.getNavigation().isDone()) {
                maid.getNavigation().moveTo(
                        data.lecternPos.getX() + 0.5D,
                        data.lecternPos.getY(),
                        data.lecternPos.getZ() + 0.5D,
                        0.6D
                );
            }
            faceLectern(maid, data.lecternPos);
            return;
        }

        data.travelTicks = 0;
        maid.getNavigation().stop();
        faceLectern(maid, data.lecternPos);

        if (data.question == null) {
            if (!startQuestion(maid, data, lecternBank)) {
                data.lecternPos = null;
            }
            return;
        }

        if (data.quizBank != lecternBank) {
            clearRuntime(maid, data);
            return;
        }

        if (data.answerTicks > 0) {
            data.answerTicks--;
        }
        if (data.answerTicks == 0) {
            finishQuestion(maid, data);
        }
    }

    private static boolean startQuestion(EntityMaid maid, MaidStudyData data, QuizBank bank) {
        List<QuizQuestion> questions = QuizQuestionManager.getQuestions(bank);
        if (questions.isEmpty()) {
            return false;
        }

        data.quizBank = bank;
        data.question = questions.get(maid.getRandom().nextInt(questions.size()));
        data.answerTicks = calculateAnswerTicks(data.getIq(), bank.difficulty());
        setBubble(
                maid,
                data,
                data.answerTicks + 40,
                questionWithIq(data),
                Component.translatable("bubble.mathmaster.maid_study.thinking")
        );
        return true;
    }

    private static void finishQuestion(EntityMaid maid, MaidStudyData data) {
        int difficulty = data.quizBank.difficulty();
        int iqBeforeAnswer = data.getIq();
        boolean correct = maid.getRandom().nextDouble() < calculateCorrectChance(iqBeforeAnswer, difficulty);

        if (correct) {
            int experienceIqCap = (int) Math.min(
                    IntelligenceData.MAX_IQ,
                    10L * difficulty + 50L
            );
            data.addExperienceCappedAtIq(difficulty, experienceIqCap);
            giveCorrectRewards(maid, data, difficulty, data.getIq());
            maid.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        } else {
            applyWrongPenalty(maid);
            maid.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
        }

        data.answerTicks = 0;
        data.resultTicks = RESULT_DISPLAY_TICKS;
        setBubble(
                maid,
                data,
                RESULT_DISPLAY_TICKS,
                questionWithIq(data),
                Component.translatable(correct
                        ? "bubble.mathmaster.maid_study.correct"
                        : "bubble.mathmaster.maid_study.wrong")
        );
    }

    private static void giveCorrectRewards(EntityMaid maid, MaidStudyData data, int difficulty, int iq) {
        if (RewardManager.qualifiesForHighestTier(difficulty, iq)) {
            long dayTime = ((ServerLevel) maid.level()).getServer().overworld().getDayTime();
            long gameDay = Math.floorDiv(dayTime, 24000L);
            if (!data.tryClaimHighestReward(gameDay, HighestTierRewardLimit.DAILY_LIMIT)) {
                return;
            }
        }

        for (ItemStack reward : RewardManager.getRewards(difficulty, iq)) {
            giveItemStack(maid, reward);
        }
    }

    private static void applyWrongPenalty(EntityMaid maid) {
        switch (QuizPenaltyManager.roll()) {
            case LIGHTNING, NONE -> {
                // Maids intentionally ignore MathMaster's lightning punishment.
            }
            case THREE_CAT_MILK_POWDER ->
                    giveItemStack(maid, new ItemStack(ModItems.THREE_CAT_MILK_POWDER.get()));
            case DAMAGE -> maid.hurt(maid.damageSources().generic(), 1.0F);
            case BLINDNESS -> maid.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }
    }

    private static void giveItemStack(EntityMaid maid, ItemStack stack) {
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                maid.getAvailableBackpackInv(),
                stack.copy(),
                false
        );
        if (!remainder.isEmpty()) {
            maid.spawnAtLocation(remainder);
        }
    }

    private static BlockPos findNearestLectern(EntityMaid maid, ServerLevel level) {
        BlockPos origin = maid.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = -VERTICAL_SEARCH_RADIUS; y <= VERTICAL_SEARCH_RADIUS; y++) {
            for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.isLoaded(cursor)
                            || !level.getBlockState(cursor).is(Blocks.LECTERN)
                            || getLecternBank(level, cursor) == null
                            || (maid.hasRestriction() && !maid.isWithinRestriction(cursor))) {
                        continue;
                    }

                    double distance = cursor.distSqr(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    private static QuizBank getLecternBank(ServerLevel level, BlockPos pos) {
        if (pos == null || !level.isLoaded(pos)
                || !level.getBlockState(pos).is(Blocks.LECTERN)
                || !(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)) {
            return null;
        }
        QuizBank bank = QuizBank.byItem(lectern.getBook().getItem());
        return bank != null && bank.hasQuestions() ? bank : null;
    }

    private static void faceLectern(EntityMaid maid, BlockPos pos) {
        if (pos == null) {
            return;
        }
        maid.getLookControl().setLookAt(
                pos.getX() + 0.5D,
                pos.getY() + 0.8D,
                pos.getZ() + 0.5D,
                30.0F,
                30.0F
        );
    }

    private static Component questionWithIq(MaidStudyData data) {
        return Component.translatable(
                "bubble.mathmaster.maid_study.question",
                data.getIq(),
                data.question.question()
        );
    }

    private static int calculateAnswerTicks(int iq, int difficulty) {
        double iqFactor = 10.0D * Math.exp(-0.03D * (iq - 50.0D));
        double highDifficultyFactor = 1.0D
                + Math.max(0.0D, difficulty - 10.0D) / 10.0D
                * (0.5D + Math.max(0.0D, iq - 100.0D) / 100.0D);
        double seconds = iqFactor * difficulty * highDifficultyFactor;
        return Mth.clamp((int) Math.round(seconds * 20.0D), MIN_ANSWER_TICKS, MAX_ANSWER_TICKS);
    }

    private static double calculateCorrectChance(int iq, int difficulty) {
        double targetIq = 40.0D + 4.0D * difficulty;
        return Mth.clamp(0.75D + (iq - targetIq) * 0.01D, 0.10D, 0.95D);
    }

    private static void setBubble(
            EntityMaid maid,
            MaidStudyData data,
            int duration,
            Component question,
            Component status
    ) {
        removeBubble(maid, data);
        data.bubbleId = maid.getChatBubbleManager().addChatBubble(
                WaitingChatBubbleData.create(
                        duration,
                        IChatBubbleData.TYPE_2,
                        IChatBubbleData.DEFAULT_PRIORITY,
                        question,
                        status,
                        bubbleIcon(data.quizBank)
                )
        );
    }

    private static ResourceLocation bubbleIcon(QuizBank bank) {
        QuizBank displayedBank = bank != null ? bank : QuizBank.GRADE_2_MATH;
        ResourceLocation bookId = BuiltInRegistries.ITEM.getKey(displayedBank.bookItem());
        return ResourceLocation.fromNamespaceAndPath(
                bookId.getNamespace(),
                "textures/item/" + bookId.getPath() + ".png"
        );
    }

    private static void removeBubble(EntityMaid maid, MaidStudyData data) {
        if (data.bubbleId >= 0L) {
            maid.getChatBubbleManager().removeChatBubble(data.bubbleId);
            data.bubbleId = -1L;
        }
    }

    private static void clearRuntime(EntityMaid maid, MaidStudyData data) {
        removeBubble(maid, data);
        data.resetRuntime();
        maid.getNavigation().stop();
    }
}
