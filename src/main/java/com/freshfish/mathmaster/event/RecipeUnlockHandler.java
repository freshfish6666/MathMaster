package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MathMaster.MODID)
public final class RecipeUnlockHandler {
    private static final List<ResourceLocation> ALWAYS_UNLOCKED_RECIPES = List.of(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "elementary_grade_2_math"),
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "millennium_problems_math")
    );
    private static final List<IqRecipeRequirement> IQ_UNLOCKED_RECIPES = List.of(
            new IqRecipeRequirement(
                    ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "junior_high_math"),
                    40
            ),
            new IqRecipeRequirement(
                    ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "senior_high_math"),
                    45
            ),
            new IqRecipeRequirement(
                    ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "advanced_math_workbook"),
                    50
            )
    );

    private RecipeUnlockHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            synchronizeUnlockedRecipes(player, IntelligenceManager.get(player).getIq());
        }
    }

    public static void synchronizeUnlockedRecipes(ServerPlayer player, int iq) {
        List<ResourceLocation> recipesToAward = new ArrayList<>(ALWAYS_UNLOCKED_RECIPES);
        List<RecipeHolder<?>> recipesToReset = new ArrayList<>();

        for (IqRecipeRequirement requirement : IQ_UNLOCKED_RECIPES) {
            if (iq >= requirement.minimumIq()) {
                recipesToAward.add(requirement.recipeId());
            } else {
                player.serverLevel().getRecipeManager().byKey(requirement.recipeId())
                        .ifPresent(recipesToReset::add);
            }
        }

        player.awardRecipesByKey(recipesToAward);
        if (!recipesToReset.isEmpty()) {
            player.resetRecipes(recipesToReset);
        }
    }

    private record IqRecipeRequirement(ResourceLocation recipeId, int minimumIq) {
    }
}
