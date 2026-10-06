package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.entity.FiveEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Prevents blocks from being attached to Number 5's temporary living surface. */
public final class FivePlatformHandler {
    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getLevel() instanceof Level level)) {
            return;
        }
        double x = event.getPos().getX() + 0.5D;
        double y = event.getPos().getY();
        double z = event.getPos().getZ() + 0.5D;
        AABB search = new AABB(x - 27.0D, y - 5.0D, z - 27.0D,
                x + 27.0D, y + 2.0D, z + 27.0D);
        for (FiveEntity five : level.getEntitiesOfClass(FiveEntity.class, search)) {
            if (five.isOverPlatform(x, z) && Math.abs(y - five.platformTopY()) <= 1.25D) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
