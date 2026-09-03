package com.freshfish.mathmaster.intellect;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public enum InsightDifficulty {
    ENLIGHTENMENT_SHORT(0, 3, weighted("grade_2", 100)),
    ENLIGHTENMENT_FULL(30, 5, weighted("grade_2", 100)),
    LOW(40, 7, weighted("grade_2", 90), weighted("junior_high", 10)),
    INTERMEDIATE(
            70,
            10,
            weighted("grade_2", 60),
            weighted("junior_high", 30),
            weighted("senior_high", 10)
    ),
    HIGH(
            100,
            12,
            weighted("grade_2", 30),
            weighted("junior_high", 30),
            weighted("senior_high", 30),
            weighted("advanced", 10)
    ),
    HELL(
            140,
            15,
            weighted("junior_high", 30),
            weighted("senior_high", 30),
            weighted("advanced", 30),
            weighted("millennium_problems", 10)
    );

    private final int minimumIntellect;
    private final int questionCount;
    private final List<WeightedBank> weightedBanks;

    InsightDifficulty(
            int minimumIntellect,
            int questionCount,
            WeightedBank... weightedBanks
    ) {
        if (questionCount <= 0) {
            throw new IllegalArgumentException("Insight question count must be positive");
        }
        int totalWeight = java.util.Arrays.stream(weightedBanks)
                .mapToInt(WeightedBank::weight)
                .sum();
        if (totalWeight != 100) {
            throw new IllegalArgumentException("Insight quiz bank weights must total 100");
        }
        this.minimumIntellect = minimumIntellect;
        this.questionCount = questionCount;
        this.weightedBanks = List.of(weightedBanks);
    }

    public int minimumIntellect() {
        return this.minimumIntellect;
    }

    public int questionCount() {
        return this.questionCount;
    }

    public List<WeightedBank> weightedBanks() {
        return this.weightedBanks;
    }

    public static InsightDifficulty forIntellect(int intellect) {
        InsightDifficulty selected = ENLIGHTENMENT_SHORT;
        for (InsightDifficulty difficulty : values()) {
            if (intellect < difficulty.minimumIntellect) {
                break;
            }
            selected = difficulty;
        }
        return selected;
    }

    private static WeightedBank weighted(String path, int weight) {
        return new WeightedBank(
                ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, path),
                weight
        );
    }

    public record WeightedBank(ResourceLocation bankId, int weight) {
        public WeightedBank {
            if (weight <= 0) {
                throw new IllegalArgumentException("Insight quiz bank weight must be positive");
            }
        }
    }
}
