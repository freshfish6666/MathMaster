package com.freshfish.mathmaster.worldgen;

import com.freshfish.mathmaster.init.ModStructures;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/** Original sanctuary on moderately flat, dry Overworld terrain. */
public final class GeometrySanctuaryStructure extends Structure {
    public static final MapCodec<GeometrySanctuaryStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(settingsCodec(instance), ResourceLocation.CODEC.fieldOf("template")
                    .forGetter(structure -> structure.template)).apply(instance, GeometrySanctuaryStructure::new));
    private final ResourceLocation template;

    public GeometrySanctuaryStructure(StructureSettings settings, ResourceLocation template) {
        super(settings);
        this.template = template;
    }

    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var manager = context.structureTemplateManager();
        var original = manager.get(template);
        if (original.isEmpty()) return Optional.empty();
        var element = GeometrySanctuaryPoolElement.create(template);
        Rotation rotation = Rotation.getRandom(context.random());
        var localBox = element.getBoundingBox(manager, BlockPos.ZERO, rotation);
        int x = context.chunkPos().getMiddleBlockX() - ((localBox.minX() + localBox.maxX()) >> 1);
        int z = context.chunkPos().getMiddleBlockZ() - ((localBox.minZ() + localBox.maxZ()) >> 1);
        var footprint = element.getBoundingBox(manager, new BlockPos(x, 0, z), rotation);
        var floor = GeometrySanctuaryTerrain.findFloor(footprint, original.get().getSize().getY(),
                context.heightAccessor().getMinBuildHeight(), context.heightAccessor().getMaxBuildHeight(),
                (columnX, columnZ) -> context.chunkGenerator().getBaseColumn(columnX, columnZ,
                        context.heightAccessor(), context.randomState()));
        if (floor.isEmpty()) return Optional.empty();
        BlockPos origin = new BlockPos(x, floor.getAsInt(), z);
        var box = element.getBoundingBox(manager, origin, rotation);
        BlockPos center = new BlockPos(context.chunkPos().getMiddleBlockX(), origin.getY() + 3,
                context.chunkPos().getMiddleBlockZ());
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new PoolElementStructurePiece(
                manager, element, origin, 0, rotation, box, LiquidSettings.IGNORE_WATERLOGGING))));
    }

    @Override public StructureType<?> type() { return ModStructures.GEOMETRY_SANCTUARY.get(); }
}
