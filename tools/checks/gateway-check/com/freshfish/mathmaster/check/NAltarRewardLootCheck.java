package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModItems;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Samples the decoded random pool; exercises real guarantees/maps and split chest filling separately. */
final class NAltarRewardLootCheck {
    private static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.parse("mathmaster:n/blood_sacrifice"));
    private static final ResourceLocation SANCTUARY = ResourceLocation.parse("mathmaster:trials/geometry_sanctuary");
    private static int checks;

    static String run(ServerLevel level) {
        checks = 0;
        var table = level.getServer().reloadableRegistries().getLootTable(TABLE);
        var mapTag = TagKey.create(Registries.STRUCTURE,ResourceLocation.parse("mathmaster:on_geometry_sanctuary_maps"));
        var targets = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(mapTag).orElseThrow();
        require(targets.size() == 1 && targets.get(0).is(SANCTUARY), "map tag only contains geometry sanctuary");
        require(table.getPool("guaranteed_geometry_sanctuary_map") != null
                && table.getPool("guaranteed_lingxu_ingots") != null && table.getPool("random_rewards") != null,
                "two independent guarantees and random pool loaded");
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        // Share only the immutable decoded pool; no registry table is modified, and map searches aren't repeated 1024 times.
        var randomTable = LootTable.lootTable().setParamSet(LootContextParamSets.CHEST).build();
        randomTable.addPool(table.getPool("random_rewards"));
        var ranges = ranges();
        var seen = new HashSet<Item>();
        var seeds = RandomSource.create(7231);
        boolean three = false, five = false, lowNuggets = false, highNuggets = false;
        for (int sample = 0; sample < 1024; sample++) {
            var rewards = randomTable.getRandomItems(params,seeds.nextLong());
            require(rewards.size() >= 3 && rewards.size() <= 5, "random pool gives exactly 3-5 draws");
            three |= rewards.size() == 3; five |= rewards.size() == 5;
            for (var stack : rewards) {
                validateRandom(stack,ranges); seen.add(stack.getItem());
                if (stack.is(ModItems.LINGXU_NUGGET.get())) { lowNuggets |= stack.getCount() == 8; highNuggets |= stack.getCount() == 16; }
            }
        }
        require(seen.equals(ranges.keySet()), "all 13 random rewards selectable; geometry core absent");
        require(three && five && lowNuggets && highNuggets, "random draw and quantity boundaries covered");
        BlockPos firstTarget = null;
        for (int sample = 0; sample < 3; sample++) {
            var rewards = table.getRandomItems(params,seeds.nextLong());
            require(rewards.size() >= 5 && rewards.size() <= 7, "guarantees do not consume random draws");
            int maps = 0, ingots = 0, random = 0;
            for (var stack : rewards) {
                if (stack.is(Items.FILLED_MAP)) {
                    maps += stack.getCount(); var target = checkMap(level,stack);
                    if (firstTarget == null) firstTarget = target;
                    else require(firstTarget.equals(target), "repeat rewards may point to the same sanctuary");
                } else if (stack.is(ModItems.LINGXU_INGOT.get())) ingots += stack.getCount();
                else { validateRandom(stack,ranges); random++; }
            }
            require(maps == 1 && ingots >= 2 && ingots <= 3 && random >= 3 && random <= 5, "complete raw reward guarantees");
        }
        var chestPos = new BlockPos(100,240,100);
        try {
            for (int sample = 0; sample < 3; sample++) {
                level.setBlockAndUpdate(chestPos,Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(chestPos,Blocks.CHEST.defaultBlockState());
                var chest = (ChestBlockEntity)level.getBlockEntity(chestPos);
                chest.setLootTable(TABLE,seeds.nextLong()); chest.unpackLootTable(null);
                int maps = 0, ingots = 0;
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    var stack = chest.getItem(slot); if (stack.isEmpty()) continue;
                    if (stack.is(Items.FILLED_MAP)) { maps += stack.getCount(); checkMap(level,stack); }
                    else if (stack.is(ModItems.LINGXU_INGOT.get())) ingots += stack.getCount();
                    else {
                        var range = ranges.get(stack.getItem());
                        require(range != null && (!range.enchanted() || stack.isEnchanted()), "real chest only contains approved random rewards");
                    }
                }
                require(maps == 1 && ingots >= 2 && ingots <= 3, "split chest fill retains guarantees");
                var opened = chest.saveWithFullMetadata(level.registryAccess()).getList("Items",10).copy();
                require(chest.getLootTable() == null, "loot reference consumed after first open");
                chest.unpackLootTable(null);
                require(opened.equals(chest.saveWithFullMetadata(level.registryAccess()).getList("Items",10)), "second open doesn't reroll");
            }
        } finally { level.setBlockAndUpdate(chestPos,Blocks.AIR.defaultBlockState()); }
        // Vanilla fallback outside the Overworld remains a named empty map, never a fabricated destination.
        var nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        var netherParams = new LootParams.Builder(nether).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).create(LootContextParamSets.CHEST);
        var fallback = table.getRandomItems(netherParams,73L).stream().filter(stack -> stack.is(Items.MAP)).toList();
        require(fallback.size() == 1 && fallback.getFirst().getCount() == 1
                && fallback.getFirst().get(DataComponents.MAP_ID) == null, "unsupported dimension preserves one empty map fallback");
        return "N altar rewards: " + checks + " assertions; 1024 random-pool samples/13 items/3-5 draws; map + 2-3 ingots guaranteed; six actual sanctuary maps/chest reopening; native fallback";
    }

    private static BlockPos checkMap(ServerLevel level, ItemStack stack) {
        var data = MapItem.getSavedData(stack,level);
        require(data != null && data.scale == 2 && data.dimension.equals(level.dimension()), "filled map has correct saved dimension/scale");
        var name = stack.get(DataComponents.ITEM_NAME);
        require(name != null && name.getContents() instanceof TranslatableContents translated
                && translated.getKey().equals("filled_map.mathmaster.geometry_sanctuary"), "translated sanctuary map name");
        var decorations = stack.get(DataComponents.MAP_DECORATIONS);
        require(decorations != null && decorations.decorations().containsKey("+"), "target marker present");
        var marker = decorations.decorations().get("+");
        require(marker.type().equals(MapDecorationTypes.RED_X), "red cross marker");
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(SANCTUARY).orElseThrow();
        var target = BlockPos.containing(marker.x(),0,marker.z());
        var chunk = new ChunkPos(target);
        var start = level.getChunk(chunk.x,chunk.z).getStartForStructure(holder.value());
        require(start != null && start.isValid(), "map points at actual generated sanctuary");
        int references = start.getReferences();
        var located = level.getChunkSource().getGenerator().findNearestMapStructure(level,HolderSet.direct(holder),target,1,false);
        require(located != null && located.getFirst().equals(target) && start.getReferences() == references,
                "repeated target lookup doesn't reserve/consume structure references");
        require(Math.abs(data.centerX-marker.x()) <= 256 && Math.abs(data.centerZ-marker.z()) <= 256, "marker fits map extent");
        var restored = ItemStack.parse(level.registryAccess(),stack.save(level.registryAccess())).orElseThrow();
        require(restored.get(DataComponents.MAP_ID).equals(stack.get(DataComponents.MAP_ID))
                && restored.get(DataComponents.MAP_DECORATIONS).equals(decorations), "map components survive item save/load");
        return target;
    }

    private static void validateRandom(ItemStack stack, Map<Item,Range> ranges) {
        var range = ranges.get(stack.getItem());
        require(range != null && stack.getCount() >= range.min() && stack.getCount() <= range.max()
                && (!range.enchanted() || stack.isEnchanted()), "valid random quantity/enchantment: " + stack);
    }

    private static Map<Item,Range> ranges() {
        return Map.ofEntries(
                Map.entry(ModItems.LINGXU_NUGGET.get(),new Range(8,16,false)),
                Map.entry(Items.ENDER_PEARL,new Range(2,4,false)), Map.entry(Items.GOLDEN_APPLE,new Range(1,1,false)),
                Map.entry(Items.DIAMOND,new Range(1,3,false)), Map.entry(ModItems.PRIME_CORE.get(),new Range(1,1,false)),
                Map.entry(ModItems.GRADE_2_MATH_STUDY_NOTE.get(),new Range(1,1,false)),
                Map.entry(ModItems.JUNIOR_HIGH_MATH_STUDY_NOTE.get(),new Range(1,1,false)),
                Map.entry(ModItems.LINGXU_SWORD.get(),new Range(1,1,true)), Map.entry(ModItems.LINGXU_AXE.get(),new Range(1,1,true)),
                Map.entry(ModItems.LINGXU_CHESTPLATE.get(),new Range(1,1,true)), Map.entry(Items.ENCHANTED_GOLDEN_APPLE,new Range(1,1,false)),
                Map.entry(ModItems.PRIME_NUGGET.get(),new Range(1,1,false)), Map.entry(ModBlocks.LINGXU_BLOCK.get().asItem(),new Range(1,1,false)));
    }

    private record Range(int min,int max,boolean enchanted) {}
    private static void require(boolean value,String message) { checks++; if (!value) throw new AssertionError("Blood loot: " + message); }
}
