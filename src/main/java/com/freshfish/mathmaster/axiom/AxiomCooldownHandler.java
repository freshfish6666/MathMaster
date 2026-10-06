package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.item.EquippedMathematicalRing;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Shared cooldown policy. Registered once, before the individual skill tick handlers. */
public final class AxiomCooldownHandler {
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AxiomSkillData data = player.getData(ModAttachments.AXIOM_SKILL_DATA);
        // An idle player needs no Curios lookup; equipment is read live when cooldowns exist.
        if (!data.hasCooldowns()) {
            return;
        }
        if (bypassesCooldown(player)) {
            data.clearCooldowns();
        } else {
            data.tickCooldowns();
        }
    }

    public static boolean bypassesCooldown(ServerPlayer player) {
        return EquippedMathematicalRing.isEquipped(player);
    }

    /** Capture the ring once per activation, preserving the before/after-use clearing policy. */
    public static boolean beforeSkillUse(ServerPlayer player) {
        boolean bypass = bypassesCooldown(player);
        afterSkillUse(player, bypass);
        return bypass;
    }

    public static void afterSkillUse(ServerPlayer player, boolean bypass) {
        if (bypass) {
            player.getData(ModAttachments.AXIOM_SKILL_DATA).clearCooldowns();
        }
    }

    /** Geodesic checks and assigns cooldowns between ticks, so it also needs the live ring policy. */
    public static int applyRingBypass(ServerPlayer player, int ticks) {
        return ticks > 0 && bypassesCooldown(player) ? 0 : ticks;
    }
}
