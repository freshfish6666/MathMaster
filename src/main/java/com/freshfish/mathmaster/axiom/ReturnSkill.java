package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.network.ReturnAnchorsPayload;
import com.freshfish.mathmaster.network.ReturnControlPayload;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/** Server-owned anchor operations, held-input charging and exact-position travel. */
public final class ReturnSkill {
    public static final int TAP_TICKS = 6, CHARGE_TICKS = 40;
    private static final int INPUT_TIMEOUT = 30, LOAD_TIMEOUT = 200;
    private static final TicketType<UUID> LOAD_TICKET = TicketType.create("mathmaster_return", UUID::compareTo);
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, ReturnAnchorsPayload> VIEWERS = new HashMap<>();
    private static final DustParticleOptions PARTICLE = new DustParticleOptions(new Vector3f(.65f, .72f, 1f), 1f);

    public static int cooldownTicks(int level) { return (80 - 10 * level) * 20; }
    public static ReturnAnchorData anchors(ServerPlayer player) { return player.getData(ModAttachments.AXIOM_SKILL_DATA).returnAnchors(); }
    public static boolean isCharging(ServerPlayer player) { return SESSIONS.containsKey(player.getUUID()); }

    public static void press(ServerPlayer player, int level) {
        if (SESSIONS.containsKey(player.getUUID()) || !canUse(player) || !selected(player)
                || !AxiomDefinition.RETURN.acceptsNoteLevel(level)
                || AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.RETURN) != level) return;
        SESSIONS.put(player.getUUID(), new Session(player));
    }

    public static void release(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        boolean tap = session.ticks < TAP_TICKS && valid(player, session);
        cancel(player);
        if (tap) open(player);
    }

    public static void control(ServerPlayer player, int action, int slot) {
        if (action == ReturnControlPayload.HEARTBEAT) {
            Session session = SESSIONS.get(player.getUUID());
            if (session != null) session.lastInput = player.serverLevel().getGameTime();
            return;
        }
        if (action == ReturnControlPayload.CANCEL) { cancel(player); return; }
        if (action == ReturnControlPayload.CLOSE) { VIEWERS.remove(player.getUUID()); return; }
        // A stale screen or forged packet cannot edit hidden slots or supply coordinates.
        if (!VIEWERS.containsKey(player.getUUID()) || !canUse(player) || !selected(player)) return;
        int level = AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.RETURN);
        ReturnAnchorData data = anchors(player);
        data.normalizeSelection(level);
        if (!AxiomDefinition.RETURN.acceptsNoteLevel(level) || slot < 0 || slot >= level) return;
        if (action == ReturnControlPayload.SELECT) {
            data.select(slot, level);
        } else if (slot == data.selected() && action == ReturnControlPayload.RECORD) {
            cancel(player);
            data.set(slot, new ReturnAnchorData.Anchor(player.level().dimension().location(),
                    player.getX(), player.getY(), player.getZ()));
        } else if (slot == data.selected() && action == ReturnControlPayload.DELETE && data.get(slot).isPresent()) {
            cancel(player);
            data.set(slot, null);
        } else return;
        sendSnapshot(player, false);
    }

    private static void open(ServerPlayer player) { sendSnapshot(player, true); }

    private static void sendSnapshot(ServerPlayer player, boolean open) {
        int level = Math.max(0, AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.RETURN));
        var data = anchors(player);
        data.normalizeSelection(level);
        var snapshot = new ReturnAnchorsPayload(open, level, data.selected(), data.visible(level));
        VIEWERS.put(player.getUUID(), new ReturnAnchorsPayload(false, level, data.selected(), snapshot.anchors()));
        PacketDistributor.sendToPlayer(player, snapshot);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        var data = anchors(player);
        if (data.selected() >= 0 || VIEWERS.containsKey(id) || SESSIONS.containsKey(id)) {
            int level = AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.RETURN);
            data.normalizeSelection(level);
            var previous = VIEWERS.get(id);
            if (previous != null) {
                if (!canUse(player) || !selected(player)) {
                    PacketDistributor.sendToPlayer(player, new ReturnAnchorsPayload(false, 0, -1, java.util.List.of()));
                    VIEWERS.remove(id);
                } else if (previous.level() != level || previous.selected() != data.selected()) {
                    sendSnapshot(player, false);
                }
            }
        }
        Session session = SESSIONS.get(id);
        if (session == null) return;
        long now = player.serverLevel().getGameTime();
        if (!valid(player, session) || now - session.lastInput > INPUT_TIMEOUT) { cancel(player); return; }
        session.ticks++;
        if (session.ticks < TAP_TICKS) return;
        if (player.position().distanceToSqr(session.origin) > .01) { interrupted(player); return; }
        if (session.target == null && !prepare(player, session)) return;
        int level = AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.RETURN);
        if (session.slot >= level || data.selected() != session.slot || !data.get(session.slot).filter(session.target::equals).isPresent()) {
            interrupted(player); return;
        }
        if (session.ticks % 2 == 0) particles(player, session.ticks);
        if (session.ticks < CHARGE_TICKS) return;
        if (session.destination.getChunkSource().getChunkNow(session.chunk.x, session.chunk.z) == null) {
            if (session.ticks == CHARGE_TICKS) message(player, "message.mathmaster.axiom.return.loading");
            if (session.ticks >= CHARGE_TICKS + LOAD_TIMEOUT) { message(player, "message.mathmaster.axiom.return.unavailable"); cancel(player); }
            return;
        }
        finish(player, session, level);
    }

    private static boolean prepare(ServerPlayer player, Session session) {
        int cooldown = player.getData(ModAttachments.AXIOM_SKILL_DATA).getReturnCooldownTicks();
        if (AxiomCooldownHandler.applyRingBypass(player, cooldown) > 0) {
            message(player, "message.mathmaster.axiom.return.cooldown", (cooldown + 19) / 20); cancel(player); return false;
        }
        session.slot = anchors(player).selected();
        session.target = anchors(player).get(session.slot).orElse(null);
        if (session.target == null) {
            message(player, "message.mathmaster.axiom.return.empty"); cancel(player); return false;
        }
        session.destination = player.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                session.target.dimension()));
        if (session.destination == null) {
            message(player, "message.mathmaster.axiom.return.unavailable"); cancel(player); return false;
        }
        session.chunk = new ChunkPos(BlockPos.containing(session.target.x(), session.target.y(), session.target.z()));
        // A temporary ticket prepares FULL chunks without blocking the server in getChunkFuture/join.
        session.destination.getChunkSource().addRegionTicket(LOAD_TICKET, session.chunk, 2, player.getUUID());
        return true;
    }

    private static void finish(ServerPlayer player, Session session, int level) {
        var target = session.target;
        SESSIONS.remove(player.getUUID());
        // NeoForge can cancel inter-dimensional travel; only a completed teleport is billed.
        boolean moved = player.teleportTo(session.destination, target.x(), target.y(), target.z(), Set.of(), player.getYRot(), player.getXRot());
        boolean arrived = moved && player.serverLevel() == session.destination
                && player.position().distanceToSqr(new Vec3(target.x(), target.y(), target.z())) < .000001;
        removeTicket(player.getUUID(), session);
        if (!arrived) { message(player, "message.mathmaster.axiom.return.unavailable"); return; }
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.getData(ModAttachments.AXIOM_SKILL_DATA).setReturnCooldownTicks(AxiomCooldownHandler.applyRingBypass(player, cooldownTicks(level)));
        DigitalPollutionManager.add(player, 2 * level);
        if (player.isAlive()) message(player, "message.mathmaster.axiom.return.arrived");
    }

    private static void particles(ServerPlayer player, int ticks) {
        double progress = Math.min(1, ticks / (double) CHARGE_TICKS);
        double radius = 1 - .45 * progress;
        for (int i = 0; i < 6; i++) {
            double angle = ticks * (.16 + progress * .12) + i * Math.PI / 3;
            player.serverLevel().sendParticles(PARTICLE, player.getX() + Math.cos(angle) * radius,
                    player.getY() + .3 + (i % 3) * .55, player.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    public static void cancel(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) removeTicket(player.getUUID(), session);
    }

    private static void removeTicket(UUID id, Session session) {
        if (session.destination != null && session.chunk != null) session.destination.getChunkSource().removeRegionTicket(LOAD_TICKET, session.chunk, 2, id);
    }

    private static void interrupted(ServerPlayer player) { cancel(player); message(player, "message.mathmaster.axiom.return.interrupted"); }
    private static boolean canUse(ServerPlayer player) { return player.isAlive() && !player.isSpectator() && !player.isSleeping() && !player.isPassenger(); }
    private static boolean valid(ServerPlayer player, Session session) {
        return canUse(player) && player.level().dimension().equals(session.dimension) && selected(player)
                && AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.RETURN) >= 2;
    }

    private static boolean selected(ServerPlayer player) {
        return EquippedAxiomCase.find(player).map(stack -> {
            var active = AxiomCaseItem.getAxioms(stack).stream().filter(AxiomDefinition::isActive).toList();
            return AxiomCaseItem.getSelectedAxiom(stack).filter(active::contains)
                    .orElse(active.isEmpty() ? null : active.getFirst()) == AxiomDefinition.RETURN;
        }).orElse(false);
    }

    @SubscribeEvent public void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0) interruptedIfCharging(player);
    }
    private static void interruptedIfCharging(ServerPlayer player) { if (isCharging(player)) interrupted(player); }
    @SubscribeEvent public void onDeath(LivingDeathEvent event) { if (event.getEntity() instanceof ServerPlayer player) clear(player); }
    @SubscribeEvent public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) clear(player); }
    @SubscribeEvent public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) clear(player); }
    @SubscribeEvent public void onLeave(EntityLeaveLevelEvent event) { if (event.getEntity() instanceof ServerPlayer player) clear(player); }
    @SubscribeEvent public void onServerStop(ServerStoppedEvent event) { SESSIONS.clear(); VIEWERS.clear(); }
    private static void clear(ServerPlayer player) { cancel(player); VIEWERS.remove(player.getUUID()); }
    private static void message(ServerPlayer player, String key, Object... arguments) { player.displayClientMessage(Component.translatable(key, arguments), true); }

    private static final class Session {
        private final ResourceKey<Level> dimension;
        private final Vec3 origin;
        private long lastInput;
        private int ticks, slot = -1;
        private ReturnAnchorData.Anchor target;
        private ServerLevel destination;
        private ChunkPos chunk;
        private Session(ServerPlayer player) { dimension = player.level().dimension(); origin = player.position(); lastInput = player.level().getGameTime(); }
    }
}
