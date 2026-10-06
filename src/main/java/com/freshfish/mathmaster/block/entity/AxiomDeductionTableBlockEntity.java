package com.freshfish.mathmaster.block.entity;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.init.ModBlockEntities;
import com.freshfish.mathmaster.item.StudyNoteItem;
import com.freshfish.mathmaster.menu.AxiomDeductionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;

public final class AxiomDeductionTableBlockEntity extends BaseContainerBlockEntity {
    public static final int NOTE_SLOT = 0;
    public static final int MATERIAL_SLOT = 1;
    public static final int CONTAINER_SIZE = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public AxiomDeductionTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AXIOM_DEDUCTION_TABLE.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.mathmaster.axiom_deduction_table");
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case NOTE_SLOT -> StudyNoteItem.getNoteLevel(stack) > 0 && this.items.get(NOTE_SLOT).isEmpty();
            case MATERIAL_SLOT -> AxiomDefinition.isMaterial(stack);
            default -> false;
        };
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AxiomDeductionMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }
}
