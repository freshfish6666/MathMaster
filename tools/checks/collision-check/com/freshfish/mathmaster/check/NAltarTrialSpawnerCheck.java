package com.freshfish.mathmaster.check;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;

/** Exercises the exact documented commands through vanilla parsing, codecs and real spawns. */
public final class NAltarTrialSpawnerCheck {
    public static int run(ServerLevel level) {
        int checks = 0;
        var pos = new BlockPos(300, 240, 300);
        var mobs = new ArrayList<Mob>();
        try {
            var file = Path.of("docs/n-altar-trial-spawners.md");
            if (!Files.exists(file)) file = Path.of("../../docs/n-altar-trial-spawners.md");
            var commands = Files.readAllLines(file).stream().filter(s -> s.startsWith("give @p ")).toList();
            require(commands.size() == 4, "four placement commands"); checks++;
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                level.setBlockAndUpdate(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 5; y++) level.setBlockAndUpdate(pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            String[] ids = {"minecraft:zombie", "minecraft:skeleton", "mathmaster:nine", "mathmaster:eight"};
            for (int index = 0; index < commands.size(); index++) {
                var command = commands.get(index);
                var parsed = level.getServer().getCommands().getDispatcher().parse(command, level.getServer().createCommandSourceStack());
                require(parsed.getExceptions().isEmpty() && !parsed.getReader().canRead(), "give syntax: " + ids[index]); checks++;
                int start = command.indexOf("block_entity_data=") + "block_entity_data=".length();
                var tag = TagParser.parseTag(command.substring(start, command.lastIndexOf("] 1")));
                level.setBlockAndUpdate(pos, Blocks.TRIAL_SPAWNER.defaultBlockState());
                var block = (TrialSpawnerBlockEntity) level.getBlockEntity(pos);
                block.loadWithComponents(tag, level.registryAccess());
                var spawner = block.getTrialSpawner();
                for (var config : new net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig[]{spawner.getNormalConfig(), spawner.getOminousConfig()}) {
                    require(config.calculateTargetTotalMobs(0) == 5 && config.calculateTargetTotalMobs(3) == 5, "five mobs independent of player count"); checks++;
                    require(config.calculateTargetSimultaneousMobs(0) == 1 && config.calculateTargetSimultaneousMobs(3) == 1, "one concurrent independent of player count"); checks++;
                    require(config.lootTablesToEject().isEmpty(), "no completion loot in either mode"); checks++;
                    require(config.spawnRange() == 3 && config.ticksBetweenSpawn() == 40, "spawn spacing"); checks++;
                    var entry = config.spawnPotentialsDefinition().getRandomValue(level.random).orElseThrow();
                    require(entry.getEntityToSpawn().getString("id").equals(ids[index]), "correct entity in both modes"); checks++;
                }
                require(spawner.getRequiredPlayerRange() == 14 && spawner.getTargetCooldownLength() == 36000, "activation and cooldown"); checks++;
                var saved = block.saveWithoutMetadata(level.registryAccess());
                block.loadWithComponents(saved, level.registryAccess());
                require(block.getTrialSpawner().getOminousConfig().calculateTargetTotalMobs(3) == 5
                        && block.getTrialSpawner().getOminousConfig().lootTablesToEject().isEmpty(), "structure save/load keeps inherited ominous config"); checks++;
                java.util.Optional<java.util.UUID> spawned = java.util.Optional.empty();
                for (int attempt = 0; attempt < 100 && spawned.isEmpty(); attempt++) spawned = block.getTrialSpawner().spawnMob(level, pos);
                require(spawned.isPresent(), "actual trial spawn without Peano: " + ids[index]); checks++;
                var mob = (Mob) level.getEntity(spawned.orElseThrow()); mobs.add(mob);
                require(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString().equals(ids[index]) && mob.isPersistenceRequired(), "correct persistent spawned mob"); checks++;
                mob.discard();
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            // Open-air high-altitude position: natural generation still respects its light/Peano gate.
            for (var type : java.util.List.of(ModEntities.NINE.get(), ModEntities.EIGHT.get())) {
                require(SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.TRIAL_SPAWNER, pos, level.random), "trial bypasses natural gates"); checks++;
                require(!SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, level.random), "natural gates retained without Peano"); checks++;
            }
            return checks;
        } catch (Exception e) {
            throw new AssertionError("N altar trial spawner commands", e);
        } finally {
            for (var mob : mobs) mob.discard();
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -1; y <= 5; y++)
                level.setBlockAndUpdate(pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("N altar trial spawner: " + message);
    }
}
