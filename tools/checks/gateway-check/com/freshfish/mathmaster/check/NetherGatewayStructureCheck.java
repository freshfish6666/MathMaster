package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.worldgen.NetherGatewayTerrain;
import com.freshfish.mathmaster.worldgen.UnfinishedNetherGatewayStructure;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

final class NetherGatewayStructureCheck {
    private static final ResourceLocation ID = ResourceLocation.parse("mathmaster:ruins/unfinished_nether_gateway");
    private static final ResourceLocation LOOT = ResourceLocation.parse("mathmaster:chests/unfinished_nether_gateway");
    private static int assertions;

    static String run(ServerLevel level) {
        assertions = 0;
        checkTerrain();
        var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(ID).orElseThrow();
        require(holder.value() instanceof UnfinishedNetherGatewayStructure, "custom floor-search type registered");
        var biomeIds = new HashSet<String>();
        holder.value().biomes().stream().forEach(b -> biomeIds.add(b.unwrapKey().orElseThrow().location().toString()));
        require(biomeIds.equals(java.util.Set.of("minecraft:nether_wastes", "minecraft:soul_sand_valley",
                "minecraft:crimson_forest", "minecraft:warped_forest", "minecraft:basalt_deltas")), "exact five Nether biomes");
        var ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
        var encoded = Structure.DIRECT_CODEC.encodeStart(ops, holder.value()).getOrThrow();
        require(Structure.DIRECT_CODEC.parse(ops, encoded).getOrThrow() instanceof UnfinishedNetherGatewayStructure,
                "structure codec round trip");
        var empty = encoded.copy();
        ((net.minecraft.nbt.CompoundTag) empty).put("templates", new net.minecraft.nbt.ListTag());
        require(Structure.DIRECT_CODEC.parse(ops, empty).error().isPresent(), "empty template list rejected");
        int[] heights = {8, 7, 6, 8, 8};
        int rotations = 0;
        for (int i = 1; i <= 5; i++) {
            var template = level.getStructureManager().get(templateId(i)).orElseThrow();
            require(template.getSize().equals(new net.minecraft.core.Vec3i(10, heights[i-1], 10)), "original dimensions " + i);
            for (Rotation rotation : Rotation.values()) {
                var origin = new BlockPos(256 + rotations * 32, 160, 256);
                var settings = new StructurePlaceSettings().setRotation(rotation);
                var chests = template.filterBlocks(origin, settings, Blocks.CHEST);
                require(chests.size() == 1, "one original chest");
                require(template.placeInWorld(level, origin, origin, settings, RandomSource.create(42), 18), "rotated template placement");
                require(level.getBlockState(chests.getFirst().pos()).equals(chests.getFirst().state()), "chest orientation retained");
                checkChest(level, chests.getFirst().pos());
                rotations++;
            }
        }
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(ResourceLocation.parse("mathmaster:unfinished_nether_gateways"));
        require(set.structures().size() == 1 && set.structures().getFirst().structure().is(ID), "independent Nether structure set");
        var placement = (RandomSpreadStructurePlacement) set.placement();
        require(placement.spacing() == 40 && placement.separation() == 15, "40/15 distribution");
        int eligible = 0;
        for (int x = -32; x < 32; x++) for (int z = -32; z < 32; z++) {
            var candidate = placement.getPotentialStructureChunk(level.getSeed(), x * 40, z * 40);
            if (placement.applyAdditionalChunkRestrictions(candidate.x, candidate.z, level.getSeed())) eligible++;
        }
        require(eligible > 1900 && eligible < 2200, "roughly half candidates pass");
        var generator = level.getChunkSource().getGenerator();
        var selected = new HashSet<String>();
        var generatedRotations = new HashSet<Rotation>();
        int valid = 0;
        int rejected = 0;
        for (int sample = 0; sample < 160; sample++) {
            var start = holder.value().generate(level.registryAccess(), generator, generator.getBiomeSource(),
                    level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
                    new ChunkPos(32 + sample, 40), 0, level, b -> true);
            if (!start.isValid()) { rejected++; continue; }
            require(start.getPieces().size() == 1, "single intact template piece");
            var piece = (PoolElementStructurePiece) start.getPieces().getFirst();
            require(piece.getPosition().getY() >= 32 && piece.getPosition().getY() <= 100, "floor within 32..100");
            generatedRotations.add(piece.getRotation());
            String name = piece.getElement().toString();
            selected.add(name);
            var serialization = StructurePieceSerializationContext.fromLevel(level);
            var restored = new PoolElementStructurePiece(serialization, piece.createTag(serialization));
            require(restored.getPosition().equals(piece.getPosition()) && restored.getRotation() == piece.getRotation()
                    && restored.getElement().toString().equals(name), "vanilla piece persistence");
            var box = piece.getBoundingBox();
            int support = 0;
            int open = 0;
            for (int x : new int[]{box.minX(), (box.minX()+box.maxX()) >> 1, box.maxX()})
                for (int z : new int[]{box.minZ(), (box.minZ()+box.maxZ()) >> 1, box.maxZ()}) {
                    var column = generator.getBaseColumn(x,z,level,level.getChunkSource().randomState());
                    var base = column.getBlock(box.minY());
                    if (base.canOcclude() && base.getFluidState().isEmpty() && !base.is(Blocks.BEDROCK)) support++;
                    for (int y = box.minY()+1; y <= box.maxY(); y++)
                        if (column.getBlock(y).isAir()) open++;
                }
            require(support >= 7 && open * 4 >= 9 * (box.getYSpan()-1) * 3, "real terrain support and open space");
            for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                var column = generator.getBaseColumn(x,z,level,level.getChunkSource().randomState());
                for (int y = box.minY()+1; y <= box.maxY(); y++)
                    require(column.getBlock(y).getFluidState().isEmpty(), "no predicted lava inside body volume");
            }
            valid++;
        }
        require(valid > 0 && rejected > 0, "real terrain accepted and unsuitable sites skipped");
        require(selected.size() == 5 && generatedRotations.size() == 4, "all five templates and rotations generate");
        var located = generator.findNearestMapStructure(level, HolderSet.direct(holder), BlockPos.ZERO, 100, false);
        require(located != null, "natural Nether locate");
        var chunk = new ChunkPos(located.getFirst());
        var natural = level.getChunk(chunk.x, chunk.z).getStartForStructure(holder.value());
        require(natural != null && natural.isValid(), "located structure has real generated start");
        var piece = (PoolElementStructurePiece) natural.getPieces().getFirst();
        int chests = 0;
        for (var pos : BlockPos.betweenClosed(piece.getBoundingBox().minX(), piece.getBoundingBox().minY(), piece.getBoundingBox().minZ(),
                piece.getBoundingBox().maxX(), piece.getBoundingBox().maxY(), piece.getBoundingBox().maxZ())) {
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity) { checkChest(level, pos); chests++; }
        }
        require(chests == 1, "natural structure retains one usable loot chest");
        return "Nether gateways: " + assertions + " assertions, five original templates/20 rotations and loot chests; "
                + valid + "/160 terrain candidates valid; five biomes; 40/15 half-frequency (" + eligible
                + "/4096); vanilla piece persistence; natural locate at " + located.getFirst();
    }

    private static void checkTerrain() {
        var box = new BoundingBox(0,0,0,9,0,9);
        var chest = List.of(new BlockPos(3,1,7));
        for (int floor : new int[]{32,40,100})
            require(find(box,chest,(x,z)->flat(floor,Blocks.NETHERRACK.defaultBlockState())).orElse(-1) == floor, "flat floor " + floor);
        for (int floor : new int[]{31,101})
            require(find(box,chest,(x,z)->flat(floor,Blocks.NETHERRACK.defaultBlockState())).isEmpty(), "outside height range " + floor);
        require(find(box,chest,(x,z)->flat(40,Blocks.BEDROCK.defaultBlockState())).isEmpty(), "bedrock rejected");
        require(find(box,chest,(x,z)->flat(40,Blocks.LAVA.defaultBlockState())).isEmpty(), "lava lake rejected");
        require(find(box,chest,(x,z)->flat(127,Blocks.NETHERRACK.defaultBlockState())).isEmpty(), "fully buried site rejected");
        require(find(box,chest,(x,z)->x==0&&(z==0||z==9)?flat(-1,Blocks.NETHERRACK.defaultBlockState()):flat(40,Blocks.NETHERRACK.defaultBlockState())).orElse(-1)==40,
                "two unsupported edge samples allowed");
        require(find(box,chest,(x,z)->x==0?flat(-1,Blocks.NETHERRACK.defaultBlockState()):flat(40,Blocks.NETHERRACK.defaultBlockState())).isEmpty(),
                "three unsupported samples rejected");
        require(find(box,chest,(x,z)->{
            var column=flat(40,Blocks.NETHERRACK.defaultBlockState());
            if(x==1&&z==1) column.setBlock(42,Blocks.LAVA.defaultBlockState());
            return column;
        }).isEmpty(), "lava between support samples rejected");
        require(find(box,chest,(x,z)->{
            var column=flat(40,Blocks.NETHERRACK.defaultBlockState());
            if(x==3&&z==7) column.setBlock(42,Blocks.NETHERRACK.defaultBlockState());
            return column;
        }).isEmpty(), "blocked chest lid rejected");
        require(find(box,chest,(x,z)->{
            var column=flat(40,Blocks.NETHERRACK.defaultBlockState());
            if((x==2&&z==7)||(x==4&&z==7)||(x==3&&z==6)||(x==3&&z==8)) {
                column.setBlock(41,Blocks.NETHERRACK.defaultBlockState());
                column.setBlock(42,Blocks.NETHERRACK.defaultBlockState());
            }
            return column;
        }).isEmpty(), "all chest entrances blocked");
        require(NetherGatewayTerrain.findFloor(box,8,chest,0,35,RandomSource.create(42),
                (x,z)->flat(32,Blocks.NETHERRACK.defaultBlockState())).isEmpty(), "build height bounds enforced");
        var columns = new HashSet<Long>();
        require(find(box,chest,(x,z)->{
            require(columns.add(BlockPos.asLong(x,0,z)), "each terrain column requested only once");
            return flat(40,Blocks.NETHERRACK.defaultBlockState());
        }).isPresent(), "column cache");
        require(columns.size()<=104, "bounded footprint scan");
    }

    private static java.util.OptionalInt find(BoundingBox box, List<BlockPos> chests, NetherGatewayTerrain.Columns columns) {
        return NetherGatewayTerrain.findFloor(box,8,chests,0,128,RandomSource.create(42),columns);
    }

    private static NoiseColumn flat(int floor, BlockState base) {
        var states = new BlockState[128];
        for(int y=0;y<states.length;y++) states[y]=y<=floor?base:Blocks.AIR.defaultBlockState();
        return new NoiseColumn(0,states);
    }

    private static ResourceLocation templateId(int i) {
        return ResourceLocation.parse("mathmaster:ruins/unfinished_nether_gateway_"+i);
    }

    private static void checkChest(ServerLevel level, BlockPos pos) {
        require(level.getBlockEntity(pos) instanceof ChestBlockEntity, "chest entity present");
        var chest = (ChestBlockEntity) level.getBlockEntity(pos);
        require(chest.getLootTable()!=null && chest.getLootTable().location().equals(LOOT), "independent Nether loot reference");
        chest.unpackLootTable(null);
        boolean hasLoot = false;
        for(int slot=0;slot<chest.getContainerSize();slot++) hasLoot |= !chest.getItem(slot).isEmpty();
        require(hasLoot, "actual loot unpack succeeds");
    }

    private static void require(boolean ok, String message) {
        assertions++;
        if(!ok) throw new AssertionError("Nether gateway: " + message);
    }
}
