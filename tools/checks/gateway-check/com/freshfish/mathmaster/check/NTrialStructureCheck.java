package com.freshfish.mathmaster.check;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public final class NTrialStructureCheck {
    public static String run(ServerLevel level) {
        checkSupplySamples(level);
        var id = ResourceLocation.parse("mathmaster:trials/n_trial");
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(id).orElseThrow();
        require(holder.value().biomes().size() == 17, "seventeen trial biomes");
        var gateway = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.parse("mathmaster:ruins/unfinished_overworld_gateway"));
        require(gateway.biomes().size() == 21, "twenty-one gateway biomes");
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(ResourceLocation.parse("mathmaster:n_trials"));
        var spread = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement)set.placement();
        require(spread.spacing() == 48 && spread.separation() == 18, "48 region scheme");
        int passed = 0;
        for (int x = -32; x < 32; x++) for (int z = -32; z < 32; z++) {
            var candidate = spread.getPotentialStructureChunk(level.getSeed(), x * 48, z * 48);
            if (spread.applyAdditionalChunkRestrictions(candidate.x, candidate.z, level.getSeed())) passed++;
        }
        require(passed > 1900 && passed < 2200, "half frequency");
        var template = level.getStructureManager().get(id).orElseThrow();
        require(template.getSize().equals(new net.minecraft.core.Vec3i(34,18,33)), "original template size");
        int supplies = 0;
        for (var rotation : Rotation.values()) {
            var origin = new BlockPos(3000 + rotation.ordinal()*96, 180, 3000);
            var settings = new StructurePlaceSettings().setRotation(rotation);
            // SinglePoolElement uses UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE (18), keeping double blocks intact until the template is complete.
            require(template.placeInWorld(level,origin,origin,settings,RandomSource.create(42),18), "rotated jigsaw placement");
            var bases = template.filterBlocks(origin,settings,com.freshfish.mathmaster.init.ModBlocks.LINGXU_BLOCK.get());
            require(bases.size()==1, "one restored lingxu base");
            var base = bases.getFirst();
            require(base.nbt()==null && level.getBlockState(base.pos()).is(com.freshfish.mathmaster.init.ModBlocks.LINGXU_BLOCK.get()), "base has no stale chest data");
            var altarPos = base.pos().above();
            var altar = (com.freshfish.mathmaster.block.entity.NAltarBlockEntity)level.getBlockEntity(altarPos);
            require(altar!=null, "lower altar entity at "+altarPos+" state="+level.getBlockState(altarPos));
            require(altar.isBloodSacrifice(), "blood mode at "+altarPos+" state="+altar.getBlockState()+" removed="+altar.isRemoved());
            require(altar.offerings()==0, "zero initial offerings");
            com.freshfish.mathmaster.block.entity.NAltarBlockEntity.serverTick(level,altarPos,level.getBlockState(altarPos),altar);
            require(level.getBlockState(altarPos).getValue(com.freshfish.mathmaster.block.NAltarBlock.BLOOD_SACRIFICE)
                    && level.getBlockState(altarPos.above()).getValue(com.freshfish.mathmaster.block.NAltarBlock.BLOOD_SACRIFICE), "both altar halves enter blood mode");
            var spawners = template.filterBlocks(origin,settings,Blocks.TRIAL_SPAWNER);
            require(spawners.size() == 4, "four trial spawners");
            var entities = new HashSet<String>();
            for(var info : spawners) {
                var block = (TrialSpawnerBlockEntity)level.getBlockEntity(info.pos());
                require(block != null, "trial block entity survives placement");
                for(var config : new net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig[]{block.getTrialSpawner().getNormalConfig(),block.getTrialSpawner().getOminousConfig()}) {
                    require(config.calculateTargetTotalMobs(0)==5 && config.calculateTargetTotalMobs(3)==5, "five total without multiplayer scaling");
                    require(config.calculateTargetSimultaneousMobs(0)==1 && config.calculateTargetSimultaneousMobs(3)==1, "one at a time");
                    require(config.lootTablesToEject().isEmpty(), "no completion loot");
                    entities.add(config.spawnPotentialsDefinition().getRandomValue(level.random).orElseThrow().getEntityToSpawn().getString("id"));
                }
            }
            require(entities.equals(java.util.Set.of("minecraft:zombie","minecraft:skeleton","mathmaster:nine","mathmaster:eight")), "four original entity types");
            for(var info : template.filterBlocks(origin,settings,Blocks.CHEST)) {
                var chest = (ChestBlockEntity)level.getBlockEntity(info.pos());
                if(info.nbt()!=null && info.nbt().getString("LootTable").equals("mathmaster:chests/n_altar_ruins")) {
                    require(chest!=null && chest.getLootTable()!=null && chest.getLootTable().location().toString().equals("mathmaster:chests/n_altar_ruins"), "supply table retained");
                    chest.unpackLootTable(null);
                    var counts = new java.util.HashMap<String, Integer>();
                    for(int i=0;i<chest.getContainerSize();i++) if(!chest.getItem(i).isEmpty()) {
                        var stack = chest.getItem(i);
                        String item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        var range = supplyRanges().get(item);
                        require(range != null, "unexpected supply item " + item);
                        require(!range.enchanted() || stack.isEnchanted(), "supply equipment enchanted");
                        counts.merge(item, stack.getCount(), Integer::sum);
                    }
                    int minRolls = 0, maxRolls = 0;
                    for (var entry : counts.entrySet()) {
                        var range = supplyRanges().get(entry.getKey());
                        minRolls += (entry.getValue() + range.max() - 1) / range.max();
                        maxRolls += entry.getValue() / range.min();
                    }
                    require(minRolls <= 6 && maxRolls >= 4 && minRolls <= maxRolls, "supply chest consistent with 4-6 rolls");
                    supplies++;
                }
            }
        }
        require(supplies==24,"six supply chests per rotation");
        var generator = level.getChunkSource().getGenerator();
        for(int i=0;i<8;i++) {
            var start=holder.value().generate(level.registryAccess(),generator,generator.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),42L+i,new net.minecraft.world.level.ChunkPos(80+i,80),0,level,b->true);
            require(start.isValid() && start.getPieces().size()==1,"registered jigsaw height projection");
            var piece=(net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece)start.getPieces().getFirst();
            require(piece.getElement().getSize(level.getStructureManager(),Rotation.NONE).equals(template.getSize()),"generated geometry preserved");
        }
        var found=generator.findNearestMapStructure(level,HolderSet.direct(holder),BlockPos.ZERO,100,false);
        require(found!=null,"natural locate");
        var center=new net.minecraft.world.level.ChunkPos(found.getFirst()); boolean valid=false;
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
            var chunk=level.getChunk(center.x+x,center.z+z);
            var start=chunk.getStartForStructure(holder.value());
            if(start!=null&&start.isValid()) valid=true;
        }
        require(valid,"located trial has actual generated start");
        return "N trial: original 34x18x33 geometry, four rotations/fresh blood altars/16 spawners/24 supply chests; 1024 supply samples covering 24 items, 4-6 rolls and seven enchanted equipment items; 17 trial and 21 gateway biomes, 48/18 half-frequency ("+passed+"/4096), eight generated pieces, natural locate at "+found.getFirst();
    }

    private static void checkSupplySamples(ServerLevel level) {
        var table = level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                Registries.LOOT_TABLE, ResourceLocation.parse("mathmaster:chests/n_altar_ruins")));
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        var ranges = supplyRanges();
        var seen = new HashSet<String>();
        var minSeen = new HashSet<String>();
        var maxSeen = new HashSet<String>();
        var seeds = RandomSource.create(42);
        boolean four = false, six = false;
        for (int i = 0; i < 1024; i++) {
            var stacks = table.getRandomItems(params, seeds.nextLong());
            require(stacks.size() >= 4 && stacks.size() <= 6, "4-6 raw supply rolls");
            four |= stacks.size() == 4;
            six |= stacks.size() == 6;
            for (var stack : stacks) {
                String item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                var range = ranges.get(item);
                require(range != null, "unexpected sampled supply " + item);
                require(stack.getCount() >= range.min() && stack.getCount() <= range.max(), "supply quantity " + stack);
                require(!range.enchanted() || stack.isEnchanted(), "sampled supply equipment enchanted");
                seen.add(item);
                if (stack.getCount() == range.min()) minSeen.add(item);
                if (stack.getCount() == range.max()) maxSeen.add(item);
            }
        }
        require(four && six && seen.equals(ranges.keySet()), "supply rolls and all 24 items covered");
        require(minSeen.equals(ranges.keySet()) && maxSeen.equals(ranges.keySet()), "all supply quantity endpoints covered");
    }

    private static java.util.Map<String, SupplyRange> supplyRanges() {
        var ranges = new java.util.HashMap<String, SupplyRange>();
        ranges.put("minecraft:stone_bricks", new SupplyRange(8,16,false));
        ranges.put("minecraft:cobweb", new SupplyRange(1,3,false));
        ranges.put("minecraft:redstone", new SupplyRange(4,12,false));
        ranges.put("minecraft:book", new SupplyRange(1,3,false));
        ranges.put("mathmaster:lingxu_nugget", new SupplyRange(4,12,false));
        ranges.put("minecraft:gold_ingot", new SupplyRange(2,5,false));
        ranges.put("minecraft:iron_ingot", new SupplyRange(2,5,false));
        ranges.put("minecraft:emerald", new SupplyRange(2,4,false));
        ranges.put("minecraft:diamond", new SupplyRange(1,3,false));
        ranges.put("mathmaster:lingxu_ingot", new SupplyRange(1,2,false));
        ranges.put("mathmaster:prime_core", new SupplyRange(1,2,false));
        for (String id : new String[]{"minecraft:golden_apple", "minecraft:enchanted_golden_apple",
                "mathmaster:grade_2_math_study_note", "mathmaster:junior_high_math_study_note",
                "mathmaster:prime_nugget", "mathmaster:lingxu_block"}) ranges.put(id, new SupplyRange(1,1,false));
        for (String id : new String[]{"minecraft:diamond_helmet", "minecraft:diamond_chestplate",
                "minecraft:diamond_leggings", "minecraft:diamond_boots", "mathmaster:lingxu_sword",
                "mathmaster:lingxu_axe", "mathmaster:lingxu_hoe"}) ranges.put(id, new SupplyRange(1,1,true));
        return ranges;
    }

    private record SupplyRange(int min, int max, boolean enchanted) {}
    private static void require(boolean ok,String message) { if(!ok)throw new AssertionError("N trial: "+message); }
}
