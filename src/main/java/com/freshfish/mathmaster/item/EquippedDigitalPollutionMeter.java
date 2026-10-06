package com.freshfish.mathmaster.item;

import com.freshfish.mathmaster.init.ModItems;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.CuriosApi;

public final class EquippedDigitalPollutionMeter {
    public static final String SLOT_ID = "trinkets";

    private EquippedDigitalPollutionMeter() {
    }

    public static boolean isEquipped(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.findCurio(SLOT_ID, 0))
                .map(result -> result.stack().is(ModItems.DIGITAL_POLLUTION_METER.get()))
                .orElse(false);
    }
}
