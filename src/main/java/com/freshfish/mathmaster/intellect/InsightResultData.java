package com.freshfish.mathmaster.intellect;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

public final class InsightResultData implements INBTSerializable<CompoundTag> {
    private static final int MAX_TRACKED_ENTITY_TYPES = 4_096;

    private long completedInsights;
    private long cumulativeCorrectAnswers;
    private long cumulativeQuestions;
    private int lastCorrectAnswers;
    private int lastTotalQuestions;
    private boolean nAltarUnlocked;
    private final Set<ResourceLocation> successfullyInsightEntityTypes = new HashSet<>();

    public void record(
            ResourceLocation entityTypeId,
            int correctAnswers,
            int totalQuestions
    ) {
        int safeTotal = Math.max(0, totalQuestions);
        int safeCorrect = Math.max(0, Math.min(correctAnswers, safeTotal));
        this.completedInsights++;
        this.cumulativeCorrectAnswers += safeCorrect;
        this.cumulativeQuestions += safeTotal;
        this.lastCorrectAnswers = safeCorrect;
        this.lastTotalQuestions = safeTotal;
        if (safeTotal > 0
                && safeCorrect == safeTotal
                && this.successfullyInsightEntityTypes.size() < MAX_TRACKED_ENTITY_TYPES) {
            this.successfullyInsightEntityTypes.add(entityTypeId);
        }
    }

    public long getCompletedInsights() {
        return this.completedInsights;
    }

    public long getCumulativeCorrectAnswers() {
        return this.cumulativeCorrectAnswers;
    }

    public long getCumulativeQuestions() {
        return this.cumulativeQuestions;
    }

    public int getLastCorrectAnswers() {
        return this.lastCorrectAnswers;
    }

    public int getLastTotalQuestions() {
        return this.lastTotalQuestions;
    }

    public int getDistinctEntityTypeCount() {
        return this.successfullyInsightEntityTypes.size();
    }

    public boolean hasUnlockedNAltar() {
        return this.nAltarUnlocked;
    }

    public void unlockNAltar() {
        this.nAltarUnlocked = true;
    }

    public boolean hasSuccessfullyInsighted(ResourceLocation entityTypeId) {
        return this.successfullyInsightEntityTypes.contains(entityTypeId);
    }

    public Set<ResourceLocation> getSuccessfullyInsightEntityTypes() {
        return Set.copyOf(this.successfullyInsightEntityTypes);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("completed_insights", this.completedInsights);
        tag.putLong("cumulative_correct_answers", this.cumulativeCorrectAnswers);
        tag.putLong("cumulative_questions", this.cumulativeQuestions);
        tag.putInt("last_correct_answers", this.lastCorrectAnswers);
        tag.putInt("last_total_questions", this.lastTotalQuestions);
        ListTag entityTypes = new ListTag();
        this.successfullyInsightEntityTypes.stream()
                .sorted()
                .map(ResourceLocation::toString)
                .map(StringTag::valueOf)
                .forEach(entityTypes::add);
        tag.put("successfully_insight_entity_types", entityTypes);
        tag.putBoolean("n_altar_unlocked", this.nAltarUnlocked);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.nAltarUnlocked = tag.getBoolean("n_altar_unlocked");
        this.completedInsights = Math.max(0L, tag.getLong("completed_insights"));
        this.cumulativeCorrectAnswers = Math.max(0L, tag.getLong("cumulative_correct_answers"));
        this.cumulativeQuestions = Math.max(
                this.cumulativeCorrectAnswers,
                tag.getLong("cumulative_questions")
        );
        this.lastTotalQuestions = Math.max(0, tag.getInt("last_total_questions"));
        this.lastCorrectAnswers = Math.max(
                0,
                Math.min(tag.getInt("last_correct_answers"), this.lastTotalQuestions)
        );
        this.successfullyInsightEntityTypes.clear();
        ListTag entityTypes = tag.getList("successfully_insight_entity_types", Tag.TAG_STRING);
        for (int index = 0;
             index < entityTypes.size()
                     && this.successfullyInsightEntityTypes.size() < MAX_TRACKED_ENTITY_TYPES;
             index++) {
            ResourceLocation entityTypeId = ResourceLocation.tryParse(entityTypes.getString(index));
            if (entityTypeId != null) {
                this.successfullyInsightEntityTypes.add(entityTypeId);
            }
        }
    }
}
