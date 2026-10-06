package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.intellect.EntityIntellectDefinition;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.intellect.InsightTargeting;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.network.InvolutionStatePayload;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative visual exchange for the logic-category Involution axiom. */
public final class InvolutionSkill {
    public static final int DURATION_TICKS = 30 * 20;
    public static final int COOLDOWN_TICKS = 100 * 20;
    public static final int POLLUTION_COST = 20;
    public static final double MAXIMUM_DISTANCE = 32.0D;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static void use(ServerPlayer player, int level) {
        if (level < 5) {
            message(player, "message.mathmaster.axiom.involution.level");
            return;
        }
        if (SESSIONS.containsKey(player.getUUID())) {
            message(player, "message.mathmaster.axiom.involution.already_active");
            return;
        }
        AxiomSkillData data = player.getData(ModAttachments.AXIOM_SKILL_DATA);
        if (data.getInvolutionCooldownTicks() > 0) {
            message(player, "message.mathmaster.axiom.involution.cooldown",
                    (data.getInvolutionCooldownTicks() + 19) / 20);
            return;
        }

        if (!(InsightTargeting.findTarget(player) instanceof Mob target)) {
            message(player, "message.mathmaster.axiom.involution.no_target");
            return;
        }
        useSelectedTarget(player, target);
    }

    static void useSelectedTarget(ServerPlayer player, Mob target) {
        EntityIntellectDefinition intellect = EntityIntellectManager.get(target);
        if (intellect == null) {
            message(player, "message.mathmaster.axiom.involution.no_intellect");
            return;
        }
        if (intellect.intellect() >= IntelligenceManager.getEffectiveIq(player)) {
            message(player, "message.mathmaster.axiom.involution.too_smart");
            return;
        }
        if (target.getType().is(Tags.EntityTypes.BOSSES)) {
            message(player, "message.mathmaster.axiom.involution.boss");
            return;
        }
        if (SESSIONS.values().stream().anyMatch(session -> session.targetId.equals(target.getUUID()))) {
            message(player, "message.mathmaster.axiom.involution.occupied");
            return;
        }

        Session session = new Session(player.level().dimension(), target.getUUID());
        SESSIONS.put(player.getUUID(), session);
        player.getData(ModAttachments.AXIOM_SKILL_DATA).setInvolutionCooldownTicks(COOLDOWN_TICKS);
        DigitalPollutionManager.add(player, POLLUTION_COST);
        // Pollution can synchronously kill the player and finish this session through the death event.
        if (!player.isAlive() || SESSIONS.get(player.getUUID()) != session) {
            return;
        }
        broadcastStart(player, target);
        message(player, "message.mathmaster.axiom.involution.started", target.getDisplayName());
    }

    public static boolean isActive(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        Mob target = resolveTarget(player, session);
        if (target == null || !player.level().dimension().equals(session.dimension)) {
            finish(player, "message.mathmaster.axiom.involution.lost");
        } else if (!player.isAlive()) {
            finish(player, "message.mathmaster.axiom.involution.body_died");
        } else if (!target.isAlive()) {
            finish(player, "message.mathmaster.axiom.involution.target_died");
        } else if (player.distanceToSqr(target) > MAXIMUM_DISTANCE * MAXIMUM_DISTANCE) {
            finish(player, "message.mathmaster.axiom.involution.too_far");
        } else if (--session.remainingTicks <= 0) {
            finish(player, "message.mathmaster.axiom.involution.expired");
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isActive(player)) {
            finish(player, "message.mathmaster.axiom.involution.body_died");
            return;
        }
        for (var entry : new ArrayList<>(SESSIONS.entrySet())) {
            if (entry.getValue().targetId.equals(event.getEntity().getUUID())) {
                ServerPlayer owner = event.getEntity().getServer().getPlayerList().getPlayer(entry.getKey());
                if (owner != null) {
                    finish(owner, "message.mathmaster.axiom.involution.target_died");
                }
            }
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            finish(player, null);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer viewer) {
            synchronizeActiveSessions(viewer);
        }
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer viewer) {
            synchronizeActiveSessions(viewer);
        }
    }

    private static void synchronizeActiveSessions(ServerPlayer viewer) {
        for (var entry : SESSIONS.entrySet()) {
            ServerPlayer owner = viewer.getServer().getPlayerList().getPlayer(entry.getKey());
            if (owner == null || !owner.level().dimension().equals(viewer.level().dimension())) {
                continue;
            }
            Mob target = resolveTarget(owner, entry.getValue());
            if (target != null) {
                PacketDistributor.sendToPlayer(viewer,
                        new InvolutionStatePayload(true, owner.getId(), target.getId()));
            }
        }
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        for (var entry : new ArrayList<>(SESSIONS.entrySet())) {
            if (entry.getValue().targetId.equals(event.getEntity().getUUID())) {
                ServerPlayer owner = event.getEntity().getServer().getPlayerList().getPlayer(entry.getKey());
                if (owner != null) {
                    finish(owner, "message.mathmaster.axiom.involution.lost");
                }
            }
        }
    }

    private static Mob resolveTarget(ServerPlayer player, Session session) {
        ServerLevel targetLevel = player.getServer().getLevel(session.dimension);
        if (targetLevel == null) {
            return null;
        }
        Entity entity = targetLevel.getEntity(session.targetId);
        return entity instanceof Mob mob ? mob : null;
    }

    static void finish(ServerPlayer player, String messageKey) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        InvolutionStatePayload payload = new InvolutionStatePayload(false, player.getId(), -1);
        ServerLevel originalLevel = player.getServer().getLevel(session.dimension);
        if (originalLevel != null) {
            PacketDistributor.sendToPlayersInDimension(originalLevel, payload);
        }
        if (player.level() != originalLevel) {
            PacketDistributor.sendToPlayer(player, payload);
        }
        if (messageKey != null) {
            message(player, messageKey);
        }
    }

    private static void broadcastStart(ServerPlayer player, Mob target) {
        PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(),
                new InvolutionStatePayload(true, player.getId(), target.getId()));
    }

    private static void message(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable(key, arguments), true);
    }

    private static final class Session {
        private final ResourceKey<Level> dimension;
        private final UUID targetId;
        private int remainingTicks = DURATION_TICKS;

        private Session(ResourceKey<Level> dimension, UUID targetId) {
            this.dimension = dimension;
            this.targetId = targetId;
        }
    }
}
