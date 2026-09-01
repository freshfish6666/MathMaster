package com.freshfish.mathmaster.intelligence;

import com.freshfish.mathmaster.init.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public final class IntelligenceManager {
    private IntelligenceManager() {
    }

    public static IntelligenceData get(Player player) {
        return player.getData(ModAttachments.INTELLIGENCE);
    }

    public static void addExperience(Player player, int amount) {
        int oldIq = get(player).getIq();
        get(player).addExperience(amount);
        int newIq = get(player).getIq();
        notifyIfLeveledUp(player, oldIq, newIq);
    }

    public static void addIq(Player player, int amount) {
        int oldIq = get(player).getIq();
        get(player).addIq(amount);
        int newIq = get(player).getIq();
        notifyIfLeveledUp(player, oldIq, newIq);
    }

    public static void setIq(Player player, int iq) {
        int oldIq = get(player).getIq();
        get(player).setIq(iq);
        int newIq = get(player).getIq();
        notifyIfLeveledUp(player, oldIq, newIq);
    }

    public static boolean setExperience(Player player, int experience) {
        return get(player).setExperience(experience);
    }

    private static void notifyIfLeveledUp(Player player, int oldIq, int newIq) {
        if (newIq > oldIq && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.mathmaster.level_up", newIq)
            );
            serverPlayer.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
