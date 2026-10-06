package com.freshfish.mathmaster.pollution;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class DigitalPollutionData implements INBTSerializable<CompoundTag> {
    public static final int MAX_VALUE = 100;
    public static final int OVERWORLD_REDUCTION_INTERVAL_TICKS = 20 * 60;
    public static final int OVERWORLD_REDUCTION_AMOUNT = 1;
    public static final int SLEEP_REDUCTION_AMOUNT = 10;
    public static final int MOB_DECAY_INTERVAL_TICKS = 20 * 60;

    private int value;
    private int overworldTicks;
    private double effectProgress;
    private int identityShields;
    private int identityShieldTicks;
    private int mobDecayTicks;
    private int mobDecayStage;

    public int getValue() {
        return value;
    }

    public void add(int amount) {
        if (amount > 0) {
            value = Math.min(MAX_VALUE, value + amount);
            resetMobDecay();
        }
    }

    public void reduce(int amount) {
        reduce(amount, 0);
    }

    public void reduce(int amount, int minimum) {
        if (amount <= 0) {
            return;
        }
        value = Math.max(Math.max(0, minimum), value - amount);
        if (value == 0) {
            overworldTicks = 0;
            resetMobDecay();
        }
    }

    public void reset() {
        value = 0;
        overworldTicks = 0;
        effectProgress = 0.0D;
        identityShields = 0;
        identityShieldTicks = 0;
        resetMobDecay();
    }

    public boolean tickPollutionEffect(int intervalTicks) {
        if (intervalTicks <= 0) {
            return false;
        }
        effectProgress += 1.0D / intervalTicks;
        if (effectProgress < 1.0D) {
            return false;
        }
        effectProgress -= 1.0D;
        return true;
    }

    public void resetPollutionEffectProgress() {
        effectProgress = 0.0D;
    }

    public void tickNonPlayerDecay(boolean blockedByPollutionEffect) {
        if (blockedByPollutionEffect) {
            resetMobDecay();
            return;
        }
        if (value <= 0) {
            resetMobDecay();
            return;
        }

        mobDecayTicks++;
        if (mobDecayTicks < MOB_DECAY_INTERVAL_TICKS) {
            return;
        }

        mobDecayTicks = 0;
        mobDecayStage = Math.min(MAX_VALUE, mobDecayStage + 1);
        reduce(mobDecayStage);
    }

    public void resetMobDecay() {
        mobDecayTicks = 0;
        mobDecayStage = 0;
    }

    public void tickInOverworld(int minimum) {
        advanceOverworldTime(1L, minimum, OVERWORLD_REDUCTION_AMOUNT);
    }

    public void advanceSleepTime(long ticks, int minimum) {
        // Sleep skips world time; its faster purification must not change awake decay.
        advanceOverworldTime(ticks, minimum, SLEEP_REDUCTION_AMOUNT);
    }

    private void advanceOverworldTime(long ticks, int minimum, int reductionAmount) {
        if (ticks <= 0L) {
            return;
        }
        if (value <= 0) {
            overworldTicks = 0;
            return;
        }

        long accumulatedTicks = overworldTicks + ticks;
        int completedIntervals = (int) Math.min(
                accumulatedTicks / OVERWORLD_REDUCTION_INTERVAL_TICKS,
                Integer.MAX_VALUE
        );
        overworldTicks = (int) (accumulatedTicks % OVERWORLD_REDUCTION_INTERVAL_TICKS);
        if (completedIntervals > 0) {
            long reduction = (long) completedIntervals * reductionAmount;
            reduce((int) Math.min(reduction, Integer.MAX_VALUE), minimum);
        }
    }

    public void tickAdditiveIdentity(int level) {
        if (level <= 0) {
            identityShields = 0;
            identityShieldTicks = 0;
            return;
        }
        value = Math.max(1, value);
        int maximumShields = (level + 1) / 2;
        identityShields = Math.min(identityShields, maximumShields);
        if (identityShields >= maximumShields) {
            identityShieldTicks = 0;
            return;
        }
        identityShieldTicks++;
        int interval = (6 - level) * 20 * 60;
        if (identityShieldTicks >= interval) {
            identityShieldTicks = 0;
            identityShields++;
        }
    }

    public boolean consumeIdentityShield(int level) {
        if (level <= 0 || identityShields <= 0) {
            return false;
        }
        identityShields = Math.min(identityShields, (level + 1) / 2) - 1;
        identityShieldTicks = 0;
        return true;
    }

    public int getIdentityShields() {
        return identityShields;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("value", value);
        tag.putInt("overworld_ticks", overworldTicks);
        tag.putDouble("effect_progress", effectProgress);
        tag.putInt("identity_shields", identityShields);
        tag.putInt("identity_shield_ticks", identityShieldTicks);
        tag.putInt("mob_decay_ticks", mobDecayTicks);
        tag.putInt("mob_decay_stage", mobDecayStage);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        value = Math.max(0, Math.min(MAX_VALUE, tag.getInt("value")));
        overworldTicks = Math.max(0, Math.min(
                OVERWORLD_REDUCTION_INTERVAL_TICKS - 1,
                tag.getInt("overworld_ticks")
        ));
        effectProgress = Math.max(0.0D, Math.min(0.999999D, tag.getDouble("effect_progress")));
        identityShields = Math.max(0, Math.min(3, tag.getInt("identity_shields")));
        identityShieldTicks = Math.max(0, tag.getInt("identity_shield_ticks"));
        mobDecayTicks = Math.max(0, Math.min(
                MOB_DECAY_INTERVAL_TICKS - 1,
                tag.getInt("mob_decay_ticks")
        ));
        mobDecayStage = Math.max(0, Math.min(MAX_VALUE, tag.getInt("mob_decay_stage")));
    }
}
