package com.freshfish.mathmaster.intellect;

public record EntityIntellectDefinition(int intellect) {
    public EntityIntellectDefinition {
        if (intellect < 0) {
            throw new IllegalArgumentException("Entity intellect must not be negative");
        }
    }

    public InsightDifficulty difficulty() {
        return InsightDifficulty.forIntellect(this.intellect);
    }
}
