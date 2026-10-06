package com.freshfish.mathmaster.worldgen;

import com.freshfish.mathmaster.init.ModStructures;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** Original templates on open Nether floors, serialized using vanilla pool-element pieces. */
public final class UnfinishedNetherGatewayStructure extends Structure {
    public static final MapCodec<UnfinishedNetherGatewayStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(settingsCodec(instance), ResourceLocation.CODEC.listOf()
                    .validate(values -> values.isEmpty()
                            ? DataResult.error(() -> "Nether gateway needs at least one template") : DataResult.success(values))
                    .fieldOf("templates").forGetter(structure -> structure.templates))
                    .apply(instance, UnfinishedNetherGatewayStructure::new));
    private final List<ResourceLocation> templates;

    public UnfinishedNetherGatewayStructure(StructureSettings settings, List<ResourceLocation> templates) {
        super(settings);
        this.templates = List.copyOf(templates);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ResourceLocation id = templates.get(context.random().nextInt(templates.size()));
        var manager = context.structureTemplateManager();
        var template = manager.get(id);
        if (template.isEmpty()) return Optional.empty();
        Rotation rotation = Rotation.getRandom(context.random());
        var element = StructurePoolElement.single(id.toString()).apply(StructureTemplatePool.Projection.RIGID);
        var localBox = element.getBoundingBox(manager, BlockPos.ZERO, rotation);
        int x = context.chunkPos().getMiddleBlockX() - ((localBox.minX() + localBox.maxX()) >> 1);
        int z = context.chunkPos().getMiddleBlockZ() - ((localBox.minZ() + localBox.maxZ()) >> 1);
        BlockPos horizontalOrigin = new BlockPos(x, 0, z);
        var footprint = element.getBoundingBox(manager, horizontalOrigin, rotation);
        var settings = new StructurePlaceSettings().setRotation(rotation);
        var chests = template.get().filterBlocks(horizontalOrigin, settings, Blocks.CHEST).stream()
                .map(info -> info.pos()).toList();
        if (chests.isEmpty()) return Optional.empty();
        var floor = NetherGatewayTerrain.findFloor(footprint, template.get().getSize().getY(), chests,
                context.heightAccessor().getMinBuildHeight(), context.heightAccessor().getMaxBuildHeight(),
                context.random(), (columnX, columnZ) -> context.chunkGenerator().getBaseColumn(
                        columnX, columnZ, context.heightAccessor(), context.randomState()));
        if (floor.isEmpty()) return Optional.empty();
        BlockPos origin = new BlockPos(x, floor.getAsInt(), z);
        var box = element.getBoundingBox(manager, origin, rotation);
        BlockPos center = new BlockPos(context.chunkPos().getMiddleBlockX(), origin.getY() + 1,
                context.chunkPos().getMiddleBlockZ());
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new PoolElementStructurePiece(
                manager, element, origin, 0, rotation, box, LiquidSettings.IGNORE_WATERLOGGING))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.UNFINISHED_NETHER_GATEWAY.get();
    }
}
