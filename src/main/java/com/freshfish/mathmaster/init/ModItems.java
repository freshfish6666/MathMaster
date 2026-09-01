package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.item.MathBookItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, MathMaster.MODID);

    public static final Supplier<Item> ADVANCED_MATH_WORKBOOK =
            ITEMS.register("advanced_math_workbook",
                    () -> new MathBookItem(new Item.Properties(), 20, 0x2E7D32, "description.mathmaster.advanced_math_workbook"));

    public static final Supplier<Item> ELEMENTARY_GRADE_2_MATH =
            ITEMS.register("elementary_grade_2_math",
                    () -> new MathBookItem(
                            new Item.Properties(),
                            1,
                            0x64B5F6,
                            "description.mathmaster.elementary_grade_2_math",
                            "hint.mathmaster.elementary_grade_2_math"
                    ));

    public static final Supplier<Item> JUNIOR_HIGH_MATH =
            ITEMS.register("junior_high_math",
                    () -> new MathBookItem(new Item.Properties(), 5, 0xFF9800, "description.mathmaster.junior_high_math"));

    public static final Supplier<Item> SENIOR_HIGH_MATH =
            ITEMS.register("senior_high_math",
                    () -> new MathBookItem(new Item.Properties(), 10, 0x673AB7, "description.mathmaster.senior_high_math"));

    public static final Supplier<Item> MILLENNIUM_PROBLEMS_MATH =
            ITEMS.register("millennium_problems_math",
                    () -> new MathBookItem(new Item.Properties(), 30, 0xC62828, "description.mathmaster.millennium_problems_math"));

    public static final Supplier<Item> THREE_CAT_MILK_POWDER =
            ITEMS.register("three_cat_milk_powder",
                    () -> new Item(new Item.Properties().food(
                            new FoodProperties.Builder()
                                    .nutrition(3)
                                    .saturationModifier(0.6F)
                                    .alwaysEdible()
                                    .build()
                    )));
}
