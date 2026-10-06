package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.intellect.EntityIntellectDefinition;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.intellect.InsightTargeting;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdditionCommutativitySkill {
    private static final double RANGE = 16.0D;
    private static final ResourceLocation MAX_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            MathMaster.MODID,
            "addition_commutativity.max_health"
    );
    private static final Map<UUID, MarkedTarget> MARKED_TARGETS = new HashMap<>();
    private static final Map<TargetKey, GlowState> GLOW_STATES = new HashMap<>();

    public static void use(ServerPlayer player, int level) {
        if (player.isShiftKeyDown()) {
            cancel(player, true);
            return;
        }

        AxiomSkillData skillData = player.getData(ModAttachments.AXIOM_SKILL_DATA);
        if (skillData.getAdditionCommutativityCooldownTicks() > 0) {
            int seconds = (skillData.getAdditionCommutativityCooldownTicks() + 19) / 20;
            message(player, "message.mathmaster.axiom.addition.cooldown", seconds);
            return;
        }

        LivingEntity target = findTarget(player);
        if (target == null) {
            message(player, "message.mathmaster.axiom.addition.no_target");
            return;
        }
        if (!isAllowedTarget(player, target)) {
            return;
        }

        MarkedTarget marked = MARKED_TARGETS.get(player.getUUID());
        if (marked == null) {
            mark(player, target);
            return;
        }

        LivingEntity first = resolve(player, marked);
        if (first == null || !first.isAlive() || first.level() != player.level()) {
            release(player, first);
            message(player, "message.mathmaster.axiom.addition.lost");
            return;
        }
        if (first.getUUID().equals(target.getUUID())) {
            message(player, "message.mathmaster.axiom.addition.same_target");
            return;
        }
        if (!isAllowedTarget(player, first)) {
            release(player, first);
            return;
        }

        exchangeHealth(first, target);
        release(player, first);
        int cooldownSeconds = 30 - 5 * level;
        skillData.setAdditionCommutativityCooldownTicks(cooldownSeconds * 20);
        DigitalPollutionManager.add(player, level);
        message(
                player,
                "message.mathmaster.axiom.addition.success.health",
                first.getDisplayName(),
                target.getDisplayName()
        );
    }

    public static void cancel(ServerPlayer player, boolean notify) {
        MarkedTarget marked = MARKED_TARGETS.get(player.getUUID());
        if (marked == null) {
            if (notify) {
                message(player, "message.mathmaster.axiom.addition.nothing_marked");
            }
            return;
        }
        release(player, resolve(player, marked));
        if (notify) {
            message(player, "message.mathmaster.axiom.addition.cancelled");
        }
    }

    private static void mark(ServerPlayer player, LivingEntity target) {
        TargetKey key = new TargetKey(target.level().dimension(), target.getUUID());
        GlowState glow = GLOW_STATES.computeIfAbsent(
                key,
                ignored -> new GlowState(target.isCurrentlyGlowing())
        );
        glow.references++;
        target.setGlowingTag(true);
        MARKED_TARGETS.put(player.getUUID(), new MarkedTarget(key));
        message(player, "message.mathmaster.axiom.addition.marked", target.getDisplayName());
    }

    private static void release(ServerPlayer player, LivingEntity loadedTarget) {
        MarkedTarget marked = MARKED_TARGETS.remove(player.getUUID());
        if (marked == null) {
            return;
        }
        GlowState glow = GLOW_STATES.get(marked.key);
        if (glow == null || --glow.references > 0) {
            return;
        }
        GLOW_STATES.remove(marked.key);
        if (loadedTarget != null) {
            loadedTarget.setGlowingTag(glow.originallyGlowing);
        }
    }

    private static LivingEntity resolve(ServerPlayer player, MarkedTarget marked) {
        ServerLevel level = player.server.getLevel(marked.key.dimension);
        if (level == null) {
            return null;
        }
        Entity entity = level.getEntity(marked.key.entityId);
        return entity instanceof LivingEntity living ? living : null;
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

    private static boolean isAllowedTarget(ServerPlayer player, LivingEntity target) {
        if (!target.isAlive() || target instanceof Player || target.getType() == EntityType.ENDER_DRAGON
                || target.getType() == EntityType.WITHER) {
            message(player, "message.mathmaster.axiom.addition.invalid_target");
            return false;
        }
        EntityIntellectDefinition intellect = EntityIntellectManager.get(target);
        if (intellect == null) {
            message(player, "message.mathmaster.axiom.addition.no_intellect");
            return false;
        }
        if (intellect.intellect() > IntelligenceManager.getEffectiveIq(player)) {
            message(player, "message.mathmaster.axiom.addition.too_intelligent");
            return false;
        }
        return true;
    }

    private static void exchangeHealth(LivingEntity first, LivingEntity second) {
        double firstMaximum = first.getMaxHealth();
        double secondMaximum = second.getMaxHealth();
        float firstCurrent = first.getHealth();
        float secondCurrent = second.getHealth();
        setMaximumHealth(first, secondMaximum);
        setMaximumHealth(second, firstMaximum);
        first.setHealth(Math.min(first.getMaxHealth(), secondCurrent));
        second.setHealth(Math.min(second.getMaxHealth(), firstCurrent));
    }

    private static void setMaximumHealth(LivingEntity entity, double desiredMaximum) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(MAX_HEALTH_MODIFIER_ID);
        double adjustment = Math.max(1.0D, desiredMaximum) - attribute.getValue();
        if (Math.abs(adjustment) > 1.0E-7D) {
            attribute.addPermanentModifier(new AttributeModifier(
                    MAX_HEALTH_MODIFIER_ID,
                    adjustment,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MarkedTarget marked = MARKED_TARGETS.get(player.getUUID());
        if (marked == null) {
            return;
        }
        LivingEntity target = resolve(player, marked);
        ItemStack caseStack = EquippedAxiomCase.find(player).orElse(ItemStack.EMPTY);
        boolean stillEquipped = !caseStack.isEmpty()
                && AxiomCaseItem.getAxioms(caseStack).contains(AxiomDefinition.ADDITION_COMMUTATIVITY);
        if (!stillEquipped || target == null || !target.isAlive() || target.level() != player.level()) {
            release(player, target);
            message(player, "message.mathmaster.axiom.addition.lost");
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancel(player, false);
        }
    }

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        TargetKey key = new TargetKey(event.getLevel().dimension(), event.getEntity().getUUID());
        GlowState glow = GLOW_STATES.remove(key);
        if (glow == null) {
            return;
        }
        List<UUID> owners = MARKED_TARGETS.entrySet().stream()
                .filter(entry -> entry.getValue().key.equals(key))
                .map(Map.Entry::getKey)
                .toList();
        owners.forEach(MARKED_TARGETS::remove);
        event.getEntity().setGlowingTag(glow.originallyGlowing);
        if (event.getLevel() instanceof ServerLevel level) {
            for (UUID owner : owners) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
                if (player != null) {
                    message(player, "message.mathmaster.axiom.addition.lost");
                }
            }
        }
    }

    private static void message(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable(key, arguments), true);
    }

    private record TargetKey(ResourceKey<Level> dimension, UUID entityId) {
    }

    private record MarkedTarget(TargetKey key) {
    }

    private static final class GlowState {
        private final boolean originallyGlowing;
        private int references;

        private GlowState(boolean originallyGlowing) {
            this.originallyGlowing = originallyGlowing;
        }
    }
}
