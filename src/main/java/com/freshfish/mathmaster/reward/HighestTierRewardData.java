package com.freshfish.mathmaster.reward;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class HighestTierRewardData implements INBTSerializable<CompoundTag> {
    private long gameDay = Long.MIN_VALUE;
    private int claims;

    public boolean tryClaim(long currentGameDay, int dailyLimit) {
        if (this.gameDay != currentGameDay) {
            this.gameDay = currentGameDay;
            this.claims = 0;
        }

        if (this.claims >= dailyLimit) {
            return false;
        }

        this.claims++;
        return true;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("game_day", this.gameDay);
        tag.putInt("claims", this.claims);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.gameDay = tag.contains("game_day") ? tag.getLong("game_day") : Long.MIN_VALUE;
        this.claims = Math.max(0, tag.getInt("claims"));
    }
}
