package com.freshfish.mathmaster.menu;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.init.ModMenuTypes;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.item.StudyNoteItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.EnumSet;

public final class AxiomCaseMenu extends AbstractContainerMenu {
    private static final int PLAYER_SLOT_COUNT = 36;

    private final Inventory playerInventory;
    private final SimpleContainer contents;
    private final ItemStack sourceStack;
    private final int capacity;
    private final int sourceSlot;

    public AxiomCaseMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readVarInt(), buffer.readVarInt(), true);
    }

    public AxiomCaseMenu(int containerId, Inventory inventory, int capacity, int sourceSlot) {
        this(containerId, inventory, capacity, sourceSlot, false);
    }

    private AxiomCaseMenu(
            int containerId,
            Inventory inventory,
            int capacity,
            int sourceSlot,
            boolean clientSide
    ) {
        super(ModMenuTypes.AXIOM_CASE.get(), containerId);
        if (capacity != 3 && capacity != 6 && capacity != 9) {
            throw new IllegalArgumentException("Unsupported axiom case capacity: " + capacity);
        }
        this.playerInventory = inventory;
        this.capacity = capacity;
        this.sourceSlot = sourceSlot;
        this.sourceStack = clientSide ? ItemStack.EMPTY : inventory.getItem(sourceSlot);
        this.contents = new SimpleContainer(capacity);

        if (!clientSide) {
            loadContents(this.sourceStack, this.contents);
            ejectDuplicateActiveNotes(inventory, this.contents);
            saveContents();
            this.contents.addListener(ignored -> saveContents());
        }

        int caseStartX = 8 + (9 - capacity) * 9;
        for (int index = 0; index < capacity; index++) {
            int slotIndex = index;
            this.addSlot(new Slot(this.contents, slotIndex, caseStartX + slotIndex * 18, 27) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return StudyNoteItem.getAxiom(stack)
                            .map(axiom -> !axiom.isActive()
                                    || !hasActiveCategory(axiom.category(), slotIndex))
                            .orElse(false);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventorySlot = column + row * 9 + 9;
                this.addSlot(playerSlot(inventory, inventorySlot, 8 + column * 18, 72 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(playerSlot(inventory, column, 8 + column * 18, 130));
        }
    }

    private Slot playerSlot(Inventory inventory, int inventorySlot, int x, int y) {
        return new Slot(inventory, inventorySlot, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return inventorySlot != AxiomCaseMenu.this.sourceSlot && super.mayPickup(player);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return inventorySlot != AxiomCaseMenu.this.sourceSlot && super.mayPlace(stack);
            }
        };
    }

    private static void loadContents(ItemStack source, SimpleContainer target) {
        ItemContainerContents stored = source.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(target.getContainerSize(), ItemStack.EMPTY);
        stored.copyInto(items);
        for (int index = 0; index < items.size(); index++) {
            target.setItem(index, items.get(index));
        }
    }

    private static void ejectDuplicateActiveNotes(Inventory inventory, SimpleContainer contents) {
        EnumSet<AxiomDefinition.AxiomCategory> activeCategories =
                EnumSet.noneOf(AxiomDefinition.AxiomCategory.class);
        for (int index = 0; index < contents.getContainerSize(); index++) {
            ItemStack note = contents.getItem(index);
            var axiom = StudyNoteItem.getAxiom(note);
            if (axiom.isEmpty() || !axiom.get().isActive()) {
                continue;
            }
            if (activeCategories.add(axiom.get().category())) {
                continue;
            }
            ItemStack duplicate = contents.removeItemNoUpdate(index);
            if (!inventory.add(duplicate)) {
                inventory.player.drop(duplicate, false);
            }
        }
    }

    private void saveContents() {
        if (!this.sourceStack.isEmpty()) {
            this.sourceStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.contents.getItems()));
            this.playerInventory.setChanged();
        }
    }

    public int getCapacity() {
        return this.capacity;
    }

    public ItemStack getCaseItem(int index) {
        return index >= 0 && index < this.contents.getContainerSize()
                ? this.contents.getItem(index)
                : ItemStack.EMPTY;
    }

    public boolean conflictsWithEquippedActive(ItemStack stack) {
        return StudyNoteItem.getAxiom(stack)
                .filter(AxiomDefinition::isActive)
                .map(axiom -> hasActiveCategory(axiom.category(), -1))
                .orElse(false);
    }

    public AxiomDefinition.AxiomCategory carriedConflictCategory() {
        return StudyNoteItem.getAxiom(this.getCarried())
                .filter(AxiomDefinition::isActive)
                .filter(axiom -> hasActiveCategory(axiom.category(), -1))
                .map(AxiomDefinition::category)
                .orElse(null);
    }

    private boolean hasActiveCategory(AxiomDefinition.AxiomCategory category, int excludedSlot) {
        for (int index = 0; index < this.contents.getContainerSize(); index++) {
            if (index == excludedSlot) {
                continue;
            }
            var axiom = StudyNoteItem.getAxiom(this.contents.getItem(index));
            if (axiom.isPresent() && axiom.get().isActive() && axiom.get().category() == category) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide()) {
            return true;
        }
        return this.playerInventory.getItem(this.sourceSlot) == this.sourceStack
                && AxiomCaseItem.getCapacity(this.sourceStack) == this.capacity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = this.capacity;
        int playerInventoryEnd = playerStart + 27;
        int hotbarEnd = playerStart + PLAYER_SLOT_COUNT;

        if (index < this.capacity) {
            if (!this.moveItemStackTo(stack, playerStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (StudyNoteItem.hasAxiom(stack)) {
            if (!this.moveItemStackTo(stack, 0, this.capacity, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < playerInventoryEnd) {
            if (!this.moveItemStackTo(stack, playerInventoryEnd, hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, playerStart, playerInventoryEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        this.saveContents();
        return original;
    }

    @Override
    public void removed(Player player) {
        this.saveContents();
        super.removed(player);
    }
}
