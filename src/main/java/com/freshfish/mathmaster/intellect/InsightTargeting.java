package com.freshfish.mathmaster.intellect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.entity.PartEntity;

public final class InsightTargeting {
    public static final double RANGE = 6.0D;

    private InsightTargeting() {
    }

    public static LivingEntity findTarget(Player player) {
        HitResult hitResult = ProjectileUtil.getHitResultOnViewVector(
                player,
                entity -> isValidCandidate(player, entity),
                RANGE
        );
        if (hitResult instanceof EntityHitResult entityHitResult) {
            return resolveLivingTarget(entityHitResult.getEntity());
        }
        return null;
    }

    public static LivingEntity resolveLivingTarget(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        if (entity instanceof PartEntity<?> partEntity
                && partEntity.getParent() instanceof LivingEntity livingParent) {
            return livingParent;
        }
        return null;
    }

    private static boolean isValidCandidate(Player player, Entity entity) {
        LivingEntity livingTarget = resolveLivingTarget(entity);
        return livingTarget != null
                && livingTarget != player
                && livingTarget.isAlive()
                && entity.isPickable();
    }
}
