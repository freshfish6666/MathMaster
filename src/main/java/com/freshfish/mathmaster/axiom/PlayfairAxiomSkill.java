package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.intellect.InsightTargeting;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative active implementation for the geometry-category Playfair axiom. */
public final class PlayfairAxiomSkill {
    private static final double AXIS_SELECTION_DISTANCE = 1.5D;
    private static final double PERPENDICULAR_TOLERANCE = 1.0D;
    private static final double MINIMUM_TARGET_DISTANCE = 2.0D;
    private static final double EPSILON = 1.0E-6D;
    private static final int GUIDE_INTERVAL_TICKS = 8;
    private static final double GUIDE_SPACING = 2.0D;
    private static final double PENDING_GUIDE_LENGTH = 4.0D;
    private static final DustParticleOptions QUARTZ_GUIDE_PARTICLE = new DustParticleOptions(
            new Vector3f(0.92F, 0.87F, 0.76F),
            0.38F
    );
    private static final Map<UUID, ActiveEffect> ACTIVE_EFFECTS = new HashMap<>();
    private static final Map<TargetKey, Map<UUID, ActiveEffect>> EFFECTS_BY_TARGET = new HashMap<>();

    public static void use(ServerPlayer player, int level) {
        if (player.isShiftKeyDown()) {
            cancel(player, true);
            return;
        }
        if (ACTIVE_EFFECTS.containsKey(player.getUUID())) {
            message(player, "message.mathmaster.axiom.playfair.already_active");
            return;
        }

        int actualLevel = Math.max(1, Math.min(5, level));
        AxiomSkillData data = player.getData(ModAttachments.AXIOM_SKILL_DATA);
        if (data.getPlayfairCooldownTicks() > 0) {
            message(player, "message.mathmaster.axiom.playfair.cooldown",
                    (data.getPlayfairCooldownTicks() + 19) / 20);
            return;
        }

        double range = 20.0D + 2.0D * actualLevel;
        LivingEntity target = findTarget(player, range);
        if (target == null || target instanceof Player) {
            message(player, "message.mathmaster.axiom.playfair.no_target");
            return;
        }
        if (EntityIntellectManager.get(target) == null) {
            message(player, "message.mathmaster.axiom.playfair.no_intellect");
            return;
        }

        int durationTicks = (5 + 5 * actualLevel) * 20;
        addEffect(player.getUUID(), new ActiveEffect(
                player.level().dimension(), target.getUUID(), player.position(), range, durationTicks));
        data.setPlayfairCooldownTicks((60 - 10 * actualLevel) * 20);
        DigitalPollutionManager.add(player, 2 * actualLevel);
        if (!player.isAlive()) {
            removeEffect(player.getUUID());
            return;
        }
        drawPendingAxes(player, player.position());
        message(player, "message.mathmaster.axiom.playfair.active", target.getDisplayName());
    }

