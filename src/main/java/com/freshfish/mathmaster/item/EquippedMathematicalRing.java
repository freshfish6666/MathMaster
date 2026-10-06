package com.freshfish.mathmaster.item;

import com.freshfish.mathmaster.init.ModItems;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.CuriosApi;

/** Curios lookup for the creative-only ring that bypasses axiom cooldowns. */
public final class EquippedMathematicalRing {
    public static final String SLOT_ID = "ring";

    private EquippedMathematicalRing() {
    }

    public static boolean isEquipped(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.findCurio(SLOT_ID, 0))
                .map(result -> result.stack().is(ModItems.MATHEMATICAL_RING.get()))
                .orElse(false);
    }
}
