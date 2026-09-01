package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MathMaster.MODID);

    public static final Supplier<CreativeModeTab> MATHMASTER_TAB =
            CREATIVE_MODE_TABS.register("mathmaster",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.mathmaster"))
                            .icon(() -> new ItemStack(ModItems.ADVANCED_MATH_WORKBOOK.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.ADVANCED_MATH_WORKBOOK.get());
                                output.accept(ModItems.ELEMENTARY_GRADE_2_MATH.get());
                                output.accept(ModItems.JUNIOR_HIGH_MATH.get());
                                output.accept(ModItems.SENIOR_HIGH_MATH.get());
                                output.accept(ModItems.MILLENNIUM_PROBLEMS_MATH.get());
                                output.accept(ModItems.THREE_CAT_MILK_POWDER.get());
                            })
                            .build());
}
