package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.item.MathBookItem;
import com.freshfish.mathmaster.item.LingxuMirrorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, MathMaster.MODID);

    public static final Supplier<Item> ADVANCED_MATH_WORKBOOK =
            ITEMS.register("advanced_math_workbook",
                    () -> new MathBookItem(
                            new Item.Properties(),
                            20,
                            0x2E7D32,
                            "description.mathmaster.advanced_math_workbook",
                            "verse.mathmaster.advanced_math_workbook"
                    ));

    public static final Supplier<Item> ELEMENTARY_GRADE_2_MATH =
            ITEMS.register("elementary_grade_2_math",
                    () -> new MathBookItem(
                            new Item.Properties(),
                            1,
                            0x64B5F6,
                            "description.mathmaster.elementary_grade_2_math",
                            "verse.mathmaster.elementary_grade_2_math",
                            "hint.mathmaster.elementary_grade_2_math"
                    ));

    public static final Supplier<Item> JUNIOR_HIGH_MATH =
            ITEMS.register("junior_high_math",
                    () -> new MathBookItem(
                            new Item.Properties(),
                            5,
                            0xFF9800,
                            "description.mathmaster.junior_high_math",
                            "verse.mathmaster.junior_high_math"
                    ));

    public static final Supplier<Item> SENIOR_HIGH_MATH =
            ITEMS.register("senior_high_math",
                    () -> new MathBookItem(
                            new Item.Properties(),
                            10,
                            0x673AB7,
                            "description.mathmaster.senior_high_math",
                            "verse.mathmaster.senior_high_math"
                    ));

    public static final Supplier<Item> MILLENNIUM_PROBLEMS_MATH =
            ITEMS.register("millennium_problems_math",
                    () -> new MathBookItem(
                            new Item.Properties(),
                            30,
                            0xC62828,
                            "description.mathmaster.millennium_problems_math",
                            "verse.mathmaster.millennium_problems_math"
                    ));

    public static final Supplier<Item> THREE_CAT_MILK_POWDER =
            ITEMS.register("three_cat_milk_powder",
                    () -> new Item(new Item.Properties().food(
                            new FoodProperties.Builder()
                                    .nutrition(3)
                                    .saturationModifier(0.6F)
                                    .alwaysEdible()
                                    .build()
                    )));

    public static final Supplier<Item> LINGXU_INGOT =
            ITEMS.register("lingxu_ingot",
                    () -> new Item(new Item.Properties()));

    public static final Supplier<Item> LINGXU_NUGGET =
            ITEMS.register("lingxu_nugget",
                    () -> new Item(new Item.Properties()));

    public static final Supplier<Item> LINGXU_BLOCK =
            ITEMS.register("lingxu_block",
                    () -> new BlockItem(ModBlocks.LINGXU_BLOCK.get(), new Item.Properties().fireResistant()));

    public static final Supplier<Item> LINGXU_SWORD =
            ITEMS.register("lingxu_sword",
                    () -> new SwordItem(
                            ModToolTiers.LINGXU,
                            new Item.Properties()
                                    .fireResistant()
                                    .attributes(SwordItem.createAttributes(ModToolTiers.LINGXU, 7, -2.1F))
                    ));

    public static final Supplier<Item> LINGXU_PICKAXE =
            ITEMS.register("lingxu_pickaxe",
                    () -> new PickaxeItem(
                            ModToolTiers.LINGXU,
                            new Item.Properties()
                                    .fireResistant()
                                    .attributes(PickaxeItem.createAttributes(ModToolTiers.LINGXU, 4.0F, -2.5F))
                    ));

    public static final Supplier<Item> LINGXU_AXE =
            ITEMS.register("lingxu_axe",
                    () -> new AxeItem(
                            ModToolTiers.LINGXU,
                            new Item.Properties()
                                    .fireResistant()
                                    .attributes(AxeItem.createAttributes(ModToolTiers.LINGXU, 10.0F, -2.7F))
                    ));

    public static final Supplier<Item> LINGXU_SHOVEL =
            ITEMS.register("lingxu_shovel",
                    () -> new ShovelItem(
                            ModToolTiers.LINGXU,
                            new Item.Properties()
                                    .fireResistant()
                                    .attributes(ShovelItem.createAttributes(ModToolTiers.LINGXU, 4.75F, -2.7F))
                    ));

    public static final Supplier<Item> LINGXU_HOE =
            ITEMS.register("lingxu_hoe",
                    () -> new HoeItem(
                            ModToolTiers.LINGXU,
                            new Item.Properties()
                                    .fireResistant()
                                    .attributes(HoeItem.createAttributes(ModToolTiers.LINGXU, -3.5F, 0.3F))
                    ));

    public static final Supplier<Item> LINGXU_MIRROR =
            ITEMS.register("lingxu_mirror",
                    () -> new LingxuMirrorItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> GOTHAM_BAT_ICON =
            ITEMS.register("gotham_bat_icon", () -> new Item(new Item.Properties()));
}
