package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.Set;

public class FoodIntelligenceHandler {
    private static final Set<Item> RAW_MEATS = Set.of(
            Items.BEEF,
            Items.CHICKEN,
            Items.COD,
            Items.MUTTON,
            Items.PORKCHOP,
            Items.RABBIT,
            Items.SALMON
    );

    private static final Set<Item> COOKED_MEATS = Set.of(
            Items.COOKED_BEEF,
            Items.COOKED_CHICKEN,
            Items.COOKED_COD,
            Items.COOKED_MUTTON,
            Items.COOKED_PORKCHOP,
            Items.COOKED_RABBIT,
            Items.COOKED_SALMON
    );

    @SubscribeEvent
    public void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Item item = event.getItem().getItem();
        int delta = getIntelligenceDelta(item);
        if (delta != 0) {
            IntelligenceManager.addExperience(player, delta);
        }
    }

    private int getIntelligenceDelta(Item item) {
        if (item == ModItems.THREE_CAT_MILK_POWDER.get()) {
            return -10;
        }
        if (item == Items.ROTTEN_FLESH || RAW_MEATS.contains(item)) {
            return -1;
        }
        if (item == Items.MILK_BUCKET) {
            return 1;
        }
        if (COOKED_MEATS.contains(item)) {
            return 1;
        }
        if (item == Items.GOLDEN_APPLE) {
            return 5;
        }
        if (item == Items.ENCHANTED_GOLDEN_APPLE) {
            return 20;
        }
        return 0;
    }
}
