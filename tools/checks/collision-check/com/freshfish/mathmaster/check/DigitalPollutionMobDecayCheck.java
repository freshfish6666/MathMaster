package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.event.DigitalPollutionHandler;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.pollution.DigitalPollutionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

final class DigitalPollutionMobDecayCheck {
    private DigitalPollutionMobDecayCheck() {
    }

    static int run(ServerLevel level) {
        DigitalPollutionData data = new DigitalPollutionData();
        data.add(20);
        tick(data, 1_199, false);
        require(data.getValue() == 20, "mob pollution decayed before the first minute");
        tick(data, 1, false);
        require(data.getValue() == 19, "first clean minute did not reduce one pollution");
        tick(data, 1_200, false);
        require(data.getValue() == 17, "second clean minute did not reduce two pollution");

        tick(data, 600, false);
        data.tickNonPlayerDecay(true);
        tick(data, 1_199, false);
        require(data.getValue() == 17, "pollution effect did not reset the clean-time decay ramp");
        tick(data, 1, false);
        require(data.getValue() == 16, "decay did not restart at one after pollution effect");

        CompoundTag oldData = new CompoundTag();
        oldData.putInt("value", 10);
        DigitalPollutionData migrated = new DigitalPollutionData();
        migrated.deserializeNBT(level.registryAccess(), oldData);
        tick(migrated, 1_200, false);
        require(migrated.getValue() == 9, "old pollution data did not default to a fresh decay ramp");

        var cow = EntityType.COW.create(level);
        if (cow == null) {
            throw new AssertionError("Decay-check cow failed to instantiate");
        }
        cow.getData(ModAttachments.DIGITAL_POLLUTION).add(5);
        DigitalPollutionHandler handler = new DigitalPollutionHandler();
        for (int i = 0; i < 1_200; i++) {
            handler.onNonPlayerEntityTick(new EntityTickEvent.Post(cow));
        }
        require(cow.getData(ModAttachments.DIGITAL_POLLUTION).getValue() == 4,
                "intelligent non-player entity did not run the decay handler");
        cow.discard();
        return 7;
    }

    private static void tick(DigitalPollutionData data, int ticks, boolean blocked) {
        for (int i = 0; i < ticks; i++) {
            data.tickNonPlayerDecay(blocked);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
