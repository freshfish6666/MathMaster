package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.item.StudyNoteItem;
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
                                output.accept(ModItems.LINGXU_BLOCK.get());
                                output.accept(ModItems.LINGXU_INGOT.get());
                                output.accept(ModItems.LINGXU_NUGGET.get());
                                output.accept(ModItems.PRIME_CORE.get());
                                output.accept(ModItems.AXIOM_DEDUCTION_TABLE.get());
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
                                output.accept(ModItems.GRADUATION_CAP.get());
                                output.accept(ModItems.THREE_CAT_MILK_POWDER.get());
                                output.accept(ModItems.EIGHT_SPAWN_EGG.get());
                                output.accept(ModItems.NINE_SPAWN_EGG.get());
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
}
