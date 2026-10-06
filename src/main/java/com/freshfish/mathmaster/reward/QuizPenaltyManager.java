package com.freshfish.mathmaster.reward;

import java.util.concurrent.ThreadLocalRandom;

/** Shared outcome distribution for an incorrect MathMaster answer. */
public final class QuizPenaltyManager {
    private QuizPenaltyManager() {
    }

    public static Penalty roll() {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.001) {
            return Penalty.LIGHTNING;
        }
        if (roll < 0.011) {
            return Penalty.THREE_CAT_MILK_POWDER;
        }
        if (roll < 0.111) {
            return Penalty.DAMAGE;
        }
        if (roll < 0.311) {
            return Penalty.BLINDNESS;
        }
        return Penalty.NONE;
    }

    public enum Penalty {
        LIGHTNING,
        THREE_CAT_MILK_POWDER,
        DAMAGE,
        BLINDNESS,
        NONE
    }
}
