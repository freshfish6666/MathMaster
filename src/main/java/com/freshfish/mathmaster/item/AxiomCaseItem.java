package com.freshfish.mathmaster.item;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.menu.AxiomCaseMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AxiomCaseItem extends Item {
    private static final String SELECTED_AXIOM_TAG = "mathmaster_selected_axiom";
    private final int capacity;

    public AxiomCaseItem(int capacity, Properties properties) {
        super(properties.stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
        this.capacity = capacity;
    }

    public int capacity() {
        return this.capacity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int sourceSlot = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().selected
                    : Inventory.SLOT_OFFHAND;
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) ->
                                    new AxiomCaseMenu(containerId, inventory, this.capacity, sourceSlot),
                            stack.getHoverName()
                    ),
                    buffer -> {
                        buffer.writeVarInt(this.capacity);
                        buffer.writeVarInt(sourceSlot);
                    }
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        long stored = contents.nonEmptyStream().count();
        tooltipComponents.add(Component.translatable("tooltip.mathmaster.axiom_case.capacity", stored, this.capacity)
                .withStyle(ChatFormatting.GRAY));
        for (ItemStack note : contents.nonEmptyItems()) {
            StudyNoteItem.getAxiomNote(note).ifPresent(parsed -> tooltipComponents.add(
                    Component.translatable(
                            "tooltip.mathmaster.axiom_case.entry",
                            Component.translatable(parsed.definition().translationKey()),
                            parsed.level()
                    ).withStyle(parsed.definition().category().formatting())
            ));
        }
        tooltipComponents.add(Component.translatable("tooltip.mathmaster.axiom_case.open")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    public static int getCapacity(ItemStack stack) {
        return stack.getItem() instanceof AxiomCaseItem item ? item.capacity() : 0;
    }

    public static List<AxiomDefinition> getAxioms(ItemStack stack) {
        List<AxiomDefinition> axioms = new ArrayList<>();
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        for (ItemStack note : contents.nonEmptyItems()) {
            StudyNoteItem.getAxiom(note).ifPresent(axioms::add);
        }
        return List.copyOf(axioms);
    }

    public static List<EquippedAxiom> getEquippedAxioms(ItemStack stack) {
        List<EquippedAxiom> axioms = new ArrayList<>();
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        for (ItemStack note : contents.nonEmptyItems()) {
            StudyNoteItem.getAxiomNote(note).ifPresent(parsed ->
                    axioms.add(new EquippedAxiom(parsed.definition(), parsed.level())));
        }
        return List.copyOf(axioms);
    }

    public record EquippedAxiom(AxiomDefinition definition, int level) {
    }

    public static Optional<AxiomDefinition> getSelectedAxiom(ItemStack stack) {
        String id = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getString(SELECTED_AXIOM_TAG);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        try {
            return AxiomDefinition.byId(net.minecraft.resources.ResourceLocation.parse(id));
        } catch (IllegalArgumentException | ResourceLocationException ignored) {
            return Optional.empty();
        }
    }

    public static void setSelectedAxiom(ItemStack stack, AxiomDefinition axiom) {
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(SELECTED_AXIOM_TAG, axiom.id().toString()));
    }

}
