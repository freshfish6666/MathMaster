package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.init.ModItems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Exercises template rotation, real chest loot, and natural world generation. */
@EventBusSubscriber(modid = "mathmaster")
public final class GatewayStructureCheck {
    private static final ResourceLocation STRUCTURE = ResourceLocation.parse("mathmaster:ruins/unfinished_overworld_gateway");
    private static final ResourceLocation GIANT = ResourceLocation.parse("mathmaster:ruins/unfinished_overworld_giant_gateway");
    private static final ResourceLocation LOOT = ResourceLocation.parse("mathmaster:chests/unfinished_overworld_gateway");

    private static int templateIndex(Object element, String[] names) {
        // Width alone cannot distinguish templates 1, 3 and 4.
        String identity = element.toString();
        for (int i = 0; i < names.length; i++) {
            if (identity.contains("mathmaster:ruins/" + names[i] + "]")) return i;
        }
        throw new AssertionError("Unexpected template " + identity);
    }

    @SubscribeEvent
    public static void check(ServerStartedEvent event) {
        String result;
        try {
            ServerLevel level = event.getServer().overworld();
            checkLootSamples(level);
            var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(STRUCTURE).orElseThrow();
            int rotations = 0;
            String[] templateNames = {"unfinished_overworld_gateway_1", "unfinished_overworld_gateway_11",
                    "unfinished_overworld_gateway_2", "unfinished_overworld_gateway_3", "unfinished_overworld_gateway_4",
                    "unfinished_overworld_gateway_5", "unfinished_overworld_gateway_6",
                    "unfinished_overworld_gateway_7", "unfinished_overworld_gateway_8",
                    "unfinished_overworld_gateway_9", "unfinished_overworld_gateway_10",
                    "unfinished_overworld_giant_gateway_1"};
            int normalCount = templateNames.length - 1;
            var sizes = new net.minecraft.core.Vec3i[] {
                    new net.minecraft.core.Vec3i(10, 9, 10), new net.minecraft.core.Vec3i(8, 8, 10),
                    new net.minecraft.core.Vec3i(9, 9, 9), new net.minecraft.core.Vec3i(10, 9, 10),
                    new net.minecraft.core.Vec3i(10, 11, 10), new net.minecraft.core.Vec3i(10, 9, 10),
                    new net.minecraft.core.Vec3i(9, 9, 9), new net.minecraft.core.Vec3i(10, 9, 10),
                    new net.minecraft.core.Vec3i(10, 9, 10), new net.minecraft.core.Vec3i(10, 9, 10),
                    new net.minecraft.core.Vec3i(10, 9, 10), new net.minecraft.core.Vec3i(16, 16, 11)};
            for (int index = 0; index < templateNames.length; index++) {
                var template = level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath("mathmaster", "ruins/" + templateNames[index])).orElseThrow();
                require(template.getSize().equals(sizes[index]), "Template dimensions changed");
                for (Rotation rotation : Rotation.values()) {
                    BlockPos origin = new BlockPos(256 + rotations * 48, 200, 256);
                    var settings = new StructurePlaceSettings().setRotation(rotation);
                    var chests = template.filterBlocks(origin, settings, Blocks.CHEST);
                    require(chests.size() == (index == normalCount ? 3 : 1), "Template chest count changed");
                    require(template.placeInWorld(level, origin, origin, settings, RandomSource.create(42), 2), "Template placement failed");
                    int unsupported = 0;
                    for (var chest : chests) {
                        require(level.getBlockState(chest.pos()).equals(chest.state()), "Chest orientation changed");
                        if (!level.getBlockState(chest.pos().below()).isFaceSturdy(level,
                                chest.pos().below(), net.minecraft.core.Direction.UP)) unsupported++;
                        checkChest(level, chest.pos());
                    }
                    // Preserve the giant source's floating chest at local [6,4,2].
                    require(unsupported == (index == normalCount || index == 5 ? 1 : 0), "Original chest support layout changed");
                    rotations++;
                }
            }

            var pool = level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL).get(STRUCTURE);
            require(pool != null && pool.size() == normalCount, "Expected eleven equally weighted templates");
            var sampled = new HashSet<Integer>();
            var random = RandomSource.create(42);
            for (int i = 0; i < 96; i++) {
                var element = pool.getRandomTemplate(random);
                int index = templateIndex(element, templateNames);
                require(index < normalCount && element.getSize(level.getStructureManager(), Rotation.NONE).equals(sizes[index]), "Unexpected template in pool");
                sampled.add(index);
            }
            require(sampled.size() == normalCount, "All eleven normal templates must be selectable");
            var giantPool = level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL).get(GIANT);
            require(giantPool != null && giantPool.size() == 1, "Expected one giant template");
            require(giantPool.getRandomTemplate(random).getSize(level.getStructureManager(), Rotation.NONE).equals(sizes[normalCount]),
                    "Giant pool must use the original template dimensions");

            var sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
            var gatewaySet = sets.get(ResourceLocation.parse("mathmaster:unfinished_overworld_gateways"));
            require(gatewaySet.structures().size() == 2, "Both sizes must share one structure set");
            require(gatewaySet.structures().stream().anyMatch(entry -> entry.structure().is(STRUCTURE) && entry.weight() == 19),
                    "Normal gateways must have weight 19");
            require(gatewaySet.structures().stream().anyMatch(entry -> entry.structure().is(GIANT) && entry.weight() == 1),
                    "Giant gateways must have weight 1");
            var placement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement) gatewaySet.placement();
            var vanilla = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement)
                    sets.get(ResourceLocation.parse("minecraft:ruined_portals")).placement();
            require(placement.spacing() == vanilla.spacing() && placement.separation() == vanilla.separation(), "Spread must match vanilla ruined portals");
            int eligible = 0;
            for (int x = -32; x < 32; x++) {
                for (int z = -32; z < 32; z++) {
                    var candidate = placement.getPotentialStructureChunk(level.getSeed(), x * placement.spacing(), z * placement.spacing());
                    if (placement.applyAdditionalChunkRestrictions(candidate.x, candidate.z, level.getSeed())) eligible++;
                }
            }
            require(eligible > 1900 && eligible < 2200, "Expected roughly half of 4096 candidates, got " + eligible);

            var generator = level.getChunkSource().getGenerator();
            var heightChecked = new HashSet<Integer>();
            for (int sample = 0; sample < 64; sample++) {
                // Exercise the registered structure's height projection, not just raw template placement.
                var start = holder.value().generate(level.registryAccess(), generator, generator.getBiomeSource(),
                        level.getChunkSource().randomState(), level.getStructureManager(), 42L + sample,
                        new net.minecraft.world.level.ChunkPos(32 + sample, 32), 0, level, biome -> true);
                require(start.isValid(), "Height-check structure generation failed");
                var piece = (net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece) start.getPieces().getFirst();
                int index = templateIndex(piece.getElement(), templateNames);
                var template = level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath("mathmaster", "ruins/" + templateNames[index])).orElseThrow();
                var chest = template.filterBlocks(piece.getPosition(), new StructurePlaceSettings().setRotation(piece.getRotation()), Blocks.CHEST).getFirst();
                var box = piece.getBoundingBox();
                int terrainY = generator.getFirstFreeHeight((box.minX() + box.maxX()) / 2,
                        (box.minZ() + box.maxZ()) / 2, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                        level, level.getChunkSource().randomState()) - 1;
                require(piece.getPosition().getY() + 1 == terrainY,
                        templateNames[index] + " must retain the verified template-1 height projection");
                int chestLocalY = new int[]{2, 2, 1, 2, 1, 0, 2, 1, 2, 1, 1}[index];
                require(chest.pos().getY() == piece.getPosition().getY() + chestLocalY, "Original chest height changed");
                heightChecked.add(index);
            }
            require(heightChecked.size() == normalCount, "Height regression must cover all eleven normal templates");
            var giantHolder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(GIANT).orElseThrow();
            require(giantHolder.value().biomes().equals(holder.value().biomes()), "Both sizes must use the same biome rules");
            for (int sample = 0; sample < 16; sample++) {
                var start = giantHolder.value().generate(level.registryAccess(), generator, generator.getBiomeSource(),
                        level.getChunkSource().randomState(), level.getStructureManager(), 42L + sample,
                        new net.minecraft.world.level.ChunkPos(32 + sample, 34), 0, level, biome -> true);
                require(start.isValid(), "Giant height-check generation failed");
                var piece = (net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece) start.getPieces().getFirst();
                require(piece.getElement().getSize(level.getStructureManager(), Rotation.NONE).equals(sizes[normalCount]), "Giant geometry changed");
                var box = piece.getBoundingBox();
                int terrainY = generator.getFirstFreeHeight((box.minX() + box.maxX()) / 2,
                        (box.minZ() + box.maxZ()) / 2, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                        level, level.getChunkSource().randomState()) - 1;
                require(piece.getPosition().getY() + 1 == terrainY, "Giant must use the verified template-1 height projection");
            }
            var normalLocation = checkNatural(level, holder, 1);
            var giantLocation = checkNatural(level, giantHolder, 3);
            require(!new net.minecraft.world.level.ChunkPos(normalLocation).equals(new net.minecraft.world.level.ChunkPos(giantLocation)),
                    "Normal and giant must occupy different candidates");
            String trialResult = NTrialStructureCheck.run(level);
            String netherResult = NetherGatewayStructureCheck.run(event.getServer().getLevel(net.minecraft.world.level.Level.NETHER));
            String sanctuaryResult = GeometrySanctuaryStructureCheck.run(level);
            String bloodRewardResult = NAltarRewardLootCheck.run(level);
            result = "PASS: twelve original-size templates (eleven normal, one giant); " + rotations
                    + " rotations and 56 chest loot checks; 1024 loot samples covering all 21 items, 6-8 rolls, quantities, six enchanted equipment items and actual N trial maps; 64 normal + 16 giant height checks; separate locate IDs; shared 19:1 weights; half-frequency ("
                    + eligible + "/4096); natural normal at " + normalLocation + "; natural giant at " + giantLocation + "; " + trialResult + "; " + netherResult + "; " + sanctuaryResult + "; " + bloodRewardResult;
        } catch (Throwable failure) {
            result = "FAIL: " + failure;
            failure.printStackTrace();
        }
        System.out.println("GATEWAY_STRUCTURE_CHECK " + result);
        try {
            Files.writeString(Path.of("gateway-result.txt"), result);
        } catch (Exception failure) {
            throw new RuntimeException(failure);
        } finally {
            event.getServer().halt(false);
        }
    }

    private static BlockPos checkNatural(ServerLevel level, net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure> holder,
            int expectedChests) {
            var generator = level.getChunkSource().getGenerator();
            var located = generator.findNearestMapStructure(level, HolderSet.direct(holder), BlockPos.ZERO, 100, false);
            require(located != null, "No naturally generated gateway found");
            var center = new net.minecraft.world.level.ChunkPos(located.getFirst());
            BoundingBox bounds = null;
            for (int dx = -1; dx <= 1 && bounds == null; dx++) {
                for (int dz = -1; dz <= 1 && bounds == null; dz++) {
                    var start = level.getChunk(center.x + dx, center.z + dz).getStartForStructure(holder.value());
                    if (start != null && start.isValid()) {
                        var box = start.getPieces().getFirst().getBoundingBox();
                        var pos = located.getFirst();
                        // Locate positions use the placement's Y=0, not the surface elevation.
                        if (pos.getX() >= box.minX() && pos.getX() <= box.maxX()
                                && pos.getZ() >= box.minZ() && pos.getZ() <= box.maxZ()) {
                            bounds = box;
                        }
                    }
                }
            }
            require(bounds != null, "Located structure has no valid natural start");
            int chestCount = 0;
            for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                if (level.getBlockEntity(pos) instanceof ChestBlockEntity) {
                    checkChest(level, pos);
                    chestCount++;
                }
            }
            require(chestCount == expectedChests, "Natural gateway chest count: " + chestCount + ", expected " + expectedChests);
            return located.getFirst();
    }

    private static void checkChest(ServerLevel level, BlockPos pos) {
        require(level.getBlockEntity(pos) instanceof ChestBlockEntity, "Chest block entity missing at " + pos);
        var chest = (ChestBlockEntity) level.getBlockEntity(pos);
        require(chest.getLootTable() != null && chest.getLootTable().location().equals(LOOT), "Chest lost its loot table");
        chest.unpackLootTable(null);
        var ranges = lootRanges();
        var counts = new HashMap<Item, Integer>();
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            var stack = chest.getItem(slot);
            if (!stack.isEmpty()) {
                require(ranges.containsKey(stack.getItem()), "Unexpected chest loot: " + stack);
                require(!ranges.get(stack.getItem()).enchanted() || stack.isEnchanted(), "Missing equipment enchantment: " + stack);
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        // Chest filling splits stacks; validate aggregate quantities against a possible 6-8-roll result.
        int minimumRolls = 0;
        int maximumRolls = 0;
        for (var entry : counts.entrySet()) {
            var range = ranges.get(entry.getKey());
            minimumRolls += (entry.getValue() + range.max() - 1) / range.max();
            maximumRolls += entry.getValue() / range.min();
        }
        require(minimumRolls <= 8 && maximumRolls >= 6 && minimumRolls <= maximumRolls,
                "Chest quantities cannot result from 6-8 rolls: " + counts);
    }

    private static void checkLootSamples(ServerLevel level) {
        var table = level.getServer().reloadableRegistries().getLootTable(
                net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, LOOT));
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        var ranges = lootRanges();
        var seen = new HashSet<Item>();
        boolean sixRolls = false;
        boolean eightRolls = false;
        boolean threeNuggets = false;
        boolean nineNuggets = false;
        // Unrelated deterministic seeds avoid correlated first draws from consecutive legacy RNG seeds.
        var sampleSeeds = RandomSource.create(42L);
        for (int sample = 1; sample <= 1024; sample++) {
            var stacks = table.getRandomItems(params, sampleSeeds.nextLong());
            require(stacks.size() >= 6 && stacks.size() <= 8, "Loot must have 6-8 raw draws");
            sixRolls |= stacks.size() == 6;
            eightRolls |= stacks.size() == 8;
            for (ItemStack stack : stacks) {
                var range = ranges.get(stack.getItem());
                require(range != null, "Unexpected sampled loot: " + stack);
                require(stack.getCount() >= range.min() && stack.getCount() <= range.max(), "Invalid single-draw quantity: " + stack);
                require(!range.enchanted() || stack.isEnchanted(), "Sampled equipment is not enchanted: " + stack);
                seen.add(stack.getItem());
                if (stack.is(Items.FILLED_MAP)) checkTrialMap(level, stack);
                if (stack.is(ModItems.LINGXU_NUGGET.get())) {
                    threeNuggets |= stack.getCount() == 3;
                    nineNuggets |= stack.getCount() == 9;
                }
            }
        }
        require(seen.equals(ranges.keySet()), "Sampling must cover all 21 loot items");
        require(sixRolls && eightRolls && threeNuggets && nineNuggets,
                "Boundary coverage: rolls6=" + sixRolls + ", rolls8=" + eightRolls
                        + ", nuggets3=" + threeNuggets + ", nuggets9=" + nineNuggets);
    }

    private static Map<Item, LootRange> lootRanges() {
        return Map.ofEntries(
                Map.entry(Items.STONE_BRICKS, new LootRange(4, 12, false)),
                Map.entry(Items.ROTTEN_FLESH, new LootRange(3, 8, false)),
                Map.entry(ModItems.DIGITALLY_CORRUPTED_BLOCK.get(), new LootRange(1, 3, false)),
                Map.entry(Items.COBWEB, new LootRange(1, 3, false)),
                Map.entry(ModItems.LINGXU_NUGGET.get(), new LootRange(3, 9, false)),
                Map.entry(Items.IRON_NUGGET, new LootRange(6, 18, false)),
                Map.entry(Items.GOLD_NUGGET, new LootRange(4, 12, false)),
                Map.entry(Items.GOLDEN_APPLE, new LootRange(1, 1, false)),
                Map.entry(ModItems.LINGXU_HOE.get(), new LootRange(1, 1, true)),
                Map.entry(ModItems.LINGXU_SHOVEL.get(), new LootRange(1, 1, true)),
                Map.entry(ModItems.LINGXU_BOOTS.get(), new LootRange(1, 1, true)),
                Map.entry(ModItems.GRADUATION_CAP.get(), new LootRange(1, 1, true)),
                Map.entry(ModItems.PRIME_CORE.get(), new LootRange(1, 2, false)),
                Map.entry(ModItems.ELEMENTARY_GRADE_2_MATH.get(), new LootRange(1, 1, false)),
                Map.entry(ModItems.EMPTY_SET.get(), new LootRange(1, 1, false)),
                Map.entry(Items.ENCHANTED_GOLDEN_APPLE, new LootRange(1, 1, false)),
                Map.entry(ModItems.LINGXU_INGOT.get(), new LootRange(1, 2, false)),
                Map.entry(Items.FILLED_MAP, new LootRange(1, 1, false)),
                Map.entry(ModItems.LINGXU_CHESTPLATE.get(), new LootRange(1, 1, true)),
                Map.entry(ModItems.LINGXU_LEGGINGS.get(), new LootRange(1, 1, true)),
                Map.entry(ModItems.PRIME_NUGGET.get(), new LootRange(1, 1, false)));
    }

    private static void checkTrialMap(ServerLevel level, ItemStack stack) {
        var data = net.minecraft.world.item.MapItem.getSavedData(stack, level);
        require(data != null && data.scale == 2 && data.dimension.equals(level.dimension()), "Map data missing or incorrect");
        var name = stack.get(net.minecraft.core.component.DataComponents.ITEM_NAME);
        require(name != null && name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated
                && translated.getKey().equals("filled_map.mathmaster.n_trial"), "Map must use its translated trial name");
        var decorations = stack.get(net.minecraft.core.component.DataComponents.MAP_DECORATIONS);
        require(decorations != null && decorations.decorations().containsKey("+"), "Trial marker missing");
        var marker = decorations.decorations().get("+");
        require(marker.type().equals(net.minecraft.world.level.saveddata.maps.MapDecorationTypes.RED_X), "Wrong trial marker");
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getHolder(ResourceLocation.parse("mathmaster:trials/n_trial")).orElseThrow();
        BlockPos position = BlockPos.containing(marker.x(), 0, marker.z());
        var located = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), position, 1, false);
        require(located != null && located.getFirst().equals(position), "Map does not point to an actual N trial");
        require(Math.abs(data.centerX - marker.x()) <= 256 && Math.abs(data.centerZ - marker.z()) <= 256, "Trial marker outside map");
    }

    private record LootRange(int min, int max, boolean enchanted) {}

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
