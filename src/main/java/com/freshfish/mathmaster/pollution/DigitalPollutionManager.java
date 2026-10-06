package com.freshfish.mathmaster.pollution;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.AxiomEffectManager;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public final class DigitalPollutionManager {
    private DigitalPollutionManager() {}

    public static void add(LivingEntity entity, int amount) {
        if (amount <= 0 || entity.level().isClientSide()) {
            return;
        }

        DigitalPollutionData data = entity.getData(ModAttachments.DIGITAL_POLLUTION);
        if (data.getValue() < DigitalPollutionData.MAX_VALUE && entity instanceof ServerPlayer player) {
            int identityLevel = AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.ADDITIVE_IDENTITY);
            if (data.consumeIdentityShield(identityLevel)) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.mathmaster.axiom.identity.blocked"),
                        true
                );
                return;
            }
        }
        data.add(amount);
        if (data.getValue() >= DigitalPollutionData.MAX_VALUE) {
            // Vanilla blocks even genericKill until the client acknowledges dimension travel.
            // Keep the persisted threshold; DigitalPollutionHandler retries it after protection ends.
            if (entity instanceof ServerPlayer player && player.isChangingDimension()) {
                return;
            }
            data.reset();
            var genericKill = entity.damageSources().genericKill();
            var lethalDamage = entity instanceof ServerPlayer
                    ? new DigitalPollutionDamageSource(
                            genericKill.typeHolder(),
                            entity.getRandom().nextInt(DigitalPollutionDamageSource.MESSAGE_VARIANT_COUNT)
                    )
                    : genericKill;
            boolean wasAlive = entity.isAlive();
            entity.hurt(lethalDamage, Float.MAX_VALUE);
            // Vanilla die sets DYING only after the cancellable death event has passed.
            if (wasAlive && !entity.isAlive() && entity.getPose() == Pose.DYING) {
                PollutionAdvancements.awardDeath(entity);
            }
            if (!(entity instanceof ServerPlayer) && entity.level() instanceof ServerLevel level) {
                var nine = ModEntities.NINE.get().create(level);
                if (nine != null) {
                    nine.moveTo(
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            entity.getYRot(),
                            entity.getXRot()
                    );
                    level.addFreshEntity(nine);
                }
            }
        }
    }

    public static void reduce(ServerPlayer player, int amount) {
        int minimum = AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.ADDITIVE_IDENTITY) > 0 ? 1 : 0;
        player.getData(ModAttachments.DIGITAL_POLLUTION).reduce(amount, minimum);
    }

    public static void tryAddFromKill(ServerPlayer player, boolean isEight) {
        float chance = isEight ? 0.02F : 0.01F;
        if (player.getRandom().nextFloat() < chance) {
            add(player, 1);
        }
    }

    public static void tryAddFromAttack(ServerPlayer player, boolean isEight) {
        tryAddFromAttack(player, isEight ? 8 : 9);
    }

    public static void tryAddFromAttack(ServerPlayer player, int digitalNumber) {
        float chance = switch (digitalNumber) {
            case 9 -> 0.10F;
            case 8 -> 0.15F;
            case 7 -> 0.20F;
            case 6 -> 0.25F;
            default -> 0.0F;
        };
        if (chance == 0.0F) {
            return;
        }
        if (player.getRandom().nextFloat() < chance) {
            add(player, 10 - digitalNumber);
        }
    }
}