    public static void cancel(ServerPlayer player, boolean notify) {
        ActiveEffect removed = removeEffect(player.getUUID());
        if (!notify) {
            return;
        }
        message(player, removed == null
                ? "message.mathmaster.axiom.playfair.nothing_active"
                : "message.mathmaster.axiom.playfair.cancelled");
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ActiveEffect effect = ACTIVE_EFFECTS.get(player.getUUID());
        if (effect == null) {
            return;
        }
        LivingEntity target = validate(player, effect);
        if (target == null) {
            return;
        }
        effect.remainingTicks--;
        if (effect.remainingTicks <= 0) {
            end(player, "message.mathmaster.axiom.playfair.expired");
            return;
        }
        if (effect.axis == null) {
            effect.axis = selectAxis(player.position().subtract(effect.origin));
            if (effect.axis == null) {
                if (effect.remainingTicks % GUIDE_INTERVAL_TICKS == 0) {
                    drawPendingAxes(player, effect.origin);
                }
                return;
            }
            message(player, "message.mathmaster.axiom.playfair.axis", effect.axis.getName().toUpperCase());
        }
        if (hasLeftAxis(player.position(), effect)) {
            end(player, "message.mathmaster.axiom.playfair.deviated");
            return;
        }
        keepTargetAway(player, target);
        if (effect.remainingTicks % GUIDE_INTERVAL_TICKS == 0) {
            drawSelectedCorridor(player, target, effect);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ActiveEffect effect = ACTIVE_EFFECTS.get(player.getUUID());
        if (effect == null || effect.axis == null || validate(player, effect) == null) {
            return;
        }
        if (hasLeftAxis(player.position(), effect)) {
            end(player, "message.mathmaster.axiom.playfair.deviated");
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker.getUUID().equals(effect.targetId)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTargetTick(EntityTickEvent.Post event) {
        if (EFFECTS_BY_TARGET.isEmpty() || !(event.getEntity() instanceof LivingEntity target)
                || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        Map<UUID, ActiveEffect> matching = EFFECTS_BY_TARGET.get(
                new TargetKey(level.dimension(), target.getUUID()));
        if (matching == null) {
            return;
        }
        // Snapshot only this target's owners: moving a target can trigger other event handlers.
        for (Map.Entry<UUID, ActiveEffect> entry : List.copyOf(matching.entrySet())) {
            ActiveEffect effect = entry.getValue();
            if (effect.axis == null) {
                continue;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.level() == level && !hasLeftAxis(player.position(), effect)
                    && player.distanceToSqr(target) <= effect.range * effect.range) {
                keepTargetAway(player, target);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            removeEffect(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            removeEffect(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Map<UUID, ActiveEffect> matching = EFFECTS_BY_TARGET.get(
                new TargetKey(event.getLevel().dimension(), event.getEntity().getUUID()));
        if (matching == null) {
            return;
        }
        List<UUID> owners = List.copyOf(matching.keySet());
        for (UUID ownerId : owners) {
            removeEffect(ownerId);
            if (event.getLevel() instanceof ServerLevel level
                    && level.getPlayerByUUID(ownerId) instanceof ServerPlayer player) {
                message(player, "message.mathmaster.axiom.playfair.lost");
            }
        }
    }

    private static LivingEntity validate(ServerPlayer player, ActiveEffect effect) {
        if (!player.isAlive() || !player.level().dimension().equals(effect.dimension) || !stillEquipped(player)) {
            end(player, "message.mathmaster.axiom.playfair.lost");
            return null;
        }
        Entity entity = ((ServerLevel) player.level()).getEntity(effect.targetId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            end(player, "message.mathmaster.axiom.playfair.lost");
            return null;
        }
        if (player.distanceToSqr(target) > effect.range * effect.range) {
            end(player, "message.mathmaster.axiom.playfair.out_of_range");
            return null;
        }
        return target;
    }

    private static boolean stillEquipped(ServerPlayer player) {
        ItemStack caseStack = EquippedAxiomCase.find(player).orElse(ItemStack.EMPTY);
        return !caseStack.isEmpty()
                && AxiomCaseItem.getAxioms(caseStack).contains(AxiomDefinition.PARALLEL_POSTULATE);
    }

    private static LivingEntity findTarget(ServerPlayer player, double range) {
        HitResult result = ProjectileUtil.getHitResultOnViewVector(
                player,
                entity -> entity.isPickable() && InsightTargeting.resolveLivingTarget(entity) != null,
                range
        );
        if (result instanceof EntityHitResult entityHit) {
            LivingEntity target = InsightTargeting.resolveLivingTarget(entityHit.getEntity());
            return target == player ? null : target;
        }
        return null;
    }

    private static Direction.Axis selectAxis(Vec3 displacement) {
        double x = Math.abs(displacement.x);
        double y = Math.abs(displacement.y);
        double z = Math.abs(displacement.z);
        double largest = Math.max(x, Math.max(y, z));
        if (largest <= AXIS_SELECTION_DISTANCE) {
            return null;
        }
        if (x >= y && x >= z) return Direction.Axis.X;
        if (y >= z) return Direction.Axis.Y;
        return Direction.Axis.Z;
    }

    private static boolean hasLeftAxis(Vec3 position, ActiveEffect effect) {
        Vec3 delta = position.subtract(effect.origin);
        return switch (effect.axis) {
            case X -> Math.abs(delta.y) > PERPENDICULAR_TOLERANCE
                    || Math.abs(delta.z) > PERPENDICULAR_TOLERANCE;
            case Y -> Math.abs(delta.x) > PERPENDICULAR_TOLERANCE
                    || Math.abs(delta.z) > PERPENDICULAR_TOLERANCE;
            case Z -> Math.abs(delta.x) > PERPENDICULAR_TOLERANCE
                    || Math.abs(delta.y) > PERPENDICULAR_TOLERANCE;
        };
    }

    private static void keepTargetAway(ServerPlayer player, LivingEntity target) {
        if (target instanceof Mob mob && mob.getTarget() == player) {
            mob.setTarget(null);
        }
        Vec3 difference = target.position().subtract(player.position());
        double distance = difference.length();
        if (distance >= MINIMUM_TARGET_DISTANCE) {
            return;
        }
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        Vec3 direction = distance > EPSILON
                ? difference.scale(1.0D / distance)
                : player.getLookAngle().scale(-1.0D).normalize();
        double correction = Math.min(0.6D, MINIMUM_TARGET_DISTANCE - distance + 0.05D);
        target.move(MoverType.SELF, direction.scale(correction));
        target.push(direction.scale(0.25D));
        target.hurtMarked = true;
    }

    private static void drawPendingAxes(ServerPlayer player, Vec3 origin) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            Vec3 direction = axisVector(axis);
            for (double offset = -PENDING_GUIDE_LENGTH; offset <= PENDING_GUIDE_LENGTH; offset += 0.5D) {
                sendGuideParticle(player, origin.add(direction.scale(offset)).add(0.0D, 0.12D, 0.0D));
            }
        }
    }

    private static void drawSelectedCorridor(
            ServerPlayer player,
            LivingEntity target,
            ActiveEffect effect
    ) {
        Vec3 axis = axisVector(effect.axis);
        Vec3 targetOffset = target.position().subtract(effect.origin);
        double targetProjection = targetOffset.dot(axis);
        double perpendicularDistanceSqr = Math.max(
                0.0D,
                targetOffset.lengthSqr() - targetProjection * targetProjection
        );
        if (perpendicularDistanceSqr > effect.range * effect.range) {
            return;
        }
        double halfLength = Math.sqrt(effect.range * effect.range - perpendicularDistanceSqr);
        double start = targetProjection - halfLength;
        double end = targetProjection + halfLength;
        Vec3 firstPerpendicular = firstPerpendicular(effect.axis);
        Vec3 secondPerpendicular = secondPerpendicular(effect.axis);
        Vec3[] rails = {
                Vec3.ZERO,
                firstPerpendicular.scale(PERPENDICULAR_TOLERANCE),
                firstPerpendicular.scale(-PERPENDICULAR_TOLERANCE),
                secondPerpendicular.scale(PERPENDICULAR_TOLERANCE),
                secondPerpendicular.scale(-PERPENDICULAR_TOLERANCE)
        };
        double length = Math.max(0.0D, end - start);
        int steps = Math.max(1, (int) Math.ceil(length / GUIDE_SPACING));
        for (int step = 0; step <= steps; step++) {
            double distance = start + length * step / steps;
            Vec3 center = effect.origin.add(axis.scale(distance)).add(0.0D, 0.12D, 0.0D);
            for (Vec3 rail : rails) {
                sendGuideParticle(player, center.add(rail));
            }
        }
    }

    private static Vec3 axisVector(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }

    private static Vec3 firstPerpendicular(Direction.Axis axis) {
        return axis == Direction.Axis.X
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
    }

    private static Vec3 secondPerpendicular(Direction.Axis axis) {
        return axis == Direction.Axis.Z
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static void sendGuideParticle(ServerPlayer player, Vec3 position) {
        ((ServerLevel) player.level()).sendParticles(
                player,
                QUARTZ_GUIDE_PARTICLE,
                true,
                position.x,
                position.y,
                position.z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    private static void end(ServerPlayer player, String messageKey) {
        if (removeEffect(player.getUUID()) != null) {
            message(player, messageKey);
        }
    }

    private static void addEffect(UUID ownerId, ActiveEffect effect) {
        removeEffect(ownerId);
        ACTIVE_EFFECTS.put(ownerId, effect);
        EFFECTS_BY_TARGET.computeIfAbsent(new TargetKey(effect.dimension, effect.targetId),
                ignored -> new HashMap<>()).put(ownerId, effect);
    }

    private static ActiveEffect removeEffect(UUID ownerId) {
        ActiveEffect effect = ACTIVE_EFFECTS.remove(ownerId);
        if (effect == null) {
            return null;
        }
        TargetKey key = new TargetKey(effect.dimension, effect.targetId);
        Map<UUID, ActiveEffect> owners = EFFECTS_BY_TARGET.get(key);
        owners.remove(ownerId);
        if (owners.isEmpty()) {
            EFFECTS_BY_TARGET.remove(key);
        }
        return effect;
    }

    private record TargetKey(ResourceKey<Level> dimension, UUID entityId) {
    }

    private static void message(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable(key, arguments), true);
    }

    private static final class ActiveEffect {
        private final ResourceKey<Level> dimension;
        private final UUID targetId;
        private final Vec3 origin;
        private final double range;
        private int remainingTicks;
        private Direction.Axis axis;

        private ActiveEffect(ResourceKey<Level> dimension, UUID targetId, Vec3 origin,
                             double range, int remainingTicks) {
            this.dimension = dimension;
            this.targetId = targetId;
            this.origin = origin;
            this.range = range;
            this.remainingTicks = remainingTicks;
        }
    }
}
