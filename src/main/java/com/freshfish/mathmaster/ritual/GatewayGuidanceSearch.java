package com.freshfish.mathmaster.ritual;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Server-thread search: only one STRUCTURE_STARTS request at a time, never a blocking join. */
final class GatewayGuidanceSearch {
    static final int RADIUS = 4096;
    static final List<ResourceLocation> TARGETS = List.of(
            ResourceLocation.fromNamespaceAndPath("mathmaster", "ruins/unfinished_overworld_gateway"),
            ResourceLocation.fromNamespaceAndPath("mathmaster", "ruins/unfinished_overworld_giant_gateway"));
    private final ServerLevel level;
    private final List<Candidate> candidates;
    private int index;
    private CompletableFuture<ChunkResult<ChunkAccess>> future;
    private BlockPos result;

    GatewayGuidanceSearch(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.candidates = candidates(level, origin);
    }

    static List<Candidate> candidates(ServerLevel level, BlockPos origin) {
        var state = level.getChunkSource().getGeneratorState();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Candidate> result = new ArrayList<>();
        for (ResourceLocation id : TARGETS) {
            var holder = registry.getHolder(id).orElse(null);
            if (holder == null) continue;
            for (var placement : state.getPlacementsForStructure(holder)) {
                if (!(placement instanceof RandomSpreadStructurePlacement spread)) {
                    MathMaster.LOGGER.warn("Unsupported gateway guidance placement for {}", id);
                    continue;
                }
                int spacing = spread.spacing();
                int minX = Math.floorDiv(Math.floorDiv(origin.getX() - RADIUS, 16), spacing);
                int maxX = Math.floorDiv(Math.floorDiv(origin.getX() + RADIUS, 16), spacing);
                int minZ = Math.floorDiv(Math.floorDiv(origin.getZ() - RADIUS, 16), spacing);
                int maxZ = Math.floorDiv(Math.floorDiv(origin.getZ() + RADIUS, 16), spacing);
                for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                    ChunkPos chunk = spread.getPotentialStructureChunk(state.getLevelSeed(), x * spacing, z * spacing);
                    BlockPos locate = spread.getLocatePos(chunk);
                    long dx = (long) locate.getX() - origin.getX(), dz = (long) locate.getZ() - origin.getZ();
                    long distance = dx * dx + dz * dz;
                    if (distance <= (long) RADIUS * RADIUS && spread.isStructureChunk(state, chunk.x, chunk.z)) {
                        result.add(new Candidate(chunk, holder, locate, distance));
                    }
                }
            }
        }
        result.sort(Comparator.comparingLong(Candidate::distance)
                .thenComparing(candidate -> candidate.structure().unwrapKey().orElseThrow().location().toString())
                .thenComparingInt(candidate -> candidate.chunk().x).thenComparingInt(candidate -> candidate.chunk().z));
        return List.copyOf(result);
    }

    boolean advance() {
        if (index >= candidates.size()) return true;
        Candidate candidate = candidates.get(index);
        if (future == null) {
            future = level.getChunkSource().getChunkFuture(candidate.chunk().x, candidate.chunk().z,
                    ChunkStatus.STRUCTURE_STARTS, true);
            return false;
        }
        if (!future.isDone()) return false;
        ChunkAccess chunk = future.getNow(null).orElse(null);
        future = null;
        index++;
        if (chunk != null) {
            var start = chunk.getStartForStructure(candidate.structure().value());
            // Already explored starts remain eligible; do not increment structure references.
            if (start != null && start.isValid()) {
                result = candidate.locate();
                return true;
            }
        }
        return index >= candidates.size();
    }

    BlockPos result() { return result; }
    record Candidate(ChunkPos chunk, Holder<Structure> structure, BlockPos locate, long distance) {}
}
