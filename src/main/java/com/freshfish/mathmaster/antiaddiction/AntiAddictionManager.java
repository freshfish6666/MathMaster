package com.freshfish.mathmaster.antiaddiction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class AntiAddictionManager {
    private static final long WINDOW_MILLIS = 10 * 60 * 1000L;
    private static final Map<UUID, Deque<Long>> RECENT_ANSWERS = new ConcurrentHashMap<>();
    private static final List<Item> SNACKS = List.of(Items.APPLE, Items.BREAD, Items.COOKED_COD);

    private AntiAddictionManager() {
    }

    public static void onQuestionAnswered(ServerPlayer player, int difficulty) {
        long now = System.currentTimeMillis();
        Deque<Long> times = RECENT_ANSWERS.computeIfAbsent(player.getUUID(), uuid -> new ArrayDeque<>());

        synchronized (times) {
            times.removeIf(time -> now - time > WINDOW_MILLIS);
            times.addLast(now);

            if (times.size() > getThreshold(difficulty)) {
                times.clear();
                sendReminder(player);
                giveSnack(player);
            }
        }
    }

    private static int getThreshold(int difficulty) {
        if (difficulty <= 1) {
            return 24;
        } else if (difficulty <= 5) {
            return 20;
        } else if (difficulty <= 10) {
            return 16;
        } else {
            return 12;
        }
    }

    private static void sendReminder(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.mathmaster.anti_addiction"));
    }

    private static void giveSnack(ServerPlayer player) {
        ItemStack stack = new ItemStack(SNACKS.get(ThreadLocalRandom.current().nextInt(SNACKS.size())));
        if (!player.getInventory().add(stack)) {
            player.spawnAtLocation(stack);
        }
    }
}
