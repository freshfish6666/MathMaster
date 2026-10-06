package com.freshfish.mathmaster.axiom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

public final class EquippedAxiomCase {
    public static final String SLOT_ID = "axiom_case";

    private EquippedAxiomCase() {
    }

    public static Optional<ItemStack> find(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.findCurio(SLOT_ID, 0))
                .map(result -> result.stack())
                .filter(stack -> !stack.isEmpty());
    }
}
