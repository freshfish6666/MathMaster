package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

@EventBusSubscriber(modid = MathMaster.MODID)
public final class RecipeUnlockHandler {
    private static final List<ResourceLocation> BOOK_RECIPES = List.of(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "elementary_grade_2_math"),
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "junior_high_math"),
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "senior_high_math"),
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "advanced_math_workbook"),
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "millennium_problems_math")
    );

    private RecipeUnlockHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.awardRecipesByKey(BOOK_RECIPES);
        }
    }
}
