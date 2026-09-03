package com.freshfish.mathmaster.reward;

import com.freshfish.mathmaster.init.ModItems;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class RewardManager {
    private static final int HIGHEST_TIER_MINIMUM_SCORE = 400;
    private static final List<Item> BAND_1 = List.of(
            Items.APPLE,
            Items.BREAD,
            Items.WHEAT,
            Items.POTATO,
            Items.CARROT,
            Items.SWEET_BERRIES
    );

    private static final List<Item> BAND_2 = List.of(
            Items.REDSTONE,
            Items.IRON_INGOT,
            Items.COAL,
            Items.GOLD_NUGGET,
            Items.FLINT,
            Items.COPPER_INGOT
    );

    private static final List<Item> BAND_3 = List.of(
            Items.LAPIS_LAZULI,
            Items.DIAMOND,
            Items.IRON_INGOT,
            Items.EMERALD,
            Items.GOLD_INGOT,
            Items.QUARTZ,
            Items.AMETHYST_SHARD
    );

    private static final List<ItemStack> BAND_4_OTHERS = List.of(
            new ItemStack(Items.NETHERITE_SCRAP),
            new ItemStack(Items.GOLDEN_APPLE),
            new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),
            new ItemStack(Items.DIAMOND_BLOCK),
            new ItemStack(ModItems.LINGXU_INGOT.get())
    );

    private RewardManager() {
    }

    public static List<ItemStack> getRewards(int difficulty, int iq) {
        int score = difficulty * getIqMultiplier(iq);
        List<ItemStack> rewards = new ArrayList<>();

        if (score > 0 && score < 300) {
            rewards.add(randomStack(BAND_1));
        }
        if (score > 150 && score < 400) {
            rewards.add(randomStack(BAND_2));
        }
        if (score > 250) {
            rewards.add(randomStack(BAND_3));
        }
        if (score >= HIGHEST_TIER_MINIMUM_SCORE) {
            rewards.add(randomBand4Stack());
        }

        return rewards;
    }

    public static boolean qualifiesForHighestTier(int difficulty, int iq) {
        return difficulty * getIqMultiplier(iq) >= HIGHEST_TIER_MINIMUM_SCORE;
    }

    private static int getIqMultiplier(int iq) {
        if (iq <= 35) {
            return 10;
        } else if (iq <= 44) {
            return 16;
        } else if (iq <= 59) {
            return 20;
        } else if (iq <= 79) {
            return 25;
        } else if (iq <= 99) {
            return 30;
        } else if (iq <= 119) {
            return 35;
        } else if (iq <= 129) {
            return 40;
        } else if (iq <= 159) {
            return 50;
        } else {
            return 60;
        }
    }

    private static ItemStack randomStack(List<Item> items) {
        return new ItemStack(items.get(ThreadLocalRandom.current().nextInt(items.size())));
    }

    private static ItemStack randomStackFromStacks(List<ItemStack> items) {
        return items.get(ThreadLocalRandom.current().nextInt(items.size())).copy();
    }

    private static ItemStack randomBand4Stack() {
        if (ThreadLocalRandom.current().nextDouble() < 0.01) {
            return createPlayerHead();
        }
        return randomStackFromStacks(BAND_4_OTHERS);
    }

    private static ItemStack createPlayerHead() {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(
                DataComponents.PROFILE,
                new ResolvableProfile(Optional.of("freshfish6666"), Optional.empty(), new PropertyMap())
        );
        return head;
    }
}
