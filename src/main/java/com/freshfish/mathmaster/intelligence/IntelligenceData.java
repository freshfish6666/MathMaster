package com.freshfish.mathmaster.intelligence;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.concurrent.ThreadLocalRandom;

public final class IntelligenceData implements INBTSerializable<CompoundTag> {
    private static final int MIN_BIRTH_IQ = 30;
    private static final int MAX_BIRTH_IQ = 50;
    public static final int MAX_IQ = 200;
    private static final int BASE_XP = 10;
    private static final int XP_PER_IQ = 1;
    private static final long MAX_TOTAL_EXPERIENCE = totalExperienceForIq(MAX_IQ);

    private long totalExperience;

    public IntelligenceData() {
        int birthIq = ThreadLocalRandom.current().nextInt(MIN_BIRTH_IQ, MAX_BIRTH_IQ + 1);
        this.totalExperience = totalExperienceForIq(birthIq);
    }

    public int getIq() {
        return iqFromTotalExperience(this.totalExperience);
    }

    public int getExperience() {
        int iq = this.getIq();
        if (iq >= MAX_IQ) {
            return 0;
        }
        return Math.toIntExact(this.totalExperience - totalExperienceForIq(iq));
    }

    public int getXpNeededForNextIq() {
        int iq = this.getIq();
        return iq >= MAX_IQ ? 0 : requiredExperience(iq);
    }

    public long getTotalExperience() {
        return this.totalExperience;
    }

    public void addExperience(int amount) {
        if (amount == 0) {
            return;
        }

        this.totalExperience = clampTotalExperience(this.totalExperience + (long) amount);
    }

    public void addIq(int amount) {
        if (amount <= 0) {
            return;
        }

        int currentIq = this.getIq();
        int currentProgress = this.getExperience();
        long requestedIq = (long) currentIq + amount;
        int newIq = (int) Math.min(MAX_IQ, requestedIq);
        this.totalExperience = totalExperienceForIq(newIq);
        if (newIq < MAX_IQ) {
            this.totalExperience += Math.min(currentProgress, requiredExperience(newIq) - 1);
        }
    }

    public void setIq(int iq) {
        int clampedIq = Math.max(MIN_BIRTH_IQ, Math.min(MAX_IQ, iq));
        this.totalExperience = totalExperienceForIq(clampedIq);
    }

    public boolean setExperience(int experience) {
        int needed = this.getXpNeededForNextIq();
        if (needed == 0) {
            return experience == 0;
        }
        if (experience < 0 || experience >= needed) {
            return false;
        }
        this.totalExperience = totalExperienceForIq(this.getIq()) + experience;
        return true;
    }

    private static int iqFromTotalExperience(long totalExperience) {
        long clampedTotal = clampTotalExperience(totalExperience);
        if (clampedTotal >= MAX_TOTAL_EXPERIENCE) {
            return MAX_IQ;
        }

        double linearCoefficient = 2.0 * BASE_XP - XP_PER_IQ;
        double discriminant = linearCoefficient * linearCoefficient
                + 8.0 * XP_PER_IQ * clampedTotal;
        int levelOffset = (int) Math.floor(
                (Math.sqrt(discriminant) - linearCoefficient) / (2.0 * XP_PER_IQ)
        );
        int maxOffset = MAX_IQ - MIN_BIRTH_IQ;
        levelOffset = Math.max(0, Math.min(maxOffset, levelOffset));

        while (levelOffset < maxOffset
                && totalExperienceForOffset(levelOffset + 1) <= clampedTotal) {
            levelOffset++;
        }
        while (levelOffset > 0 && totalExperienceForOffset(levelOffset) > clampedTotal) {
            levelOffset--;
        }
        return MIN_BIRTH_IQ + levelOffset;
    }

    private static int requiredExperience(int iq) {
        return BASE_XP + Math.max(0, iq - MIN_BIRTH_IQ) * XP_PER_IQ;
    }

    private static long totalExperienceForIq(int iq) {
        int clampedIq = Math.max(MIN_BIRTH_IQ, Math.min(MAX_IQ, iq));
        return totalExperienceForOffset(clampedIq - MIN_BIRTH_IQ);
    }

    private static long totalExperienceForOffset(long levelOffset) {
        return levelOffset
                * (2L * BASE_XP + (levelOffset - 1L) * XP_PER_IQ)
                / 2L;
    }

    private static long clampTotalExperience(long totalExperience) {
        return Math.max(0L, Math.min(MAX_TOTAL_EXPERIENCE, totalExperience));
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("total_experience", this.totalExperience);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt.contains("total_experience")) {
            this.totalExperience = clampTotalExperience(nbt.getLong("total_experience"));
            return;
        }

        int savedIq = nbt.contains("iq") ? nbt.getInt("iq") : nbt.getInt("level");
        int clampedIq = Math.max(MIN_BIRTH_IQ, Math.min(MAX_IQ, savedIq));
        long migratedExperience = totalExperienceForIq(clampedIq)
                + Math.max(0, nbt.getInt("experience"));
        this.totalExperience = clampTotalExperience(migratedExperience);
    }
}
