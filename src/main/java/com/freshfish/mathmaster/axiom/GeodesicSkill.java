package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.network.GeodesicStatePayload;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The server owns activation, cost, cooldown, collision checks and cleanup. */
public final class GeodesicSkill {
    public static final int HOLD_TICKS = 6;
    private static final int INPUT_TIMEOUT_TICKS = 30;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static void press(ServerPlayer player, int level) {
        if (SESSIONS.containsKey(player.getUUID()) || !AxiomDefinition.GEODESIC.acceptsNoteLevel(level)) return;
        if (!canUse(player) || AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.GEODESIC) != level) return;
        int cooldown = player.getData(ModAttachments.AXIOM_SKILL_DATA).getGeodesicCooldownTicks();
        if (AxiomCooldownHandler.applyRingBypass(player, cooldown) > 0) {
            message(player, "message.mathmaster.axiom.geodesic.cooldown", (cooldown + 19) / 20);
            return;
        }
        SESSIONS.put(player.getUUID(), new Session(player, level));
    }

    public static void heartbeat(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null) session.lastInputTick = player.level().getGameTime();
    }

    public static void release(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        if (!valid(player, session)) {
            cancel(player);
        } else if (session.flying) {
            cancel(player);
        } else {
            SESSIONS.remove(player.getUUID());
            dash(player, session.level);
        }
    }

    private static void dash(ServerPlayer player, int level) {
        var landing = GeodesicMovement.teleportDestination(player, 2 * level);
        if (landing.isEmpty()) {
            message(player, "message.mathmaster.axiom.geodesic.blocked");
            return;
        }
        setCooldown(player, shortCooldownTicks(level));
        DigitalPollutionManager.add(player, level);
        if (!player.isAlive()) return;
        Vec3 destination = landing.get();
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.connection.teleport(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        PacketDistributor.sendToPlayer(player, new GeodesicStatePayload(0, player.isNoGravity()));
        message(player, "message.mathmaster.axiom.geodesic.dash");
    }

    public static boolean isFlying(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.flying;
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        if (!valid(player, session) || player.level().getGameTime() - session.lastInputTick > INPUT_TIMEOUT_TICKS) {
            cancel(player);
            return;
        }
        if (!session.flying) {
            if (++session.heldTicks < HOLD_TICKS) return;
            session.flying = true;
            session.originalNoGravity = player.isNoGravity();
            session.movementCredit = 4 * speedPerTick(session.level);
            session.lastMoveTick = player.level().getGameTime();
            player.setNoGravity(true);
            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
            session.flightTicks = 1;
            DigitalPollutionManager.add(player, 1);
            if (!player.isAlive() || SESSIONS.get(player.getUUID()) != session) return;
            PacketDistributor.sendToPlayer(player, new GeodesicStatePayload(session.level, session.originalNoGravity));
            message(player, "message.mathmaster.axiom.geodesic.flying");
        } else {
            // Charge each begun second: a brief hold still costs one pollution point.
            if (session.flightTicks % 20 == 0) DigitalPollutionManager.add(player, 1);
            if (!player.isAlive() || SESSIONS.get(player.getUUID()) != session) return;
            session.flightTicks++;
            player.setNoGravity(true);
            player.resetFallDistance();
        }
    }

    /** Called on the main thread before vanilla accepts a flight movement packet. */
    public static boolean validateMovement(ServerPlayer player, Vec3 destination) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.flying) return true;
        if (!valid(player, session)) {
            cancel(player);
            return true;
        }
        Vec3 desired = destination.subtract(player.position());
        double distance = desired.length();
        if (!Double.isFinite(distance)) return true; // Vanilla handles invalid packets and disconnects.
        long now = player.level().getGameTime();
        double speed = speedPerTick(session.level);
        session.movementCredit = Math.min(5 * speed,
                session.movementCredit + Math.max(0, now - session.lastMoveTick) * speed);
        session.lastMoveTick = now;
        player.resetFallDistance();
        if (distance > session.movementCredit + .05) {
            correctAndStop(player, player.position());
            return false;
        }
        session.movementCredit = Math.max(0, session.movementCredit - distance);
        var result = GeodesicMovement.sweep(player, desired);
        if (result.blocked()) {
            correctAndStop(player, player.position().add(result.movement()));
            return false;
        }
        return true;
    }

    private static void correctAndStop(ServerPlayer player, Vec3 destination) {
        cancel(player);
        player.connection.teleport(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        message(player, "message.mathmaster.axiom.geodesic.blocked");
    }

    public static void cancel(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) return;
        if (session.flying) {
            player.setNoGravity(session.originalNoGravity);
            player.resetFallDistance();
            player.setDeltaMovement(Vec3.ZERO);
            setCooldown(player, shortCooldownTicks(session.level) + 100 + 2 * session.flightTicks);
            PacketDistributor.sendToPlayer(player, new GeodesicStatePayload(0, session.originalNoGravity));
        }
    }

    private static boolean canUse(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isSleeping() && !player.isPassenger();
    }

    private static boolean valid(ServerPlayer player, Session session) {
        return canUse(player) && player.level().dimension().equals(session.dimension)
                && AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.GEODESIC) == session.level;
    }

    public static int shortCooldownTicks(int level) { return (32 - 6 * level) * 20; }
    public static double speedPerTick(int level) { return 6.0 * level / 20; }

    private static void setCooldown(ServerPlayer player, int ticks) {
        player.getData(ModAttachments.AXIOM_SKILL_DATA)
                .setGeodesicCooldownTicks(AxiomCooldownHandler.applyRingBypass(player, ticks));
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public void onLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    private static void message(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable(key, arguments), true);
    }

    private static final class Session {
        private final int level;
        private final ResourceKey<Level> dimension;
        private int heldTicks;
        private int flightTicks;
        private long lastInputTick;
        private long lastMoveTick;
        private double movementCredit;
        private boolean flying;
        private boolean originalNoGravity;

        private Session(ServerPlayer player, int level) {
            this.level = level;
            this.dimension = player.level().dimension();
            this.lastInputTick = player.level().getGameTime();
        }
    }
}
