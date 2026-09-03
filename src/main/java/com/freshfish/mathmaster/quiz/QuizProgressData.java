package com.freshfish.mathmaster.quiz;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class QuizProgressData implements INBTSerializable<CompoundTag> {
    public static final int RECENT_CORRECT_LIMIT = 10;
    public static final StreamCodec<RegistryFriendlyByteBuf, QuizProgressData> STREAM_CODEC =
            StreamCodec.of(QuizProgressData::writeNetwork, QuizProgressData::readNetwork);
    private static final int MAX_MASTERY = 2;

    private final EnumMap<QuizBank, Map<ResourceLocation, Integer>> masteryByBank =
            new EnumMap<>(QuizBank.class);
    private final EnumMap<QuizBank, Deque<ResourceLocation>> recentCorrectByBank =
            new EnumMap<>(QuizBank.class);
    private final int[] syncedCorrectCounts = new int[QuizBank.values().length];
    private final int[] syncedTotalCounts = new int[QuizBank.values().length];

    public QuizProgressData() {
        clear();
    }

    public int getMastery(QuizBank bank, ResourceLocation questionId) {
        return this.masteryByBank.get(bank).getOrDefault(questionId, 0);
    }

    public Set<ResourceLocation> getCorrectQuestionIds(QuizBank bank) {
        return Set.copyOf(this.masteryByBank.get(bank).keySet());
    }

    public int getDisplayCorrectCount(QuizBank bank) {
        int syncedCount = this.syncedCorrectCounts[bank.ordinal()];
        return syncedCount >= 0 ? syncedCount : this.masteryByBank.get(bank).size();
    }

    public int getDisplayTotalCount(QuizBank bank) {
        return this.syncedTotalCounts[bank.ordinal()];
    }

    public boolean wasRecentlyCorrect(QuizBank bank, ResourceLocation questionId) {
        return this.recentCorrectByBank.get(bank).contains(questionId);
    }

    public void recordCorrect(QuizBank bank, ResourceLocation questionId) {
        this.masteryByBank.get(bank).merge(
                questionId,
                1,
                (current, increment) -> Math.min(MAX_MASTERY, current + increment)
        );

        Deque<ResourceLocation> recent = this.recentCorrectByBank.get(bank);
        recent.remove(questionId);
        recent.addLast(questionId);
        while (recent.size() > RECENT_CORRECT_LIMIT) {
            recent.removeFirst();
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        CompoundTag banks = new CompoundTag();

        for (QuizBank bank : QuizBank.values()) {
            CompoundTag bankTag = new CompoundTag();
            ListTag masteryList = new ListTag();
            this.masteryByBank.get(bank).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        CompoundTag entryTag = new CompoundTag();
                        entryTag.putString("id", entry.getKey().toString());
                        entryTag.putByte("mastery", entry.getValue().byteValue());
                        masteryList.add(entryTag);
                    });
            bankTag.put("mastery", masteryList);

            ListTag recentList = new ListTag();
            for (ResourceLocation questionId : this.recentCorrectByBank.get(bank)) {
                recentList.add(StringTag.valueOf(questionId.toString()));
            }
            bankTag.put("recent_correct", recentList);
            banks.put(bank.dataId(), bankTag);
        }

        root.put("banks", banks);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag root) {
        clear();
        CompoundTag banks = root.getCompound("banks");

        for (QuizBank bank : QuizBank.values()) {
            CompoundTag bankTag = banks.getCompound(bank.dataId());
            ListTag masteryList = bankTag.getList("mastery", Tag.TAG_COMPOUND);
            Map<ResourceLocation, Integer> mastery = this.masteryByBank.get(bank);
            for (int index = 0; index < masteryList.size(); index++) {
                CompoundTag entryTag = masteryList.getCompound(index);
                ResourceLocation questionId = ResourceLocation.tryParse(entryTag.getString("id"));
                int level = Math.max(1, Math.min(MAX_MASTERY, entryTag.getByte("mastery")));
                if (questionId != null) {
                    mastery.put(questionId, level);
                }
            }

            ListTag recentList = bankTag.getList("recent_correct", Tag.TAG_STRING);
            Deque<ResourceLocation> recent = this.recentCorrectByBank.get(bank);
            int firstIndex = Math.max(0, recentList.size() - RECENT_CORRECT_LIMIT);
            for (int index = firstIndex; index < recentList.size(); index++) {
                ResourceLocation questionId = ResourceLocation.tryParse(recentList.getString(index));
                if (questionId != null) {
                    recent.remove(questionId);
                    recent.addLast(questionId);
                }
            }
        }
    }

    private void clear() {
        this.masteryByBank.clear();
        this.recentCorrectByBank.clear();
        for (QuizBank bank : QuizBank.values()) {
            this.masteryByBank.put(bank, new HashMap<>());
            this.recentCorrectByBank.put(bank, new ArrayDeque<>());
            this.syncedCorrectCounts[bank.ordinal()] = -1;
            this.syncedTotalCounts[bank.ordinal()] = -1;
        }
    }

    private static void writeNetwork(RegistryFriendlyByteBuf buffer, QuizProgressData data) {
        for (QuizBank bank : QuizBank.values()) {
            Set<ResourceLocation> correctIds = data.masteryByBank.get(bank).keySet();
            var loadedQuestions = QuizQuestionManager.getQuestions(bank);
            int correctCount = (int) loadedQuestions.stream()
                    .filter(question -> correctIds.contains(question.id()))
                    .count();
            buffer.writeVarInt(correctCount);
            buffer.writeVarInt(loadedQuestions.size());
        }
    }

    private static QuizProgressData readNetwork(RegistryFriendlyByteBuf buffer) {
        QuizProgressData data = new QuizProgressData();
        for (QuizBank bank : QuizBank.values()) {
            data.syncedCorrectCounts[bank.ordinal()] = buffer.readVarInt();
            data.syncedTotalCounts[bank.ordinal()] = buffer.readVarInt();
        }
        return data;
    }
}
