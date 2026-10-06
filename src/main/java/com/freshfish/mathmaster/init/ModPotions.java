package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModPotions {
    private static final int DIGITAL_POLLUTION_DURATION_TICKS = 60 * 20;

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, MathMaster.MODID);

    public static final DeferredHolder<Potion, Potion> DIGITAL_POLLUTION =
            POTIONS.register("digital_pollution", () -> new Potion(
                    "digital_pollution",
                    new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION, DIGITAL_POLLUTION_DURATION_TICKS)
            ));

    private ModPotions() {
    }

    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(Potions.AWKWARD, ModItems.LINGXU_NUGGET.get(), DIGITAL_POLLUTION);
    }
}
