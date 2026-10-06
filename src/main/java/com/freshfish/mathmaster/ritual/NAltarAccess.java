package com.freshfish.mathmaster.ritual;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Access belongs to the player, never to a particular altar or blood-sacrifice round. */
public final class NAltarAccess {
    public static final int REQUIRED_SPECIES = 5;
    public static final int IQ_THRESHOLD = 50;

    public static boolean tryUnlock(ServerPlayer player) {
        var results = player.getData(ModAttachments.INSIGHT_RESULTS);
        if (results.hasUnlockedNAltar()) return true;
        if (!player.isAlive() || player.isSpectator()
                || results.getDistinctEntityTypeCount() < REQUIRED_SPECIES
                || IntelligenceManager.getEffectiveIq(player) <= IQ_THRESHOLD) return false;
        results.unlockNAltar();
        player.displayClientMessage(Component.translatable("message.mathmaster.n_altar.unlocked"), false);
        return true;
    }

    /** Call before charging offerings, sampling randomness or starting category cooldowns. */
    public static boolean allowOffering(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        if (tryUnlock(serverPlayer)) return true;
        player.displayClientMessage(Component.translatable("message.mathmaster.n_altar.locked",
                IntelligenceManager.getEffectiveIq(player),
                player.getData(ModAttachments.INSIGHT_RESULTS).getDistinctEntityTypeCount(),
                IQ_THRESHOLD, REQUIRED_SPECIES), false);
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerTick(PlayerTickEvent.Post event) {
        // Check after equipment/Flow updates, including old saves and temporary IQ bonuses.
        if (event.getEntity() instanceof ServerPlayer player) tryUnlock(player);
    }
}
