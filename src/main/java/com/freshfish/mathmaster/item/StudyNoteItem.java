package com.freshfish.mathmaster.item;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Optional;

public final class StudyNoteItem extends Item {
    private static final String AXIOM_TAG = "mathmaster_axiom";
    private static final String AXIOM_LEVEL_TAG = "mathmaster_axiom_level";

    public StudyNoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        return getAxiom(stack)
                .<Component>map(axiom -> Component.translatable(
                        "item.mathmaster.study_note.with_axiom",
                        name,
                        Component.translatable(axiom.translationKey())
                ).withStyle(axiom.category().formatting()))
                .orElse(name);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        Optional<AxiomNote> axiomNote = getAxiomNote(stack);
        if (axiomNote.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.mathmaster.study_note")
                    .withStyle(ChatFormatting.GRAY));
        }
        axiomNote.ifPresent(note -> {
            AxiomDefinition axiom = note.definition();
            tooltipComponents.add(
                Component.translatable("tooltip.mathmaster.study_note.axiom", Component.translatable(axiom.translationKey()))
                        .withStyle(axiom.category().formatting())
            );
            tooltipComponents.add(
                Component.translatable("tooltip.mathmaster.study_note.axiom_level", note.level())
                        .withStyle(axiom.category().formatting())
            );
        });
    }

    public static Optional<AxiomDefinition> getAxiom(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return parseAxiom(data.copyTag());
    }

    private static Optional<AxiomDefinition> parseAxiom(CompoundTag tag) {
        String id = tag.getString(AXIOM_TAG);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        try {
            return AxiomDefinition.byId(ResourceLocation.parse(id));
        } catch (IllegalArgumentException | ResourceLocationException ignored) {
            return Optional.empty();
        }
    }

    public static boolean hasAxiom(ItemStack stack) {
        return getAxiom(stack).isPresent();
    }

    public static void setAxiom(ItemStack stack, AxiomDefinition axiom) {
        setAxiom(stack, axiom, getNoteLevel(stack));
    }

    public static void setAxiom(ItemStack stack, AxiomDefinition axiom, int level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> {
                    tag.putString(AXIOM_TAG, axiom.id().toString());
                    tag.putInt(AXIOM_LEVEL_TAG, Math.max(1, Math.min(5, level)));
                });
    }

    public static int getAxiomLevel(ItemStack stack) {
        return getAxiomNote(stack).map(AxiomNote::level).orElse(0);
    }

    /** Reads both fields from one snapshot, preserving legacy item-level fallback. */
    public static Optional<AxiomNote> getAxiomNote(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return parseAxiom(tag).map(axiom -> {
            int stored = tag.getInt(AXIOM_LEVEL_TAG);
            int level = stored > 0 ? Math.max(1, Math.min(5, stored)) : getNoteLevel(stack);
            return new AxiomNote(axiom, level);
        });
    }

    public record AxiomNote(AxiomDefinition definition, int level) {
    }

    public static int getNoteLevel(ItemStack stack) {
        if (stack.is(com.freshfish.mathmaster.init.ModItems.GRADE_2_MATH_STUDY_NOTE.get())) {
            return 1;
        }
        if (stack.is(com.freshfish.mathmaster.init.ModItems.JUNIOR_HIGH_MATH_STUDY_NOTE.get())) {
            return 2;
        }
        if (stack.is(com.freshfish.mathmaster.init.ModItems.SENIOR_HIGH_MATH_STUDY_NOTE.get())) {
            return 3;
        }
        if (stack.is(com.freshfish.mathmaster.init.ModItems.ADVANCED_MATH_STUDY_NOTE.get())) {
            return 4;
        }
        if (stack.is(com.freshfish.mathmaster.init.ModItems.MILLENNIUM_PROBLEMS_STUDY_NOTE.get())) {
            return 5;
        }
        return 0;
    }

    public static ItemStack createBlankNote(int level) {
        Item item = switch (level) {
            case 1 -> com.freshfish.mathmaster.init.ModItems.GRADE_2_MATH_STUDY_NOTE.get();
            case 2 -> com.freshfish.mathmaster.init.ModItems.JUNIOR_HIGH_MATH_STUDY_NOTE.get();
            case 3 -> com.freshfish.mathmaster.init.ModItems.SENIOR_HIGH_MATH_STUDY_NOTE.get();
            case 4 -> com.freshfish.mathmaster.init.ModItems.ADVANCED_MATH_STUDY_NOTE.get();
            case 5 -> com.freshfish.mathmaster.init.ModItems.MILLENNIUM_PROBLEMS_STUDY_NOTE.get();
            default -> throw new IllegalArgumentException("Study note level must be between 1 and 5");
        };
        return new ItemStack(item);
    }

    public static ItemStack createAxiomNote(AxiomDefinition axiom, int level) {
        if (!axiom.acceptsNoteLevel(level)) {
            throw new IllegalArgumentException("Axiom does not accept study note level " + level);
        }
        ItemStack stack = createBlankNote(level);
        setAxiom(stack, axiom, level);
        return stack;
    }
}
