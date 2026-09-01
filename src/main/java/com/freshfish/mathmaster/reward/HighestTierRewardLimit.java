package com.freshfish.mathmaster.reward;

import com.freshfish.mathmaster.init.ModAttachments;
import net.minecraft.server.level.ServerPlayer;

public final class HighestTierRewardLimit {
    public static final int DAILY_LIMIT = 10;
    private static final long TICKS_PER_GAME_DAY = 24000L;

    private HighestTierRewardLimit() {
    }

    public static boolean tryClaim(ServerPlayer player) {
        long dayTime = player.serverLevel().getServer().overworld().getDayTime();
        long gameDay = Math.floorDiv(dayTime, TICKS_PER_GAME_DAY);
        return player.getData(ModAttachments.HIGHEST_TIER_REWARD).tryClaim(gameDay, DAILY_LIMIT);
    }
}
