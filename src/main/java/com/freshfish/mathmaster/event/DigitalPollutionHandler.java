package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.entity.EightEntity;
import com.freshfish.mathmaster.entity.NineEntity;
import com.freshfish.mathmaster.entity.SevenEntity;
import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.AxiomEffectManager;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModMobEffects;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.pollution.DigitalPollutionData;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import com.freshfish.mathmaster.item.EquippedDigitalPollutionMeter;
import com.freshfish.mathmaster.network.DigitalPollutionMeterPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DigitalPollutionHandler {
    private final Map<UUID, ClientState> clientStates = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }

        DigitalPollutionData data = player.getData(ModAttachments.DIGITAL_POLLUTION);
        int identityLevel = AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.ADDITIVE_IDENTITY);
        data.tickAdditiveIdentity(identityLevel);
        if (data.getValue() >= DigitalPollutionData.MAX_VALUE) {
            DigitalPollutionManager.add(player, 1);
            return;
        }
        if (Level.OVERWORLD.equals(player.level().dimension())) {
            data.tickInOverworld(identityLevel > 0 ? 1 : 0);
        }
        syncClientStateIfChanged(player, data.getValue());
    }

    @SubscribeEvent
    public void onNonPlayerEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)
                || entity instanceof Player
                || entity.level().isClientSide()
                || !entity.isAlive()
                || EntityIntellectManager.get(entity) == null) {
            return;
        }

        entity.getData(ModAttachments.DIGITAL_POLLUTION).tickNonPlayerDecay(
                entity.hasEffect(ModMobEffects.DIGITAL_POLLUTION)
        );
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clientStates.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !Level.OVERWORLD.equals(level.dimension())) {
            return;
        }

        long skippedTicks = Math.max(0L, event.getNewTime() - level.getDayTime());
        if (skippedTicks == 0L) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (!player.isSleepingLongEnough()) {
                continue;
            }
            int identityLevel = AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.ADDITIVE_IDENTITY);
            DigitalPollutionData data = player.getData(ModAttachments.DIGITAL_POLLUTION);
            data.advanceSleepTime(skippedTicks, identityLevel > 0 ? 1 : 0);
            syncClientStateIfChanged(player, data.getValue());
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getEntity() instanceof EightEntity) {
            DigitalPollutionManager.tryAddFromKill(player, true);
        } else if (event.getEntity() instanceof NineEntity && !(event.getEntity() instanceof SevenEntity)) {
            DigitalPollutionManager.tryAddFromKill(player, false);
        }
    }

    private void syncClientStateIfChanged(ServerPlayer player, int pollution) {
        boolean equipped = EquippedDigitalPollutionMeter.isEquipped(player);
        ClientState current = new ClientState(equipped, pollution);
        ClientState previous = clientStates.put(player.getUUID(), current);
        if (!current.equals(previous)) {
            PacketDistributor.sendToPlayer(
                    player,
                    new DigitalPollutionMeterPayload(equipped, pollution)
            );
        }
    }

    private record ClientState(boolean meterEquipped, int pollution) {
    }
}
