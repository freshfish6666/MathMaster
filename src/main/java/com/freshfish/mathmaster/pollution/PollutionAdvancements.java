package com.freshfish.mathmaster.pollution;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Called only after the pollution threshold has actually killed its victim. */
public final class PollutionAdvancements {
    private PollutionAdvancements() {}

    public static void awardDeath(LivingEntity victim) {
        String path;
        if (victim.getType() == EntityType.WARDEN) path = "high_tower_falls";
        else if (victim.getType() == EntityType.VILLAGER) path = "last_breath";
        else if (victim.getType() == EntityType.IRON_GOLEM) path = "iron_to_dust";
        else return;
        if (!(victim.level() instanceof ServerLevel level)) return;
        var advancement = level.getServer().getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath("mathmaster", "pollution/" + path));
        if (advancement == null) return;
        for (var player : level.players()) {
            if (player.isAlive() && player.distanceToSqr(victim) <= 32.0D * 32.0D) {
                player.getAdvancements().award(advancement, "completed");
            }
        }
    }
}
