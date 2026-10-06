package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.entity.FiveEntity;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

/** Focused server assertions for the experimental living platform. */
final class FiveEntityCheck {
    private FiveEntityCheck() {}

    static int run(net.minecraft.server.level.ServerLevel level) {
        FiveEntity five = ModEntities.FIVE.get().create(level);
        if (five == null) throw new AssertionError("Five failed to instantiate");
        five.setPos(80.0D, 200.0D, 80.0D);
        level.addFreshEntity(five);
        if (Math.abs(five.getBbWidth() - 50.0F) > 1.0E-5F) {
            throw new AssertionError("Five width was " + five.getBbWidth());
        }

        var rider = EntityType.COW.create(level);
        if (rider == null) throw new AssertionError("Five rider failed to instantiate");
        rider.setPos(80.0D, five.platformTopY() + 0.25D, 80.0D);
        rider.setDeltaMovement(0.0D, -0.4D, 0.0D);
        level.addFreshEntity(rider);
        five.tick();
        if (Math.abs(rider.getY() - five.platformTopY()) > 1.0E-5D || !rider.onGround()) {
            throw new AssertionError("Five failed to support rider: rider=" + rider.getY()
                    + ", top=" + five.platformTopY() + ", onGround=" + rider.onGround());
        }

        double anchoredX = five.getX();
        double anchoredZ = five.getZ();
        five.setDeltaMovement(new Vec3(4.0D, 0.0D, -3.0D));
        five.tick();
        if (Math.abs(five.getX() - anchoredX) > 1.0E-5D || Math.abs(five.getZ() - anchoredZ) > 1.0E-5D) {
            throw new AssertionError("Five moved horizontally");
        }
        if (!five.isOverPlatform(anchoredX + 20.0D, anchoredZ)
                || five.isOverPlatform(anchoredX + 25.0D, anchoredZ)) {
            throw new AssertionError("Five platform footprint was incorrect");
        }
        five.discard();
        rider.discard();
        return 4;
    }
}
