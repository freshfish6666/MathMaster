package com.freshfish.mathmaster.axiom;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Resolves blink landings and sweeps the player's entire body during continuous flight. */
public final class GeodesicMovement {
    private static final double EPSILON = 1.e-6;

    private GeodesicMovement() {}

    /** One physics step shared by client flight and the isolated movement checks. */
    public static Result flyStep(Player player, int level) {
        Result result = sweep(player, player.getLookAngle().scale(GeodesicSkill.speedPerTick(level)));
        player.setNoGravity(true);
        player.resetFallDistance();
        player.setDeltaMovement(result.movement());
        player.move(MoverType.SELF, result.movement());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        return result;
    }

    /** Blink follows the eye's aim, then fits the feet to a safe landing position. */
    public static Optional<Vec3> teleportDestination(Player player, double range) {
        Vec3 origin = player.position();
        Vec3 eye = player.getEyePosition();
        var hit = player.level().clip(new ClipContext(eye,
                eye.add(player.getLookAngle().scale(range + player.getEyeHeight())),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        List<Vec3> candidates = new ArrayList<>();
        if (hit.getType() == HitResult.Type.BLOCK) {
            var block = hit.getBlockPos();
            var shape = player.level().getBlockState(block).getCollisionShape(player.level(), block);
            if (hit.getDirection() == Direction.UP) {
                candidates.add(new Vec3(block.getX() + .5, hit.getLocation().y + 1.e-4, block.getZ() + .5));
                candidates.add(hit.getLocation().add(0, 1.e-4, 0));
            } else if (!shape.isEmpty()) {
                double top = block.getY() + shape.bounds().maxY;
                // A visible low ledge is a destination, rather than an obstacle at the player's knees.
                if (top - origin.y <= 2.01) {
                    candidates.add(new Vec3(block.getX() + .5, top + 1.e-4, block.getZ() + .5));
                }
            }
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 front = hit.getLocation().add(normal.scale(player.getBbWidth() / 2 + .01));
            var floor = player.level().clip(new ClipContext(front,
                    front.add(0, -player.getEyeHeight() - 1, 0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player));
            if (floor.getType() == HitResult.Type.BLOCK && floor.getDirection() == Direction.UP) {
                candidates.add(floor.getLocation().add(0, 1.e-4, 0));
            }
            candidates.add(front.subtract(0, player.getEyeHeight(), 0));
        } else {
            candidates.add(origin.add(player.getLookAngle().scale(range)));
        }
        for (Vec3 candidate : candidates) {
            Vec3 desired = candidate.subtract(origin);
            if (desired.length() > range) desired = desired.normalize().scale(range);
            desired = limitByLivingEntities(player, desired);
            Vec3 landing = origin.add(desired);
            if (desired.lengthSqr() > .01 && safeLanding(player, landing)) return Optional.of(landing);
        }
        // Back off toward the caster if the exact impact has too little headroom; never search through a wall.
        Vec3 desired = candidates.getLast().subtract(origin);
        double distance = Math.min(range, desired.length());
        Vec3 direction = desired.normalize();
        for (double step = distance; step >= .25; step -= .25) {
            Vec3 landing = origin.add(limitByLivingEntities(player, direction.scale(step)));
            if (landing.distanceToSqr(origin) > .01 && safeLanding(player, landing)) return Optional.of(landing);
        }
        return Optional.empty();
    }

    private static Vec3 limitByLivingEntities(Player player, Vec3 desired) {
        AABB body = player.getBoundingBox();
        double fraction = 1;
        for (var entity : player.level().getEntities(player, body.expandTowards(desired).inflate(EPSILON),
                entity -> entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator())) {
            fraction = Math.min(fraction, hit(body.getCenter(), desired, entity.getBoundingBox().inflate(
                    body.getXsize() / 2, body.getYsize() / 2, body.getZsize() / 2)));
        }
        if (fraction < 1) fraction = Math.max(0, fraction - EPSILON / Math.max(EPSILON, desired.length()));
        return desired.scale(fraction);
    }

    private static boolean safeLanding(Player player, Vec3 destination) {
        AABB body = player.getBoundingBox().move(destination.subtract(player.position())).deflate(EPSILON);
        if (body.minY < player.level().getMinBuildHeight() || body.maxY > player.level().getMaxBuildHeight()
                || !player.level().getWorldBorder().isWithinBounds(body)) return false;
        for (int x = Mth.floor(body.minX) >> 4; x <= Mth.floor(body.maxX) >> 4; x++) {
            for (int z = Mth.floor(body.minZ) >> 4; z <= Mth.floor(body.maxZ) >> 4; z++) {
                if (!player.level().hasChunk(x, z)) return false;
            }
        }
        if (!player.level().noCollision(player, body)) return false;
        if (player.level().clip(new ClipContext(player.getEyePosition(),
                destination.add(0, player.getEyeHeight(), 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) return false;
        return player.level().getEntities(player, body, entity -> entity instanceof LivingEntity living
                && living.isAlive() && !living.isSpectator()).isEmpty();
    }

    public static Result sweep(Player player, Vec3 desired) {
        if (!Double.isFinite(desired.lengthSqr())) {
            return new Result(Vec3.ZERO, true);
        }
        if (desired.lengthSqr() < EPSILON * EPSILON) {
            return new Result(Vec3.ZERO, false);
        }
        AABB body = player.getBoundingBox();
        AABB path = body.expandTowards(desired).inflate(EPSILON);
        Vec3 center = body.getCenter();
        double halfX = body.getXsize() / 2;
        double halfY = body.getYsize() / 2;
        double halfZ = body.getZsize() / 2;
        double fraction = 1;
        for (var shape : player.level().getBlockCollisions(player, path)) {
            for (AABB obstacle : shape.toAabbs()) {
                fraction = Math.min(fraction, hit(center, desired, obstacle.inflate(halfX, halfY, halfZ)));
            }
        }
        for (var entity : player.level().getEntities(player, path,
                entity -> entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator())) {
            fraction = Math.min(fraction,
                    hit(center, desired, entity.getBoundingBox().inflate(halfX, halfY, halfZ)));
        }

        // An unloaded chunk and the world limits are obstacles, never a reason to load terrain mid-flight.
        for (int x = Mth.floor(path.minX) >> 4; x <= Mth.floor(path.maxX) >> 4; x++) {
            for (int z = Mth.floor(path.minZ) >> 4; z <= Mth.floor(path.maxZ) >> 4; z++) {
                if (!player.level().hasChunk(x, z)) {
                    AABB chunk = new AABB(x * 16, -3.e7, z * 16, x * 16 + 16, 3.e7, z * 16 + 16);
                    fraction = Math.min(fraction, hit(center, desired, chunk.inflate(halfX, halfY, halfZ)));
                }
            }
        }
        var border = player.level().getWorldBorder();
        fraction = Math.min(fraction, boundary(body.minX, desired.x, border.getMinX(), false));
        fraction = Math.min(fraction, boundary(body.maxX, desired.x, border.getMaxX(), true));
        fraction = Math.min(fraction, boundary(body.minZ, desired.z, border.getMinZ(), false));
        fraction = Math.min(fraction, boundary(body.maxZ, desired.z, border.getMaxZ(), true));
        fraction = Math.min(fraction, boundary(body.minY, desired.y, player.level().getMinBuildHeight(), false));
        fraction = Math.min(fraction, boundary(body.maxY, desired.y, player.level().getMaxBuildHeight(), true));
        boolean blocked = fraction < 1;
        if (blocked) {
            fraction = Math.max(0, fraction - EPSILON / desired.length());
        }
        return new Result(desired.scale(fraction), blocked);
    }

    private static double boundary(double start, double delta, double limit, boolean upper) {
        if ((upper && delta > 0 && start + delta >= limit)
                || (!upper && delta < 0 && start + delta <= limit)) {
            return Mth.clamp((limit - start) / delta, 0, 1);
        }
        return 1;
    }

    private static double hit(Vec3 start, Vec3 delta, AABB expanded) {
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            double origin = axis == 0 ? start.x : axis == 1 ? start.y : start.z;
            double direction = axis == 0 ? delta.x : axis == 1 ? delta.y : delta.z;
            double min = axis == 0 ? expanded.minX : axis == 1 ? expanded.minY : expanded.minZ;
            double max = axis == 0 ? expanded.maxX : axis == 1 ? expanded.maxY : expanded.maxZ;
            if (Math.abs(direction) < EPSILON) {
                // Moving parallel to a touching floor or wall must remain possible.
                if (origin <= min + EPSILON || origin >= max - EPSILON) return 1;
                continue;
            }
            double a = (min - origin) / direction;
            double b = (max - origin) / direction;
            enter = Math.max(enter, Math.min(a, b));
            exit = Math.min(exit, Math.max(a, b));
            if (enter > exit) return 1;
        }
        if (exit <= EPSILON || enter >= 1 || enter > exit) return 1;
        return Math.max(0, enter);
    }

    public record Result(Vec3 movement, boolean blocked) {}
}
