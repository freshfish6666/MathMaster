package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.entity.GeometryConstructEntity;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.worldgen.GeometrySanctuaryPoolElement;
import com.freshfish.mathmaster.worldgen.GeometrySanctuaryStructure;
import com.freshfish.mathmaster.worldgen.GeometrySanctuaryTerrain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Bounded markers, original contents, rotated chunk placement, terrain selection and actual locate. */
final class GeometrySanctuaryStructureCheck {
    private static final ResourceLocation ID = ResourceLocation.parse("mathmaster:trials/geometry_sanctuary");
    private static int checks;

    static String run(ServerLevel level) {
        checks = 0;
        checkTerrain();
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(ID).orElseThrow();
        require(holder.value() instanceof GeometrySanctuaryStructure, "new structure type");
        var biomes = new HashSet<String>();
        holder.value().biomes().stream().forEach(b -> biomes.add(b.unwrapKey().orElseThrow().location().toString()));
        require(biomes.equals(Set.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna",
                "minecraft:savanna_plateau", "minecraft:desert", "minecraft:snowy_plains")), "six agreed biomes");
        var ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
        var encoded = Structure.DIRECT_CODEC.encodeStart(ops, holder.value()).getOrThrow();
        require(Structure.DIRECT_CODEC.parse(ops, encoded).getOrThrow() instanceof GeometrySanctuaryStructure, "structure codec");
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(ResourceLocation.parse("mathmaster:geometry_sanctuaries"));
        require(set.structures().size() == 1 && set.structures().getFirst().structure().is(ID), "independent structure set");
        var spread = (RandomSpreadStructurePlacement) set.placement();
        require(spread.spacing() == 64 && spread.separation() == 24, "64/24 spacing");
        checkAvoidance(level, spread, ops);
        int passed = 0;
        for (int x = -32; x < 32; x++) for (int z = -32; z < 32; z++) {
            var candidate = spread.getPotentialStructureChunk(level.getSeed(), x * 64, z * 64);
            if (spread.applyAdditionalChunkRestrictions(candidate.x, candidate.z, level.getSeed())) passed++;
        }
        require(passed > 2900 && passed < 3300, "75 percent candidate filter");
        var template = level.getStructureManager().get(ID).orElseThrow();
        require(template.getSize().equals(new net.minecraft.core.Vec3i(45,20,40)), "original size retained");
        require(template.save(new CompoundTag()).getList("entities", 10).size() == 3, "three saved entities retained");
        var element = GeometrySanctuaryPoolElement.create(ID);
        var generator = level.getChunkSource().getGenerator();
        var constructs = new ArrayList<GeometryConstructEntity>();
        Consumer<EntityJoinLevelEvent> observer = event -> {
            if (event.getEntity() instanceof GeometryConstructEntity mob) constructs.add(mob);
        };
        NeoForge.EVENT_BUS.addListener(observer);
        try {
            for (Rotation rotation : Rotation.values()) {
                constructs.clear();
                BlockPos origin = new BlockPos(4400 + rotation.ordinal() * 128, 180, 4400);
                var box = element.getBoundingBox(level.getStructureManager(), origin, rotation);
                var settings = new StructurePlaceSettings().setRotation(rotation);
                var markers = element.getDataMarkers(level.getStructureManager(), origin, rotation, true);
                require(markers.size() == 2 && markers.stream().allMatch(m -> GeometrySanctuaryPoolElement.CONSTRUCT_MARKER
                        .equals(m.nbt().getString("metadata"))), "two user marker names retained");
                // Worldgen calls the same piece once for each intersecting chunk, with a clipped box.
                for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                    for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                        var clip = new BoundingBox(cx * 16, -64, cz * 16, cx * 16 + 15, 319, cz * 16 + 15);
                        require(element.place(level.getStructureManager(), level, level.structureManager(), generator,
                                origin, origin, rotation, clip, RandomSource.create(42), LiquidSettings.IGNORE_WATERLOGGING, false),
                                "rotated clipped placement");
                    }
                }
                require(constructs.size() == 2, "exactly two markers spawn without cross-chunk duplicates");
                for (var marker : markers) {
                    require(constructs.stream().filter(m -> m.blockPosition().equals(marker.pos())).count() == 1,
                            "rotated marker world position");
                    require(level.getBlockState(marker.pos()).isAir(), "placeholder removed");
                }
                for (var mob : constructs) {
                    require(mob.isPersistenceRequired() && mob.getHealth() == 24, "persistent mobs keep ordinary stats");
                    var saved = new CompoundTag(); mob.saveWithoutId(saved);
                    require(saved.getBoolean("PersistenceRequired"), "mob persistence survives save");
                    mob.discard();
                }
                for (var info : template.filterBlocks(origin, settings, Blocks.CHEST)) {
                    var chest = (ChestBlockEntity)level.getBlockEntity(info.pos());
                    require(chest != null && level.getBlockState(info.pos()).equals(info.state()), "original chest orientation");
                    if (info.nbt().contains("LootTable")) {
                        require(chest.getLootTable() != null && chest.getLootTable().location().toString()
                                .equals(info.nbt().getString("LootTable")), "original loot references");
                    } else {
                        var saved = chest.saveWithFullMetadata(level.registryAccess());
                        require(saved.getList("Items", 10).equals(info.nbt().getList("Items", 10)), "fixed chest items and books preserved");
                    }
                }
                var altars = template.filterBlocks(origin, settings, ModBlocks.GEOMETRY_ALTAR.get());
                require(altars.size() == 1 && level.getBlockEntity(altars.getFirst().pos()) != null, "one intact altar");
                var piece = new PoolElementStructurePiece(level.getStructureManager(), element, origin, 0, rotation, box,
                        LiquidSettings.IGNORE_WATERLOGGING);
                var serialization = StructurePieceSerializationContext.fromLevel(level);
                var restored = new PoolElementStructurePiece(serialization, piece.createTag(serialization));
                require(restored.getElement() instanceof GeometrySanctuaryPoolElement
                        && restored.getPosition().equals(origin) && restored.getRotation() == rotation, "piece saves custom marker behavior");
            }
            constructs.clear();
            var metadata = new CompoundTag(); metadata.putString("metadata", "mathmaster:unknown_marker");
            var info = new StructureTemplate.StructureBlockInfo(new BlockPos(0,180,0), Blocks.STRUCTURE_BLOCK.defaultBlockState(), metadata);
            element.handleDataMarker(level, info, BlockPos.ZERO, Rotation.NONE, RandomSource.create(1), new BoundingBox(-1,170,-1,1,190,1));
            require(constructs.isEmpty(), "unknown markers do not summon arbitrary entities");
            metadata.putString("metadata", GeometrySanctuaryPoolElement.CONSTRUCT_MARKER);
            element.handleDataMarker(level, info, BlockPos.ZERO, Rotation.NONE, RandomSource.create(1), new BoundingBox(16,170,16,31,190,31));
            require(constructs.isEmpty(), "out-of-chunk marker skipped");
        } finally { NeoForge.EVENT_BUS.unregister(observer); constructs.forEach(m -> {if (!m.isRemoved()) m.discard();}); }
        int valid = 0, rejected = 0;
        for (int i = 0; i < 128; i++) {
            var start = holder.value().generate(level.registryAccess(), generator, generator.getBiomeSource(),
                    level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
                    new ChunkPos(32 + i * 3, 40), 0, level, biome -> true);
            if (!start.isValid()) { rejected++; continue; }
            valid++;
            var piece = (PoolElementStructurePiece)start.getPieces().getFirst();
            require(piece.getElement() instanceof GeometrySanctuaryPoolElement, "generated piece uses marker handler");
            var floor = GeometrySanctuaryTerrain.findFloor(piece.getBoundingBox(), 20, -64, 320,
                    (x,z) -> generator.getBaseColumn(x,z,level,level.getChunkSource().randomState()));
            require(floor.isPresent() && floor.getAsInt() == piece.getPosition().getY(), "real terrain obeys dry nine-point height rule");
        }
        require(valid > 0 && rejected > 0, "real landscape both accepts and rejects sites");
        var naturalJoins = new java.util.concurrent.CopyOnWriteArrayList<GeometryConstructEntity>();
        Consumer<EntityJoinLevelEvent> naturalObserver = event -> {
            if (event.getEntity() instanceof GeometryConstructEntity mob) naturalJoins.add(mob);
        };
        NeoForge.EVENT_BUS.addListener(naturalObserver);
        try {
            var located = generator.findNearestMapStructure(level, HolderSet.direct(holder), BlockPos.ZERO, 100, false);
            require(located != null, "natural locate succeeds");
            var chunk = new ChunkPos(located.getFirst());
            var natural = level.getChunk(chunk.x,chunk.z).getStartForStructure(holder.value());
            require(natural != null && natural.isValid(), "located sanctuary has actual generated start");
            var state = ChunkGeneratorStructureState.createForNormal(level.getChunkSource().randomState(),level.getSeed(),
                    generator.getBiomeSource(),level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET));
            var villageSet = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                    .getHolder(ResourceLocation.withDefaultNamespace("villages")).orElseThrow();
            require(!state.hasStructureChunkInRange(villageSet,chunk.x,chunk.z,12),
                    "natural sanctuary has no prospective village starts in reserved range");
            var piece = (PoolElementStructurePiece)natural.getPieces().getFirst();
            var markers = ((GeometrySanctuaryPoolElement)piece.getElement()).getDataMarkers(level.getStructureManager(),
                    piece.getPosition(), piece.getRotation(), true);
            // Finish every intersecting chunk to include markers on the far side of the large template.
            var box = piece.getBoundingBox();
                for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) level.getChunk(x,z);
                require(markers.stream().allMatch(m -> level.getBlockState(m.pos()).isAir()), "natural placeholders absent");
                // A locate probe has no visitor, so remote entity sections can be HIDDEN.
                // Observe actual joins from protochunk conversion instead of the visible-entity query.
                var naturalMobs = naturalJoins.stream().filter(m -> box.isInside(m.blockPosition())).toList();
                require(naturalMobs.size() == 2 && naturalMobs.stream().allMatch(GeometryConstructEntity::isPersistenceRequired),
                        "natural generation spawns two persistent sentries; actual=" + naturalMobs.size());
            return "Geometry sanctuary: " + checks + " checks; four chunk-clipped rotations; original chests/entities; bounded markers; "
                    + valid + "/128 terrain candidates; six biomes; 64/24 three-quarter frequency (" + passed + "/4096); natural at " + located.getFirst();
        } finally { NeoForge.EVENT_BUS.unregister(naturalObserver); }
    }

    private static void checkAvoidance(ServerLevel level, RandomSpreadStructurePlacement placement,
            RegistryOps<net.minecraft.nbt.Tag> ops) {
        var encoded = (CompoundTag)StructurePlacement.CODEC.encodeStart(ops,placement).getOrThrow();
        var zone = encoded.getCompound("exclusion_zone");
        require(zone.getString("other_set").equals("minecraft:villages") && zone.getInt("chunk_count") == 12,
                "vanilla village exclusion with 12-chunk radius");
        require(encoded.getFloat("frequency") == 0.75F && encoded.getInt("salt") == 184735117,
                "75 percent candidates, original salt retained");
        var restored = StructurePlacement.CODEC.parse(ops,encoded).getOrThrow();
        var generator = level.getChunkSource().getGenerator();
        var state = ChunkGeneratorStructureState.createForNormal(level.getChunkSource().randomState(),level.getSeed(),
                generator.getBiomeSource(),level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET));
        var villageSet = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .getHolder(ResourceLocation.withDefaultNamespace("villages")).orElseThrow();
        int rejected = 0, allowed = 0;
        for (int x=-2; x<2; x++) for (int z=-2; z<2; z++) {
            var candidate = placement.getPotentialStructureChunk(level.getSeed(),x*64,z*64);
            boolean expected = placement.applyAdditionalChunkRestrictions(candidate.x,candidate.z,level.getSeed())
                    && !state.hasStructureChunkInRange(villageSet,candidate.x,candidate.z,12);
            boolean actual = placement.isStructureChunk(state,candidate.x,candidate.z);
            if (actual) allowed++; else rejected++;
            require(actual == expected && restored.isStructureChunk(state,candidate.x,candidate.z) == actual,
                    "native exclusion/codec agree at positive and negative candidate coordinates");
        }
        require(rejected > 0 && allowed > 0, "exclusion rejects nearby villages and retains usable sanctuary sites");
    }

    private static void checkTerrain() {
        var box = new BoundingBox(0,0,0,44,19,39);
        int[] calls = {0};
        require(GeometrySanctuaryTerrain.findFloor(box,20,-64,320,(x,z) -> {calls[0]++; return column(70, false);}).orElseThrow() == 70
                && calls[0] == 9, "exactly nine columns, original one-block embedding");
        require(GeometrySanctuaryTerrain.findFloor(box,20,-64,320,(x,z) -> column(x == 0 ? 76 : 70,false)).isPresent(), "six-block slope allowed");
        require(GeometrySanctuaryTerrain.findFloor(box,20,-64,320,(x,z) -> column(x == 0 ? 77 : 70,false)).isEmpty(), "seven-block slope rejected");
        require(GeometrySanctuaryTerrain.findFloor(box,20,-64,320,(x,z) -> column(70,x==22 && z==19)).isEmpty(), "center water rejected");
        require(GeometrySanctuaryTerrain.findFloor(box,20,-64,320,(x,z) -> column(70,x==44 && z==39)).isEmpty(), "edge water rejected");
        require(GeometrySanctuaryTerrain.findFloor(box,20,-64,320,(x,z) -> column(301,false)).isEmpty(), "build-height overflow rejected");
        require(GeometrySanctuaryTerrain.findFloor(box,20,-64,320,(x,z) -> column(300,false)).isPresent(), "exact upper build boundary");
    }
    private static NoiseColumn column(int top, boolean water) {
        BlockState[] states = new BlockState[384]; Arrays.fill(states, Blocks.AIR.defaultBlockState());
        for (int y = -64; y <= top; y++) states[y+64] = Blocks.STONE.defaultBlockState();
        if (water) states[top+65] = Blocks.WATER.defaultBlockState();
        return new NoiseColumn(-64,states);
    }
    private static void require(boolean value, String message) { checks++; if (!value) throw new AssertionError("Sanctuary: " + message); }
}
