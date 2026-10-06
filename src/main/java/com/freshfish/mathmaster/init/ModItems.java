package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.item.MathBookItem;
import com.freshfish.mathmaster.item.LingxuMirrorItem;
import com.freshfish.mathmaster.item.GraduationCapItem;
import com.freshfish.mathmaster.item.StudyNoteItem;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.item.PrimeCoreItem;
import com.freshfish.mathmaster.item.EmptySetItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import java.util.function.Supplier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, MathMaster.MODID);

    public static final Supplier<Item> DIGITALLY_POLLUTED_WATER_BUCKET =
            ITEMS.register("digitally_polluted_water_bucket", () -> new BucketItem(
                    ModFluids.DIGITALLY_POLLUTED_WATER.get(), new Item.Properties()
                    .craftRemainder(Items.BUCKET).stacksTo(1)));

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

    public static final Supplier<Item> ADVANCED_MATH_STUDY_NOTE =
            ITEMS.register("advanced_math_study_note",
                    () -> new StudyNoteItem(new Item.Properties()));

    public static final Supplier<Item> GRADE_2_MATH_STUDY_NOTE =
            ITEMS.register("grade_2_math_study_note",
                    () -> new StudyNoteItem(new Item.Properties()));

    public static final Supplier<Item> JUNIOR_HIGH_MATH_STUDY_NOTE =
            ITEMS.register("junior_high_math_study_note",
                    () -> new StudyNoteItem(new Item.Properties()));

    public static final Supplier<Item> SENIOR_HIGH_MATH_STUDY_NOTE =
            ITEMS.register("senior_high_math_study_note",
                    () -> new StudyNoteItem(new Item.Properties()));

    public static final Supplier<Item> MILLENNIUM_PROBLEMS_STUDY_NOTE =
            ITEMS.register("millennium_problems_study_note",
                    () -> new StudyNoteItem(new Item.Properties()));

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

    public static final Supplier<Item> PRIME_INGOT =
            ITEMS.register("prime_ingot",
                    () -> new Item(new Item.Properties()));

    public static final Supplier<Item> PRIME_NUGGET =
            ITEMS.register("prime_nugget",
                    () -> new Item(new Item.Properties()));

    public static final Supplier<Item> PRIME_BLOCK =
            ITEMS.register("prime_block",
                    () -> new BlockItem(ModBlocks.PRIME_BLOCK.get(), new Item.Properties().fireResistant()));

    public static final Supplier<Item> PRIME_CORE =
            ITEMS.register("prime_core",
                    () -> new PrimeCoreItem(new Item.Properties()));

    public static final Supplier<Item> SET =
            ITEMS.register("set",
                    () -> new Item(new Item.Properties()));

    public static final Supplier<Item> EMPTY_SET =
            ITEMS.register("empty_set",
                    () -> new EmptySetItem(new Item.Properties()));

    public static final Supplier<Item> LINGXU_BLOCK =
            ITEMS.register("lingxu_block",
                    () -> new BlockItem(ModBlocks.LINGXU_BLOCK.get(), new Item.Properties().fireResistant()));

    public static final Supplier<Item> AXIOM_DEDUCTION_TABLE =
            ITEMS.register("axiom_deduction_table",
                    () -> new BlockItem(ModBlocks.AXIOM_DEDUCTION_TABLE.get(), new Item.Properties()));

    public static final Supplier<Item> DIGITALLY_CORRUPTED_BLOCK =
            ITEMS.register("digitally_corrupted_block",
                    () -> new BlockItem(ModBlocks.DIGITALLY_CORRUPTED_BLOCK.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_PLANKS =
            ITEMS.register("collatz_planks", () -> new BlockItem(ModBlocks.COLLATZ_PLANKS.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_SLAB =
            ITEMS.register("collatz_slab", () -> new BlockItem(ModBlocks.COLLATZ_SLAB.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_STAIRS =
            ITEMS.register("collatz_stairs", () -> new BlockItem(ModBlocks.COLLATZ_STAIRS.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_FENCE =
            ITEMS.register("collatz_fence", () -> new BlockItem(ModBlocks.COLLATZ_FENCE.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_FENCE_GATE =
            ITEMS.register("collatz_fence_gate", () -> new BlockItem(ModBlocks.COLLATZ_FENCE_GATE.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_DOOR =
            ITEMS.register("collatz_door", () -> new BlockItem(ModBlocks.COLLATZ_DOOR.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_TRAPDOOR =
            ITEMS.register("collatz_trapdoor", () -> new BlockItem(ModBlocks.COLLATZ_TRAPDOOR.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_BUTTON =
            ITEMS.register("collatz_button", () -> new BlockItem(ModBlocks.COLLATZ_BUTTON.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_PRESSURE_PLATE =
            ITEMS.register("collatz_pressure_plate", () -> new BlockItem(ModBlocks.COLLATZ_PRESSURE_PLATE.get(), new Item.Properties()));

    public static final Supplier<Item> N_ALTAR =
            ITEMS.register("n_altar", () -> new BlockItem(ModBlocks.N_ALTAR.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_FRUIT =
            ITEMS.register("collatz_fruit", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> COLLATZ_SAPLING =
            ITEMS.register("collatz_sapling", () -> new BlockItem(ModBlocks.COLLATZ_SAPLING.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_ROOT =
            ITEMS.register("collatz_root", () -> new BlockItem(ModBlocks.COLLATZ_ROOT.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_LOG =
            ITEMS.register("collatz_log", () -> new BlockItem(ModBlocks.COLLATZ_LOG.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_NODE =
            ITEMS.register("collatz_node", () -> new BlockItem(ModBlocks.COLLATZ_NODE.get(), new Item.Properties()));

    public static final Supplier<Item> COLLATZ_LEAVES =
            ITEMS.register("collatz_leaves", () -> new BlockItem(ModBlocks.COLLATZ_LEAVES.get(), new Item.Properties()));

    public static final Supplier<Item> FRUITING_COLLATZ_LEAVES =
            ITEMS.register("fruiting_collatz_leaves", () -> new BlockItem(ModBlocks.FRUITING_COLLATZ_LEAVES.get(), new Item.Properties()));

    public static final Supplier<Item> AXIOM_CASE =
            ITEMS.register("axiom_case", () -> new AxiomCaseItem(3, new Item.Properties()));

    public static final Supplier<Item> ORACLE_CASE =
            ITEMS.register("oracle_case", () -> new AxiomCaseItem(6, new Item.Properties()));

    public static final Supplier<Item> TRUTH_CASE =
            ITEMS.register("truth_case", () -> new AxiomCaseItem(9, new Item.Properties()));

    public static final Supplier<Item> LINGXU_SWORD =
            ITEMS.register("lingxu_sword",
                    () -> new SwordItem(
                            ModToolTiers.LINGXU,
                            new Item.Properties()
                                    .fireResistant()
                                    .attributes(SwordItem.createAttributes(ModToolTiers.LINGXU, 5, -2.1F))
                    ));

    public static final Supplier<Item> LINGXU_PICKAXE =
            ITEMS.register("lingxu_pickaxe",
                    () -> new PickaxeItem(
                            ModToolTiers.LINGXU,
                            new Item.Properties()
                                    .fireResistant()
                                    .attributes(PickaxeItem.createAttributes(ModToolTiers.LINGXU, 3.0F, -2.5F))
                    ));

    public static final Supplier<Item> LINGXU_AXE =
            ITEMS.register("lingxu_axe",
                    () -> new AxeItem(
                            ModToolTiers.LINGXU,
                            new Item.Properties()
                                    .fireResistant()
                                    .attributes(AxeItem.createAttributes(ModToolTiers.LINGXU, 7.0F, -2.7F))
                    ));

    public static final Supplier<Item> LINGXU_SHOVEL =
            ITEMS.register("lingxu_shovel",
                    () -> new ShovelItem(
                            ModToolTiers.LINGXU,
                            new Item.Properties()
                                    .fireResistant()
                                    .attributes(ShovelItem.createAttributes(ModToolTiers.LINGXU, 3.5F, -2.7F))
                    ));

    public static final Supplier<Item> LINGXU_HOE =
            ITEMS.register("lingxu_hoe",
                    () -> new HoeItem(
                            ModToolTiers.LINGXU,
                            new Item.Properties()
                                    .fireResistant()
                                    .attributes(HoeItem.createAttributes(ModToolTiers.LINGXU, -2.0F, 0.3F))
                    ));

    public static final Supplier<Item> LINGXU_MIRROR =
            ITEMS.register("lingxu_mirror",
                    () -> new LingxuMirrorItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> DIGITAL_POLLUTION_METER =
            ITEMS.register("digital_pollution_meter",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> MATHEMATICAL_RING =
            ITEMS.register("mathematical_ring",
                    () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final Supplier<Item> LINGXU_HELMET =
            ITEMS.register("lingxu_helmet", () -> createLingxuArmor(ArmorItem.Type.HELMET));

    public static final Supplier<Item> LINGXU_CHESTPLATE =
            ITEMS.register("lingxu_chestplate", () -> createLingxuArmor(ArmorItem.Type.CHESTPLATE));

    public static final Supplier<Item> LINGXU_LEGGINGS =
            ITEMS.register("lingxu_leggings", () -> createLingxuArmor(ArmorItem.Type.LEGGINGS));

    public static final Supplier<Item> LINGXU_BOOTS =
            ITEMS.register("lingxu_boots", () -> createLingxuArmor(ArmorItem.Type.BOOTS));

    public static final Supplier<Item> GRADUATION_CAP =
            ITEMS.register("graduation_cap",
                    () -> new GraduationCapItem(
                            ModArmorMaterials.GRADUATION_CAP,
                            new Item.Properties().durability(365)
                    ));

    public static final Supplier<Item> GOTHAM_BAT_ICON =
            ITEMS.register("gotham_bat_icon", () -> new Item(new Item.Properties()));

    // Display-only advancement icons, following the existing bat icon pattern.
    public static final Supplier<Item> POLLUTION_EFFECT_ICON =
            ITEMS.register("pollution_effect_icon", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> POLLUTION_HIGH_TOWER_FALLS_ICON =
            ITEMS.register("pollution_high_tower_falls_icon", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> POLLUTION_LAST_BREATH_ICON =
            ITEMS.register("pollution_last_breath_icon", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> POLLUTION_IRON_TO_DUST_ICON =
            ITEMS.register("pollution_iron_to_dust_icon", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> NINE_SPAWN_EGG = ITEMS.register("nine_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.NINE, 0x363B40, 0x835C35, new Item.Properties()));
    public static final Supplier<Item> FIVE_SPAWN_EGG = ITEMS.register("five_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FIVE, 0x48151B, 0x56706A, new Item.Properties()));

    public static final Supplier<Item> EIGHT_SPAWN_EGG = ITEMS.register("eight_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.EIGHT, 0x34363A, 0xBEC2C5, new Item.Properties()));
    public static final Supplier<Item> SIX_SPAWN_EGG = ITEMS.register("six_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SIX, 0x5E1118, 0xF20A12, new Item.Properties()));
    public static final Supplier<Item> GEOMETRY_HOLDER_SPAWN_EGG = ITEMS.register("geometry_holder_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GEOMETRY_HOLDER, 0x527E87, 0x26DAED, new Item.Properties()));
    public static final Supplier<Item> SEVEN_SPAWN_EGG = ITEMS.register("seven_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SEVEN, 0xC29A40, 0x42D6CB, new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_PLANKS =
            ITEMS.register("purple_collatz_planks", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_PLANKS.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_SLAB =
            ITEMS.register("purple_collatz_slab", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_SLAB.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_STAIRS =
            ITEMS.register("purple_collatz_stairs", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_STAIRS.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_FENCE =
            ITEMS.register("purple_collatz_fence", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_FENCE.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_FENCE_GATE =
            ITEMS.register("purple_collatz_fence_gate", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_FENCE_GATE.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_DOOR =
            ITEMS.register("purple_collatz_door", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_DOOR.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_TRAPDOOR =
            ITEMS.register("purple_collatz_trapdoor", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_TRAPDOOR.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_BUTTON =
            ITEMS.register("purple_collatz_button", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_BUTTON.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_PRESSURE_PLATE =
            ITEMS.register("purple_collatz_pressure_plate", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_PRESSURE_PLATE.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_SAPLING =
            ITEMS.register("purple_collatz_sapling", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_SAPLING.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_ROOT =
            ITEMS.register("purple_collatz_root", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_ROOT.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_LOG =
            ITEMS.register("purple_collatz_log", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_LOG.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_NODE =
            ITEMS.register("purple_collatz_node", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_NODE.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_COLLATZ_LEAVES =
            ITEMS.register("purple_collatz_leaves", () -> new BlockItem(ModBlocks.PURPLE_COLLATZ_LEAVES.get(), new Item.Properties()));

    public static final Supplier<Item> PURPLE_FRUITING_COLLATZ_LEAVES =
            ITEMS.register("purple_fruiting_collatz_leaves", () -> new BlockItem(ModBlocks.PURPLE_FRUITING_COLLATZ_LEAVES.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_PLANKS =
            ITEMS.register("blue_collatz_planks", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_PLANKS.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_SLAB =
            ITEMS.register("blue_collatz_slab", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_SLAB.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_STAIRS =
            ITEMS.register("blue_collatz_stairs", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_STAIRS.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_FENCE =
            ITEMS.register("blue_collatz_fence", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_FENCE.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_FENCE_GATE =
            ITEMS.register("blue_collatz_fence_gate", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_FENCE_GATE.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_DOOR =
            ITEMS.register("blue_collatz_door", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_DOOR.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_TRAPDOOR =
            ITEMS.register("blue_collatz_trapdoor", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_TRAPDOOR.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_BUTTON =
            ITEMS.register("blue_collatz_button", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_BUTTON.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_PRESSURE_PLATE =
            ITEMS.register("blue_collatz_pressure_plate", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_PRESSURE_PLATE.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_SAPLING =
            ITEMS.register("blue_collatz_sapling", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_SAPLING.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_ROOT =
            ITEMS.register("blue_collatz_root", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_ROOT.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_LOG =
            ITEMS.register("blue_collatz_log", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_LOG.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_NODE =
            ITEMS.register("blue_collatz_node", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_NODE.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_COLLATZ_LEAVES =
            ITEMS.register("blue_collatz_leaves", () -> new BlockItem(ModBlocks.BLUE_COLLATZ_LEAVES.get(), new Item.Properties()));

    public static final Supplier<Item> BLUE_FRUITING_COLLATZ_LEAVES =
            ITEMS.register("blue_fruiting_collatz_leaves", () -> new BlockItem(ModBlocks.BLUE_FRUITING_COLLATZ_LEAVES.get(), new Item.Properties()));

    public static final Supplier<Item> GEOMETRY_CORE = ITEMS.register("geometry_core", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> GEOMETRY_CONSTRUCT_SPAWN_EGG = ITEMS.register("geometry_construct_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GEOMETRY_CONSTRUCT, 0x465E6B, 0x49DDEB, new Item.Properties()));
    public static final Supplier<Item> GEOMETRY_ALTAR = ITEMS.register("geometry_altar",
            () -> new BlockItem(ModBlocks.GEOMETRY_ALTAR.get(), new Item.Properties()));

    private static ArmorItem createLingxuArmor(ArmorItem.Type type) {
        ArmorMaterial netherite = ArmorMaterials.NETHERITE.value();
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
        ResourceLocation modifierId = ResourceLocation.withDefaultNamespace("armor." + type.getName());
        // Explicit attributes preserve fractional defense and replace the integer material defaults.
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(
                        modifierId, netherite.getDefense(type) * 1.5D, AttributeModifier.Operation.ADD_VALUE
                ), slot)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                        modifierId, netherite.toughness() * 1.5D, AttributeModifier.Operation.ADD_VALUE
                ), slot)
                .build();
        return new ArmorItem(ModArmorMaterials.LINGXU, type, new Item.Properties()
                .durability(Math.round(type.getDurability(37) * 1.5F))
                .fireResistant()
                .attributes(attributes));
    }
}
