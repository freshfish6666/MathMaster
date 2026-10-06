package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModMobEffects;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;

public final class EuclidPrimeInfinitySkill {
    public static void use(ServerPlayer player, int level) {
        AxiomSkillData skillData = player.getData(ModAttachments.AXIOM_SKILL_DATA);
        if (skillData.getEuclidPrimeInfinityCooldownTicks() > 0) {
            int seconds = (skillData.getEuclidPrimeInfinityCooldownTicks() + 19) / 20;
            message(player, "message.mathmaster.axiom.prime.cooldown", seconds);
            return;
        }

        double radius = 8.0D + 2.0D * level;
        int duration = (10 + 10 * level) * 20;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive()
                        && !(entity instanceof Player)
                        && EntityIntellectManager.get(entity) != null
                        && entity.distanceToSqr(player) <= radius * radius
        );
        for (LivingEntity target : targets) {
            applyMark(target, 2, duration);
        }

        skillData.setEuclidPrimeInfinityCooldownTicks((120 - 20 * level) * 20);
        DigitalPollutionManager.add(player, 4 * level);
        message(player, "message.mathmaster.axiom.prime.success", targets.size(), (int) radius, duration / 20);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMarkedEntityKilled(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        MobEffectInstance mark = dead.getEffect(ModMobEffects.PRIME_MARK);
        if (mark == null
                || mark.getDuration() <= 0
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || dead.getKillCredit() != player
                || !(dead.level() instanceof ServerLevel level)) {
            return;
        }

        Entity created = dead.getType().create(level);
        if (!(created instanceof LivingEntity replacement)) {
            return;
        }
        replacement.moveTo(dead.getX(), dead.getY(), dead.getZ(), dead.getYRot(), dead.getXRot());
        if (replacement instanceof Mob mob) {
            EventHooks.finalizeMobSpawn(
                    mob,
                    level,
                    level.getCurrentDifficultyAt(mob.blockPosition()),
                    MobSpawnType.TRIGGERED,
                    null
            );
        }
        int currentPrime = dead.getData(ModAttachments.PRIME_MARK).getPrime();
        applyMark(replacement, nextPrime(currentPrime), mark.getDuration());
        level.addFreshEntityWithPassengers(replacement);
        replacement.syncData(ModAttachments.PRIME_MARK);
    }

    private static void applyMark(LivingEntity entity, int prime, int duration) {
        entity.getData(ModAttachments.PRIME_MARK).setMark(prime, entity.level().getGameTime() + duration);
        entity.addEffect(new MobEffectInstance(
                ModMobEffects.PRIME_MARK,
                duration,
                0,
                false,
                false,
                false
        ));
        if (!entity.level().isClientSide()) {
            entity.syncData(ModAttachments.PRIME_MARK);
        }
    }

    static int nextPrime(int current) {
        if (current >= Integer.MAX_VALUE - 1) {
            return Integer.MAX_VALUE;
        }
        int candidate = Math.max(2, current + 1);
        while (candidate < Integer.MAX_VALUE && !isPrime(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private static boolean isPrime(int value) {
        if (value < 2) {
            return false;
        }
        if (value == 2) {
            return true;
        }
        if ((value & 1) == 0) {
            return false;
        }
        for (int divisor = 3; (long) divisor * divisor <= value; divisor += 2) {
            if (value % divisor == 0) {
                return false;
            }
        }
        return true;
    }

    private static void message(ServerPlayer player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable(key, arguments), true);
    }
}
