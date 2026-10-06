package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.intellect.EntityIntellectDefinition;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.intellect.InsightTargeting;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AdditiveInverseSkill {
    private static final double RANGE = 16.0D;
    private static final double MAX_THREAD_DISTANCE = 48.0D;
    private static final DustParticleOptions THREAD_PARTICLE = new DustParticleOptions(
            new Vector3f(0.72F, 0.46F, 0.90F),
            0.55F
    );
    private static final Map<UUID, UUID> PENDING_SELECTIONS = new HashMap<>();

    public static void use(ServerPlayer player, int level) {
        if (player.isShiftKeyDown()) {
            cancel(player, true);
            return;
        }
        AxiomSkillData skillData = player.getData(ModAttachments.AXIOM_SKILL_DATA);
        if (skillData.getAdditiveInverseCooldownTicks() > 0) {
            int seconds = (skillData.getAdditiveInverseCooldownTicks() + 19) / 20;
            message(player, "message.mathmaster.axiom.inverse.cooldown", seconds);
            return;
        }

        LivingEntity target = findTarget(player);
        if (target == null) {
            message(player, "message.mathmaster.axiom.inverse.no_target");
            return;
        }
        if (!isAllowedTarget(player, target, level)) {
            return;
        }

        InverseLinkSavedData links = InverseLinkSavedData.get(player.server);
        if (links.isLinked(target.getUUID())) {
            message(player, "message.mathmaster.axiom.inverse.already_linked");
            return;
        }

        UUID selectedId = PENDING_SELECTIONS.get(player.getUUID());
        if (selectedId == null) {
            PENDING_SELECTIONS.put(player.getUUID(), target.getUUID());
            message(player, "message.mathmaster.axiom.inverse.marked", target.getDisplayName());
            return;
        }

        LivingEntity first = findLoadedEntity(player.server, selectedId);
        if (first == null || !first.isAlive() || first.level() != player.level()) {
            PENDING_SELECTIONS.remove(player.getUUID());
            message(player, "message.mathmaster.axiom.inverse.lost");
            return;
        }
        if (first == target) {
            message(player, "message.mathmaster.axiom.inverse.same_target");
            return;
        }
        if (first.getType() != target.getType()) {
            message(player, "message.mathmaster.axiom.inverse.different_type");
            return;
        }
        if (!isAllowedTarget(player, first, level) || links.isLinked(first.getUUID())) {
            PENDING_SELECTIONS.remove(player.getUUID());
            message(player, "message.mathmaster.axiom.inverse.lost");
            return;
        }
        if (!links.link(first.getUUID(), target.getUUID())) {
            message(player, "message.mathmaster.axiom.inverse.already_linked");
            return;
        }

        makePersistent(first);
        makePersistent(target);
        beginHostility(first, target);
        PENDING_SELECTIONS.remove(player.getUUID());
        skillData.setAdditiveInverseCooldownTicks((90 - 10 * level) * 20);
        DigitalPollutionManager.add(player, 2 * level);
        message(
                player,
                "message.mathmaster.axiom.inverse.success",
                first.getDisplayName(),
                target.getDisplayName()
        );
    }

    public static void cancel(ServerPlayer player, boolean notify) {
        if (PENDING_SELECTIONS.remove(player.getUUID()) != null) {
            if (notify) {
                message(player, "message.mathmaster.axiom.inverse.cancelled");
            }
        } else if (notify) {
            message(player, "message.mathmaster.axiom.inverse.nothing_marked");
        }
    }

    private static boolean isAllowedTarget(ServerPlayer player, LivingEntity target, int level) {
        if (!target.isAlive() || target instanceof Player) {
            message(player, "message.mathmaster.axiom.inverse.invalid_target");
            return false;
        }
        EntityIntellectDefinition intellect = EntityIntellectManager.get(target);
        if (intellect == null) {
            message(player, "message.mathmaster.axiom.inverse.no_intellect");
            return false;
        }
        int maximum = IntelligenceManager.getEffectiveIq(player) + 10 * level - 20;
        if (intellect.intellect() > maximum) {
            message(player, "message.mathmaster.axiom.inverse.too_intelligent", maximum);
            return false;
        }
        return true;
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        HitResult result = ProjectileUtil.getHitResultOnViewVector(
                player,
                entity -> InsightTargeting.resolveLivingTarget(entity) != null && entity.isPickable(),
                RANGE
        );
        if (result instanceof EntityHitResult entityHit) {
            LivingEntity target = InsightTargeting.resolveLivingTarget(entityHit.getEntity());
            return target == player ? null : target;
        }
        return null;
    }

    private static LivingEntity findLoadedEntity(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static void makePersistent(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
    }

    private static void beginHostility(LivingEntity first, LivingEntity second) {
        if (!(first instanceof Enemy) && !(second instanceof Enemy)) {
            return;
        }
        if (first instanceof Mob firstMob) {
            firstMob.setTarget(second);
        }
        if (second instanceof Mob secondMob) {
            secondMob.setTarget(first);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        InverseLinkSavedData links = InverseLinkSavedData.get(level.getServer());
        UUID partnerId = links.breakLink(event.getEntity().getUUID());
        if (partnerId == null) {
            return;
        }
        LivingEntity partner = findLoadedEntity(level.getServer(), partnerId);
        if (partner == null) {
            links.addPendingDeath(partnerId);
            return;
        }
        killAsInverse(partner);
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        InverseLinkSavedData links = InverseLinkSavedData.get(level.getServer());
        if (links.consumePendingDeath(living.getUUID())) {
            killAsInverse(living);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 10 != 0) {
            return;
        }
        for (InverseLinkSavedData.InversePair pair : InverseLinkSavedData.get(event.getServer()).links()) {
            LivingEntity first = findLoadedEntity(event.getServer(), pair.first());
            LivingEntity second = findLoadedEntity(event.getServer(), pair.second());
            if (first == null || second == null || !first.isAlive() || !second.isAlive()) {
                continue;
            }
            beginHostility(first, second);
            if (first.level() == second.level() && first.distanceToSqr(second) <= MAX_THREAD_DISTANCE * MAX_THREAD_DISTANCE) {
                drawThread((ServerLevel) first.level(), first, second);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !PENDING_SELECTIONS.containsKey(player.getUUID())) {
            return;
        }
        LivingEntity selected = findLoadedEntity(player.server, PENDING_SELECTIONS.get(player.getUUID()));
        ItemStack caseStack = EquippedAxiomCase.find(player).orElse(ItemStack.EMPTY);
        boolean stillEquipped = !caseStack.isEmpty()
                && AxiomCaseItem.getAxioms(caseStack).contains(AxiomDefinition.ADDITIVE_INVERSE);
        if (!stillEquipped || selected == null || !selected.isAlive() || selected.level() != player.level()) {
            PENDING_SELECTIONS.remove(player.getUUID());
            message(player, "message.mathmaster.axiom.inverse.lost");
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancel(player, false);
        }
    }

    private static void killAsInverse(LivingEntity entity) {
        entity.hurt(entity.damageSources().genericKill(), Float.MAX_VALUE);
        if (entity.isAlive()) {
            entity.kill();
        }
    }

    private static void drawThread(ServerLevel level, LivingEntity first, LivingEntity second) {
        Vec3 start = first.getEyePosition().add(0.0D, -0.2D, 0.0D);
        Vec3 end = second.getEyePosition().add(0.0D, -0.2D, 0.0D);
        double arcHeight = Math.min(2.25D, 0.45D + start.distanceTo(end) * 0.05D);
        for (int index = 1; index < 12; index++) {
            double t = index / 12.0D;
            Vec3 point = start.lerp(end, t).add(0.0D, 4.0D * t * (1.0D - t) * arcHeight, 0.0D);
            level.sendParticles(THREAD_PARTICLE, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void message(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable(key, arguments), true);
    }
}
