package com.freshfish.mathmaster.intelligence;

import com.freshfish.mathmaster.event.RecipeUnlockHandler;
import com.freshfish.mathmaster.event.LingxuMirrorStatusHandler;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public final class IntelligenceManager {
    private IntelligenceManager() {
    }

    public static IntelligenceData get(Player player) {
        return player.getData(ModAttachments.INTELLIGENCE);
    }

    public static int getEffectiveIq(Player player) {
        return Math.min(
                get(player).getIq()
                        + getFlowIntellectBonus(player)
                        + getGraduationCapIntellectBonus(player),
                IntelligenceData.MAX_IQ
        );
    }

    public static int getGraduationCapIntellectBonus(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GRADUATION_CAP.get()) ? 10 : 0;
    }

    public static int getFlowIntellectBonus(LivingEntity entity) {
        var flow = entity.getEffect(ModMobEffects.FLOW);
        if (flow == null) {
            return 0;
        }

        long effectLevel = Math.max(0L, (long) flow.getAmplifier() + 1L);
        return (int) Math.min((long) IntelligenceData.MAX_IQ, effectLevel * 5L);
    }

    public static void addExperience(Player player, int amount) {
        int oldIq = get(player).getIq();
        get(player).addExperience(amount);
        int newIq = get(player).getIq();
        sync(player);
        notifyIfLeveledUp(player, oldIq, newIq);
        LingxuMirrorStatusHandler.refreshIfHeld(player);
    }

    public static int addExperienceCappedAtIq(Player player, int amount, int maximumIq) {
        int oldIq = get(player).getIq();
        int addedExperience = get(player).addExperienceCappedAtIq(amount, maximumIq);
        if (addedExperience <= 0) {
            return 0;
        }

        int newIq = get(player).getIq();
        sync(player);
        notifyIfLeveledUp(player, oldIq, newIq);
        LingxuMirrorStatusHandler.refreshIfHeld(player);
        return addedExperience;
    }

    public static void addIq(Player player, int amount) {
        int oldIq = get(player).getIq();
        get(player).addIq(amount);
        int newIq = get(player).getIq();
        sync(player);
        notifyIfLeveledUp(player, oldIq, newIq);
        LingxuMirrorStatusHandler.refreshIfHeld(player);
    }

    public static void setIq(Player player, int iq) {
        int oldIq = get(player).getIq();
        get(player).setIq(iq);
        int newIq = get(player).getIq();
        sync(player);
        notifyIfLeveledUp(player, oldIq, newIq);
        LingxuMirrorStatusHandler.refreshIfHeld(player);
    }

    public static boolean setExperience(Player player, int experience) {
        boolean changed = get(player).setExperience(experience);
        if (changed) {
            sync(player);
            LingxuMirrorStatusHandler.refreshIfHeld(player);
        }
        return changed;
    }

    private static void notifyIfLeveledUp(Player player, int oldIq, int newIq) {
        if (oldIq == newIq || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        RecipeUnlockHandler.synchronizeUnlockedRecipes(serverPlayer, newIq);
        if (newIq > oldIq) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.mathmaster.level_up", newIq)
            );
            serverPlayer.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.syncData(ModAttachments.INTELLIGENCE);
        }
    }
}
