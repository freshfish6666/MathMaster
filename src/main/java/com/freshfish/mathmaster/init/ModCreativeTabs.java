package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.item.StudyNoteItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
                            .withTabsBefore(ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "mathmaster_notes"))
                            .icon(() -> new ItemStack(ModItems.ADVANCED_MATH_WORKBOOK.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.ADVANCED_MATH_WORKBOOK.get());
                                output.accept(ModItems.ELEMENTARY_GRADE_2_MATH.get());
                                output.accept(ModItems.JUNIOR_HIGH_MATH.get());
                                output.accept(ModItems.SENIOR_HIGH_MATH.get());
                                output.accept(ModItems.MILLENNIUM_PROBLEMS_MATH.get());
                                output.accept(ModItems.LINGXU_BLOCK.get());
                                output.accept(ModItems.LINGXU_INGOT.get());
                                output.accept(ModItems.LINGXU_NUGGET.get());
                                output.accept(ModItems.PRIME_INGOT.get());
                                output.accept(ModItems.PRIME_NUGGET.get());
                                output.accept(ModItems.PRIME_BLOCK.get());
                                output.accept(ModItems.PRIME_CORE.get());
                                output.accept(ModItems.GEOMETRY_CORE.get());
                                output.accept(ModItems.COLLATZ_FRUIT.get());
                                output.accept(ModItems.SET.get());
                                output.accept(ModItems.EMPTY_SET.get());
                                output.accept(ModItems.AXIOM_DEDUCTION_TABLE.get());
                                output.accept(ModItems.DIGITALLY_CORRUPTED_BLOCK.get());
                                output.accept(ModItems.DIGITALLY_POLLUTED_WATER_BUCKET.get());
                                output.accept(ModItems.AXIOM_CASE.get());
                                output.accept(ModItems.ORACLE_CASE.get());
                                output.accept(ModItems.TRUTH_CASE.get());
                                output.accept(ModItems.LINGXU_SWORD.get());
                                output.accept(ModItems.LINGXU_PICKAXE.get());
                                output.accept(ModItems.LINGXU_AXE.get());
                                output.accept(ModItems.LINGXU_SHOVEL.get());
                                output.accept(ModItems.LINGXU_HOE.get());
                                output.accept(ModItems.LINGXU_HELMET.get());
                                output.accept(ModItems.LINGXU_CHESTPLATE.get());
                                output.accept(ModItems.LINGXU_LEGGINGS.get());
                                output.accept(ModItems.LINGXU_BOOTS.get());
                                output.accept(ModItems.LINGXU_MIRROR.get());
                                output.accept(ModItems.DIGITAL_POLLUTION_METER.get());
                                output.accept(ModItems.MATHEMATICAL_RING.get());
                                output.accept(ModItems.GRADUATION_CAP.get());
                                output.accept(ModItems.THREE_CAT_MILK_POWDER.get());
                                output.accept(ModItems.FIVE_SPAWN_EGG.get());
                                output.accept(ModItems.SIX_SPAWN_EGG.get());
                                output.accept(ModItems.SEVEN_SPAWN_EGG.get());
                                output.accept(ModItems.EIGHT_SPAWN_EGG.get());
                                output.accept(ModItems.NINE_SPAWN_EGG.get());
                                output.accept(ModItems.GEOMETRY_HOLDER_SPAWN_EGG.get());
                                output.accept(ModItems.GEOMETRY_CONSTRUCT_SPAWN_EGG.get());
                            })
                            .build());

    public static final Supplier<CreativeModeTab> MATHMASTER_NOTES_TAB =
            CREATIVE_MODE_TABS.register("mathmaster_notes",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.mathmaster_notes"))
                            .icon(() -> StudyNoteItem.createAxiomNote(AxiomDefinition.PEANO_AXIOMS, 5))
                            .displayItems((parameters, output) -> {
                                for (int level = 1; level <= 5; level++) {
                                    output.accept(StudyNoteItem.createBlankNote(level));
                                }
                                for (AxiomDefinition axiom : AxiomDefinition.values()) {
                                    output.accept(StudyNoteItem.createAxiomNote(axiom, axiom.maximumNoteLevel()));
                                }
                            })
                            .build());

    public static final Supplier<CreativeModeTab> MATHMASTER_BLOCKS_TAB =
            CREATIVE_MODE_TABS.register("mathmaster_blocks",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.mathmaster_blocks"))
                            .withTabsBefore(ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "mathmaster"))
                            .icon(() -> new ItemStack(ModItems.DIGITALLY_CORRUPTED_BLOCK.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.LINGXU_BLOCK.get());
                                output.accept(ModItems.PRIME_BLOCK.get());
                                output.accept(ModItems.DIGITALLY_CORRUPTED_BLOCK.get());
                                output.accept(ModItems.COLLATZ_SAPLING.get());
                                output.accept(ModItems.COLLATZ_ROOT.get());
                                output.accept(ModItems.COLLATZ_LOG.get());
                                output.accept(ModItems.COLLATZ_NODE.get());
                                output.accept(ModItems.COLLATZ_LEAVES.get());
                                output.accept(ModItems.FRUITING_COLLATZ_LEAVES.get());
                                output.accept(ModItems.COLLATZ_PLANKS.get());
                                output.accept(ModItems.COLLATZ_SLAB.get());
                                output.accept(ModItems.COLLATZ_STAIRS.get());
                                output.accept(ModItems.COLLATZ_FENCE.get());
                                output.accept(ModItems.COLLATZ_FENCE_GATE.get());
                                output.accept(ModItems.COLLATZ_DOOR.get());
                                output.accept(ModItems.COLLATZ_TRAPDOOR.get());
                                output.accept(ModItems.COLLATZ_BUTTON.get());
                                output.accept(ModItems.COLLATZ_PRESSURE_PLATE.get());
                                output.accept(ModItems.PURPLE_COLLATZ_SAPLING.get());
                                output.accept(ModItems.PURPLE_COLLATZ_ROOT.get());
                                output.accept(ModItems.PURPLE_COLLATZ_LOG.get());
                                output.accept(ModItems.PURPLE_COLLATZ_NODE.get());
                                output.accept(ModItems.PURPLE_COLLATZ_LEAVES.get());
                                output.accept(ModItems.PURPLE_FRUITING_COLLATZ_LEAVES.get());
                                output.accept(ModItems.PURPLE_COLLATZ_PLANKS.get());
                                output.accept(ModItems.PURPLE_COLLATZ_SLAB.get());
                                output.accept(ModItems.PURPLE_COLLATZ_STAIRS.get());
                                output.accept(ModItems.PURPLE_COLLATZ_FENCE.get());
                                output.accept(ModItems.PURPLE_COLLATZ_FENCE_GATE.get());
                                output.accept(ModItems.PURPLE_COLLATZ_DOOR.get());
                                output.accept(ModItems.PURPLE_COLLATZ_TRAPDOOR.get());
                                output.accept(ModItems.PURPLE_COLLATZ_BUTTON.get());
                                output.accept(ModItems.PURPLE_COLLATZ_PRESSURE_PLATE.get());
                                output.accept(ModItems.BLUE_COLLATZ_SAPLING.get());
                                output.accept(ModItems.BLUE_COLLATZ_ROOT.get());
                                output.accept(ModItems.BLUE_COLLATZ_LOG.get());
                                output.accept(ModItems.BLUE_COLLATZ_NODE.get());
                                output.accept(ModItems.BLUE_COLLATZ_LEAVES.get());
                                output.accept(ModItems.BLUE_FRUITING_COLLATZ_LEAVES.get());
                                output.accept(ModItems.BLUE_COLLATZ_PLANKS.get());
                                output.accept(ModItems.BLUE_COLLATZ_SLAB.get());
                                output.accept(ModItems.BLUE_COLLATZ_STAIRS.get());
                                output.accept(ModItems.BLUE_COLLATZ_FENCE.get());
                                output.accept(ModItems.BLUE_COLLATZ_FENCE_GATE.get());
                                output.accept(ModItems.BLUE_COLLATZ_DOOR.get());
                                output.accept(ModItems.BLUE_COLLATZ_TRAPDOOR.get());
                                output.accept(ModItems.BLUE_COLLATZ_BUTTON.get());
                                output.accept(ModItems.BLUE_COLLATZ_PRESSURE_PLATE.get());
                                output.accept(ModItems.N_ALTAR.get());
                                output.accept(ModItems.GEOMETRY_ALTAR.get());
                                output.accept(ModPaintings.createDigitalFive());
                            })
                            .build());

}
