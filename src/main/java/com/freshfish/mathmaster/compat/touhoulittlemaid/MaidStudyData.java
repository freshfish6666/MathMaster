package com.freshfish.mathmaster.compat.touhoulittlemaid;

import com.freshfish.mathmaster.intelligence.IntelligenceData;
import com.freshfish.mathmaster.quiz.QuizBank;
import com.freshfish.mathmaster.quiz.QuizQuestion;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.concurrent.ThreadLocalRandom;

final class MaidStudyData {
    private static final int MIN_IQ = 30;
    private static final int MIN_BIRTH_IQ = 40;
    private static final int MAX_BIRTH_IQ = 60;
    private static final int BASE_XP = 20;
    private static final int XP_PER_IQ = 2;
    private static final long MAX_TOTAL_EXPERIENCE = totalExperienceForIq(IntelligenceData.MAX_IQ);

    private long totalExperience;
    private long highestRewardDay = Long.MIN_VALUE;
    private int highestRewardClaims;

    transient BlockPos lecternPos;
    transient QuizBank quizBank;
    transient QuizQuestion question;
    transient int answerTicks;
    transient int resultTicks;
    transient int searchCooldown;
    transient int travelTicks;
    transient long bubbleId = -1L;
    transient boolean active;

    MaidStudyData() {
        int birthIq = ThreadLocalRandom.current().nextInt(MIN_BIRTH_IQ, MAX_BIRTH_IQ + 1);
        this.totalExperience = totalExperienceForIq(birthIq);
    }

    int getIq() {
        long clamped = clampTotalExperience(this.totalExperience);
        if (clamped >= MAX_TOTAL_EXPERIENCE) {
            return IntelligenceData.MAX_IQ;
        }

        double linearCoefficient = 2.0 * BASE_XP - XP_PER_IQ;
        double discriminant = linearCoefficient * linearCoefficient
                + 8.0 * XP_PER_IQ * clamped;
        int offset = (int) Math.floor(
                (Math.sqrt(discriminant) - linearCoefficient) / (2.0 * XP_PER_IQ)
        );
        int maximumOffset = IntelligenceData.MAX_IQ - MIN_IQ;
        offset = Math.max(0, Math.min(maximumOffset, offset));

        while (offset < maximumOffset && totalExperienceForOffset(offset + 1L) <= clamped) {
            offset++;
        }
        while (offset > 0 && totalExperienceForOffset(offset) > clamped) {
            offset--;
        }
        return MIN_IQ + offset;
    }

    int addExperienceCappedAtIq(int amount, int maximumIq) {
        if (amount <= 0) {
            return 0;
        }

        int clampedMaximumIq = Math.max(MIN_IQ, Math.min(IntelligenceData.MAX_IQ, maximumIq));
        if (this.getIq() > clampedMaximumIq) {
            return 0;
        }

        long cap = clampedMaximumIq >= IntelligenceData.MAX_IQ
                ? MAX_TOTAL_EXPERIENCE
                : totalExperienceForIq(clampedMaximumIq + 1) - 1L;
        long oldTotal = this.totalExperience;
        this.totalExperience = Math.min(cap, this.totalExperience + amount);
        return Math.toIntExact(this.totalExperience - oldTotal);
    }

    boolean tryClaimHighestReward(long gameDay, int dailyLimit) {
        if (this.highestRewardDay != gameDay) {
            this.highestRewardDay = gameDay;
            this.highestRewardClaims = 0;
        }
        if (this.highestRewardClaims >= dailyLimit) {
            return false;
        }
        this.highestRewardClaims++;
        return true;
    }

    void resetRuntime() {
        this.lecternPos = null;
        this.quizBank = null;
        this.question = null;
        this.answerTicks = 0;
        this.resultTicks = 0;
        this.searchCooldown = 0;
        this.travelTicks = 0;
        this.active = false;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("total_experience", this.totalExperience);
        tag.putLong("highest_reward_day", this.highestRewardDay);
        tag.putInt("highest_reward_claims", this.highestRewardClaims);
        return tag;
    }

    static MaidStudyData load(CompoundTag tag) {
        MaidStudyData data = new MaidStudyData();
        if (tag.contains("total_experience")) {
            data.totalExperience = clampTotalExperience(tag.getLong("total_experience"));
        }
        data.highestRewardDay = tag.contains("highest_reward_day")
                ? tag.getLong("highest_reward_day")
                : Long.MIN_VALUE;
        data.highestRewardClaims = Math.max(0, tag.getInt("highest_reward_claims"));
        return data;
    }

    private static long totalExperienceForIq(int iq) {
        int clampedIq = Math.max(MIN_IQ, Math.min(IntelligenceData.MAX_IQ, iq));
        return totalExperienceForOffset(clampedIq - MIN_IQ);
    }

    private static long totalExperienceForOffset(long offset) {
        return offset * (2L * BASE_XP + (offset - 1L) * XP_PER_IQ) / 2L;
    }

    private static long clampTotalExperience(long totalExperience) {
        return Math.max(0L, Math.min(MAX_TOTAL_EXPERIENCE, totalExperience));
    }
}
