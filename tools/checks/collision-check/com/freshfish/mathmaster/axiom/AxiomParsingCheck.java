package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.item.StudyNoteItem;
import com.freshfish.mathmaster.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.List;

/** Compatibility cases for single-snapshot note reads and invocation-local case reuse. */
public final class AxiomParsingCheck {
    public static int run() {
        int checks = 0;
        for (int itemLevel = 1; itemLevel <= 5; itemLevel++) {
            for (int stored : new int[] {-4, 0, 1, 3, 5, 9}) {
                ItemStack note = StudyNoteItem.createBlankNote(itemLevel);
                CustomData.update(DataComponents.CUSTOM_DATA, note, tag -> {
                    tag.putString("mathmaster_axiom", AxiomDefinition.ADDITION_COMMUTATIVITY.id().toString());
                    if (stored != 0) tag.putInt("mathmaster_axiom_level", stored);
                    tag.putString("unrelated", "keep");
                });
                CustomData before = note.get(DataComponents.CUSTOM_DATA);
                int expected = stored > 0 ? Math.min(5, stored) : itemLevel;
                var parsed = StudyNoteItem.getAxiomNote(note).orElseThrow();
                if (parsed.definition() != AxiomDefinition.ADDITION_COMMUTATIVITY
                        || parsed.level() != expected || StudyNoteItem.getAxiomLevel(note) != expected
                        || StudyNoteItem.getAxiom(note).orElseThrow() != parsed.definition()
                        || !before.equals(note.get(DataComponents.CUSTOM_DATA))) {
                    throw new AssertionError("Legacy note changed: item=" + itemLevel + " stored=" + stored);
                }
                checks++;
            }
        }
        for (String id : new String[] {"", "mathmaster:missing", "invalid id", "MathMaster:UPPER", "mathmaster:a:b"}) {
            ItemStack note = StudyNoteItem.createBlankNote(5);
            CustomData.update(DataComponents.CUSTOM_DATA, note, tag -> {
                tag.putString("mathmaster_axiom", id);
                tag.putInt("mathmaster_axiom_level", 5);
            });
            CustomData before = note.get(DataComponents.CUSTOM_DATA);
            if (StudyNoteItem.getAxiomNote(note).isPresent() || StudyNoteItem.getAxiomLevel(note) != 0
                    || StudyNoteItem.getAxiom(note).isPresent() || !before.equals(note.get(DataComponents.CUSTOM_DATA))) {
                throw new AssertionError("Invalid ID accepted: " + id);
            }
            checks++;
        }
        for (String id : new String[] {"", "mathmaster:missing", "invalid id", "MathMaster:UPPER", "mathmaster:a:b"}) {
            ItemStack selectedCase = new ItemStack(ModItems.AXIOM_CASE.get());
            CustomData.update(DataComponents.CUSTOM_DATA, selectedCase, tag -> {
                tag.putString("mathmaster_selected_axiom", id);
                tag.putString("unrelated", "keep");
            });
            CustomData before = selectedCase.get(DataComponents.CUSTOM_DATA);
            if (AxiomCaseItem.getSelectedAxiom(selectedCase).isPresent()
                    || !before.equals(selectedCase.get(DataComponents.CUSTOM_DATA))) {
                throw new AssertionError("Invalid selected ID accepted or rewritten: " + id);
            }
            checks++;
        }
        ItemStack selectedCase = new ItemStack(ModItems.AXIOM_CASE.get());
        AxiomCaseItem.setSelectedAxiom(selectedCase, AxiomDefinition.GEODESIC);
        if (AxiomCaseItem.getSelectedAxiom(selectedCase).orElseThrow() != AxiomDefinition.GEODESIC) {
            throw new AssertionError("Valid selected ID changed");
        }
        checks++;
        ItemStack foreign = new ItemStack(Items.STONE);
        CustomData.update(DataComponents.CUSTOM_DATA, foreign,
                tag -> tag.putString("mathmaster_axiom", AxiomDefinition.ADDITION_COMMUTATIVITY.id().toString()));
        if (StudyNoteItem.getAxiomNote(foreign).orElseThrow().level() != 0) {
            throw new AssertionError("Foreign item fallback changed");
        }
        checks++;
        ItemStack caseStack = new ItemStack(ModItems.AXIOM_CASE.get());
        caseStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                StudyNoteItem.createAxiomNote(AxiomDefinition.MATHEMATICAL_INDUCTION, 4),
                StudyNoteItem.createAxiomNote(AxiomDefinition.INVOLUTION, 5),
                StudyNoteItem.createAxiomNote(AxiomDefinition.ADDITION_COMMUTATIVITY, 2))));
        var snapshot = AxiomCaseItem.getEquippedAxioms(caseStack);
        for (AxiomDefinition definition : AxiomDefinition.values()) {
            if (AxiomEffectManager.getEffectiveLevel(snapshot, definition)
                    != AxiomEffectManager.getEffectiveLevel(caseStack, definition)) {
                throw new AssertionError("Snapshot level differs for " + definition);
            }
            checks++;
        }
        caseStack.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (!AxiomCaseItem.getEquippedAxioms(caseStack).isEmpty()
                || AxiomEffectManager.getEffectiveLevel(caseStack, AxiomDefinition.INVOLUTION) != 0) {
            throw new AssertionError("Next invocation retained removed equipment");
        }
        return checks + 1;
    }
}
