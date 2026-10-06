package com.freshfish.mathmaster.compat.jei;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class AxiomDeductionJeiCategory implements IRecipeCategory<AxiomDeductionJeiRecipe> {
    private static final int WIDTH = 154;
    private static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable plus;
    private final IDrawable arrow;

    public AxiomDeductionJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(com.freshfish.mathmaster.init.ModItems.AXIOM_DEDUCTION_TABLE.get());
        this.plus = guiHelper.getRecipePlusSign();
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<AxiomDeductionJeiRecipe> getRecipeType() {
        return MathMasterJeiPlugin.AXIOM_DEDUCTION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mathmaster.axiom_deduction");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AxiomDeductionJeiRecipe recipe, IFocusGroup focuses) {
        AxiomDefinition axiom = recipe.axiom();
        builder.addInputSlot(8, 10)
                .setStandardSlotBackground()
                .addItemStack(recipe.inputNote());
        builder.addInputSlot(45, 10)
                .setStandardSlotBackground()
                .addItemStack(recipe.material());
        builder.addOutputSlot(126, 10)
                .setOutputSlotBackground()
                .addItemStack(recipe.outputNote())
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(axiom.skillTooltip(recipe.level())));
    }

    @Override
    public void draw(
            AxiomDeductionJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        this.plus.draw(graphics, 29, 12);
        this.arrow.draw(graphics, 86, 10);
        Component name = Component.translatable(recipe.axiom().translationKey());
        Component level = Component.translatable("jei.mathmaster.axiom.current_level", recipe.level());
        graphics.drawString(Minecraft.getInstance().font, name, 8, 39, recipe.axiom().category().color(), false);
        graphics.drawString(Minecraft.getInstance().font, level, 8, 51, 0xFFAAAAAA, false);
    }
}
