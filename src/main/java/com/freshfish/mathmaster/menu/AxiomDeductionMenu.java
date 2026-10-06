package com.freshfish.mathmaster.menu;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.block.entity.AxiomDeductionTableBlockEntity;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModMenuTypes;
import com.freshfish.mathmaster.item.StudyNoteItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AxiomDeductionMenu extends AbstractContainerMenu {
    private static final int TABLE_SLOT_COUNT = 2;
    private static final int PLAYER_INVENTORY_START = TABLE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container container;
    private final ContainerLevelAccess access;

    public AxiomDeductionMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, clientContainer(inventory, buffer), ContainerLevelAccess.NULL);
    }

    public AxiomDeductionMenu(int containerId, Inventory inventory, AxiomDeductionTableBlockEntity table) {
        this(containerId, inventory, table, ContainerLevelAccess.create(table.getLevel(), table.getBlockPos()));
    }

    private AxiomDeductionMenu(
            int containerId,
            Inventory inventory,
            Container container,
            ContainerLevelAccess access
    ) {
        super(ModMenuTypes.AXIOM_DEDUCTION_TABLE.get(), containerId);
        checkContainerSize(container, TABLE_SLOT_COUNT);
        this.container = container;
        this.access = access;
        container.startOpen(inventory.player);

        this.addSlot(new Slot(container, AxiomDeductionTableBlockEntity.NOTE_SLOT, 59, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return StudyNoteItem.getNoteLevel(stack) > 0;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(container, AxiomDeductionTableBlockEntity.MATERIAL_SLOT, 143, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AxiomDefinition.isMaterial(stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9,
                        28 + column * 18, 151 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 28 + column * 18, 209));
        }
    }

    public ItemStack getNote() {
        return this.container.getItem(AxiomDeductionTableBlockEntity.NOTE_SLOT);
    }

    public ItemStack getMaterial() {
        return this.container.getItem(AxiomDeductionTableBlockEntity.MATERIAL_SLOT);
    }

    public int getNoteLevel() {
        return StudyNoteItem.getNoteLevel(this.getNote());
    }

    public boolean canApply(AxiomDefinition axiom) {
        return axiom.acceptsNoteLevel(this.getNoteLevel())
                && !StudyNoteItem.hasAxiom(this.getNote())
                && axiom.acceptsMaterial(this.getMaterial());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        AxiomDefinition[] axioms = AxiomDefinition.values();
        if (!(player instanceof ServerPlayer) || id < 0 || id >= axioms.length || !this.stillValid(player)) {
            return false;
        }

        AxiomDefinition axiom = axioms[id];
        if (!this.canApply(axiom)) {
            return false;
        }

        StudyNoteItem.setAxiom(this.getNote(), axiom, this.getNoteLevel());
        this.getMaterial().shrink(1);
        this.container.setChanged();
        this.broadcastChanges();
        this.access.execute((level, pos) -> level.playSound(
                null,
                pos,
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS,
                0.8F,
                1.1F
        ));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < TABLE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (StudyNoteItem.getNoteLevel(stack) > 0) {
            if (!this.moveItemStackTo(stack, AxiomDeductionTableBlockEntity.NOTE_SLOT,
                    AxiomDeductionTableBlockEntity.NOTE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (AxiomDefinition.isMaterial(stack)) {
            if (!this.moveItemStackTo(stack, AxiomDeductionTableBlockEntity.MATERIAL_SLOT,
                    AxiomDeductionTableBlockEntity.MATERIAL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.AXIOM_DEDUCTION_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    private static Container clientContainer(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        if (inventory.player.level().getBlockEntity(pos) instanceof Container container) {
            return container;
        }
        return new SimpleContainer(TABLE_SLOT_COUNT);
    }
}
