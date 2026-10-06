package com.freshfish.mathmaster.compat.jei;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.item.StudyNoteItem;
import net.minecraft.world.item.ItemStack;

public record AxiomDeductionJeiRecipe(AxiomDefinition axiom, int level) {
    public ItemStack inputNote() {
        return StudyNoteItem.createBlankNote(this.level);
    }

    public ItemStack material() {
        return this.axiom.materialStack();
    }

    public ItemStack outputNote() {
        return StudyNoteItem.createAxiomNote(this.axiom, this.level);
    }
}
