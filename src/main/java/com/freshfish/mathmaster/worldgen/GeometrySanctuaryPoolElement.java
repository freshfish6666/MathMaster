package com.freshfish.mathmaster.worldgen;

import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModStructures;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.event.EventHooks;

/** Vanilla placement and piece persistence, with an explicit, bounded DATA marker handler. */
public final class GeometrySanctuaryPoolElement extends SinglePoolElement {
    public static final String CONSTRUCT_MARKER = "mathmaster:geometry_construct";
    public static final MapCodec<GeometrySanctuaryPoolElement> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(templateCodec(), processorsCodec(), projectionCodec(), overrideLiquidSettingsCodec())
                    .apply(instance, GeometrySanctuaryPoolElement::new));

    private GeometrySanctuaryPoolElement(Either<ResourceLocation, StructureTemplate> template,
                                         Holder<StructureProcessorList> processors, StructureTemplatePool.Projection projection,
                                         Optional<LiquidSettings> liquids) {
        super(template, processors, projection, liquids);
    }

    public static GeometrySanctuaryPoolElement create(ResourceLocation template) {
        return new GeometrySanctuaryPoolElement(Either.left(template), Holder.direct(new StructureProcessorList(List.of())),
                StructureTemplatePool.Projection.RIGID, Optional.empty());
    }

    @Override public StructurePoolElementType<?> getType() { return ModStructures.GEOMETRY_SANCTUARY_ELEMENT.get(); }

    @Override public List<StructureTemplate.StructureBlockInfo> getDataMarkers(StructureTemplateManager manager,
            BlockPos origin, Rotation rotation, boolean relativePosition) {
        // SinglePoolElement runs markers through its STRUCTURE_BLOCK ignore processor too.
        // An air placeholder preserves metadata through that pass without placing a structure block.
        return super.getDataMarkers(manager, origin, rotation, relativePosition).stream()
                .map(info -> new StructureTemplate.StructureBlockInfo(info.pos(), Blocks.AIR.defaultBlockState(), info.nbt()))
                .toList();
    }

    @Override public void handleDataMarker(LevelAccessor level, StructureTemplate.StructureBlockInfo info,
                                           BlockPos origin, Rotation rotation, RandomSource random, BoundingBox box) {
        if (!box.isInside(info.pos()) || info.nbt() == null
                || !CONSTRUCT_MARKER.equals(info.nbt().getString("metadata"))
                || !(level instanceof ServerLevelAccessor serverLevel)) return;
        // The placeholder is skipped by vanilla placement; clear any original terrain at this point.
        level.setBlock(info.pos(), Blocks.AIR.defaultBlockState(), 18);
        var mob = ModEntities.GEOMETRY_CONSTRUCT.get().create(serverLevel.getLevel());
        if (mob == null) return;
        mob.moveTo(info.pos().getX() + .5, info.pos().getY(), info.pos().getZ() + .5,
                rotation.rotate(net.minecraft.core.Direction.SOUTH).toYRot(), 0);
        EventHooks.finalizeMobSpawn(mob, serverLevel, serverLevel.getCurrentDifficultyAt(info.pos()), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        serverLevel.addFreshEntityWithPassengers(mob);
    }
}
