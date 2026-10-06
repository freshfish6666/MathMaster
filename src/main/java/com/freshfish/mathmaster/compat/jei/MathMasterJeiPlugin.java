package com.freshfish.mathmaster.compat.jei;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.item.StudyNoteItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class MathMasterJeiPlugin implements IModPlugin {
    public static final RecipeType<AxiomDeductionJeiRecipe> AXIOM_DEDUCTION = RecipeType.create(
            MathMaster.MODID,
            "axiom_deduction",
            AxiomDeductionJeiRecipe.class
    );
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            MathMaster.MODID,
            "jei_plugin"
    );

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    @SuppressWarnings("deprecation") // JEI 19.51 still requires this bridge method on its current interpreter interface.
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> interpreter = new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return subtypeId(stack);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return subtypeId(stack);
            }
        };
        for (Item item : studyNoteItems()) {
            registration.registerSubtypeInterpreter(item, interpreter);
        }
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(maximumLevelAxiomNotes());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AxiomDeductionJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<AxiomDeductionJeiRecipe> recipes = new ArrayList<>();
        for (AxiomDefinition axiom : AxiomDefinition.values()) {
            for (int level = axiom.requiredNoteLevel(); level <= axiom.maximumNoteLevel(); level++) {
                recipes.add(new AxiomDeductionJeiRecipe(axiom, level));
            }
        }
        registration.addRecipes(AXIOM_DEDUCTION, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModItems.AXIOM_DEDUCTION_TABLE.get(), AXIOM_DEDUCTION);
    }

    private static List<ItemStack> maximumLevelAxiomNotes() {
        return java.util.Arrays.stream(AxiomDefinition.values())
                .map(axiom -> StudyNoteItem.createAxiomNote(axiom, axiom.maximumNoteLevel()))
                .toList();
    }

    private static String subtypeId(ItemStack stack) {
        return StudyNoteItem.getAxiom(stack)
                .map(axiom -> axiom.id() + "@" + StudyNoteItem.getAxiomLevel(stack))
                .orElse("");
    }

    private static List<Item> studyNoteItems() {
        return List.of(
                ModItems.GRADE_2_MATH_STUDY_NOTE.get(),
                ModItems.JUNIOR_HIGH_MATH_STUDY_NOTE.get(),
                ModItems.SENIOR_HIGH_MATH_STUDY_NOTE.get(),
                ModItems.ADVANCED_MATH_STUDY_NOTE.get(),
                ModItems.MILLENNIUM_PROBLEMS_STUDY_NOTE.get()
        );
    }
}
