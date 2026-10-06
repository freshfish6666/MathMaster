package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModMobEffects;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import com.freshfish.mathmaster.pollution.PollutionExposure;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

public final class PollutionAdvancementsCheck {
    private static int checks;
    public static int run(ServerLevel level) throws Exception {
        checks = 0;
        try (var first = new SkillCheckPlayer(level, "PatientZero")) {
            var player = first.player;
            require(!done(player, "patient_zero"), "initially locked");
            DigitalPollutionManager.add(player, 1);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100));
            require(!done(player, "patient_zero"), "value and unrelated effect do not trigger");
            Consumer<MobEffectEvent.Applicable> deny = event -> {
                if (event.getEntity() == player && event.getEffectInstance().is(ModMobEffects.DIGITAL_POLLUTION))
                    event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            };
            NeoForge.EVENT_BUS.addListener(deny);
            try {
                require(!player.addEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION, 100)), "denied effect");
                require(!done(player, "patient_zero"), "denied effect stays locked");
            } finally { NeoForge.EVENT_BUS.unregister(deny); }
            require(player.addEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION, 100)), "real effect");
            require(done(player, "patient_zero"), "external effect unlocks");
            player.removeEffect(ModMobEffects.DIGITAL_POLLUTION);
            require(done(player, "patient_zero"), "completion persists");
            revoke(player, "patient_zero", "infected");
            PollutionExposure.updateBlockEffect(player, 1);
            require(player.hasEffect(ModMobEffects.DIGITAL_POLLUTION) && done(player, "patient_zero"), "forced environmental effect unlocks");
        }
        var nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        try (var near = new SkillCheckPlayer(level, "PollutionNear");
             var edge = new SkillCheckPlayer(level, "PollutionEdge");
             var far = new SkillCheckPlayer(level, "PollutionFar");
             var other = new SkillCheckPlayer(nether, "PollutionOther")) {
            near.player.setPos(101, 240, 100); edge.player.setPos(132, 240, 100);
            far.player.setPos(132.01, 240, 100); other.player.setPos(100, 240, 100);
            String[] paths = {"high_tower_falls", "last_breath", "iron_to_dust"};
            EntityType<?>[] types = {EntityType.WARDEN, EntityType.VILLAGER, EntityType.IRON_GOLEM};
            for (int index = 0; index < types.length; index++) {
                String path = paths[index];
                var advancement = near.player.server.getAdvancements().get(id(path));
                require(advancement != null && advancement.value().parent().orElseThrow().equals(id("patient_zero")), "parent " + path);
                var victim = (net.minecraft.world.entity.LivingEntity) types[index].create(level);
                victim.setPos(100, 240, 100);
                try {
                    DigitalPollutionManager.add(victim, 99);
                    require(victim.isAlive() && !done(near.player, path), "threshold not reached " + path);
                    Consumer<LivingDeathEvent> cancel = event -> {
                        if (event.getEntity() == victim) event.setCanceled(true);
                    };
                    NeoForge.EVENT_BUS.addListener(cancel);
                    try {
                        DigitalPollutionManager.add(victim, 1);
                        require(victim.getPose() != net.minecraft.world.entity.Pose.DYING && !done(near.player, path), "canceled death " + path);
                    } finally { NeoForge.EVENT_BUS.unregister(cancel); }
                    victim.setHealth(victim.getMaxHealth());
                    victim.invulnerableTime = 0;
                    DigitalPollutionManager.add(victim, 100);
                    require(!victim.isAlive(), "actual pollution death " + path);
                    require(done(near.player, path) && done(edge.player, path), "near and exact 32 " + path);
                    require(!done(far.player, path) && !done(other.player, path), "outside and other dimension " + path);
                    for (String unrelated : paths) if (!unrelated.equals(path))
                        require(!done(near.player, unrelated), "correct species " + path);
                } finally { victim.discard(); }
                revoke(near.player, path, "completed"); revoke(edge.player, path, "completed");
            }
            var ordinary = EntityType.VILLAGER.create(level); ordinary.setPos(100, 240, 100);
            try {
                ordinary.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
                require(!ordinary.isAlive() && !done(near.player, "last_breath"), "ordinary kill excluded");
            } finally { ordinary.discard(); }
            var cow = EntityType.COW.create(level); cow.setPos(100, 240, 100);
            try {
                DigitalPollutionManager.add(cow, 100);
                for (String path : paths) require(!done(near.player, path), "other species excluded");
            } finally { cow.discard(); }
        }
        return checks;
    }
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("mathmaster", "pollution/" + path);
    }
    private static boolean done(ServerPlayer player, String path) {
        var advancement = player.server.getAdvancements().get(id(path));
        if (advancement == null) throw new AssertionError("Missing advancement " + path);
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
    private static void revoke(ServerPlayer player, String path, String criterion) {
        player.getAdvancements().revoke(player.server.getAdvancements().get(id(path)), criterion);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message); checks++;
    }
}
