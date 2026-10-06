package com.freshfish.mathmaster.worldgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Tests predicted terrain only; never requests neighboring chunks during structure generation. */
public final class NetherGatewayTerrain {
    public static final int MIN_FLOOR_Y = 32;
    public static final int MAX_FLOOR_Y = 100;

    @FunctionalInterface
    public interface Columns {
        NoiseColumn get(int x, int z);
    }

    private NetherGatewayTerrain() {}

    public static OptionalInt findFloor(BoundingBox footprint, int height, List<BlockPos> localChests,
                                       int minBuildY, int maxBuildY, RandomSource random, Columns columns) {
        int minY = Math.max(MIN_FLOOR_Y, minBuildY);
        int maxY = Math.min(MAX_FLOOR_Y, maxBuildY - height);
        if (height < 4 || minY > maxY) return OptionalInt.empty();
        var cache = new HashMap<Long, NoiseColumn>();
        int[] xs = {footprint.minX(), (footprint.minX() + footprint.maxX()) >> 1, footprint.maxX()};
        int[] zs = {footprint.minZ(), (footprint.minZ() + footprint.maxZ()) >> 1, footprint.maxZ()};
        var samples = new NoiseColumn[9];
        int index = 0;
        for (int x : xs) for (int z : zs) samples[index++] = column(cache, columns, x, z);
        // Start at a random height and search down as vanilla does, then inspect the remaining upper floors.
        int startY = minY + random.nextInt(maxY - minY + 1);
        for (int offset = 0; offset <= maxY - minY; offset++) {
            int floorY = startY - offset;
            if (floorY < minY) floorY += maxY - minY + 1;
            int support = 0;
            int open = 0;
            boolean liquid = false;
            for (NoiseColumn sample : samples) {
                if (supports(sample.getBlock(floorY))) support++;
                for (int dy = 1; dy < height; dy++) {
                    BlockState state = sample.getBlock(floorY + dy);
                    if (clear(state)) open++;
                    if (!state.getFluidState().isEmpty()) liquid = true;
                }
            }
            // Seven of nine supports and >=75% open sample space permit imperfect slopes and embedded edges.
            if (support < 7 || liquid || open * 4 < 9 * (height - 1) * 3) continue;
            if (!clear(samples[4].getBlock(floorY + 1))
                    || !clear(samples[4].getBlock(floorY + 2))
                    || !clear(samples[4].getBlock(floorY + 3))) continue;
            boolean accessible = true;
            for (BlockPos chest : localChests) {
                int chestY = floorY + chest.getY();
                NoiseColumn chestColumn = column(cache, columns, chest.getX(), chest.getZ());
                if (!clear(chestColumn.getBlock(chestY)) || !clear(chestColumn.getBlock(chestY + 1))) {
                    accessible = false;
                    break;
                }
                boolean entrance = false;
                for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    NoiseColumn adjacent = column(cache, columns,
                            chest.getX() + direction.getStepX(), chest.getZ() + direction.getStepZ());
                    if (clear(adjacent.getBlock(chestY)) && clear(adjacent.getBlock(chestY + 1))) entrance = true;
                }
                if (!entrance) {
                    accessible = false;
                    break;
                }
            }
            // A narrow lava column can fall between the nine samples; check the complete above-base volume.
            if (accessible) {
                for (int x = footprint.minX(); x <= footprint.maxX() && accessible; x++) {
                    for (int z = footprint.minZ(); z <= footprint.maxZ() && accessible; z++) {
                        NoiseColumn column = column(cache, columns, x, z);
                        for (int dy = 1; dy < height; dy++) {
                            if (!column.getBlock(floorY + dy).getFluidState().isEmpty()) {
                                accessible = false;
                                break;
                            }
                        }
                    }
                }
                if (accessible) return OptionalInt.of(floorY);
            }
        }
        return OptionalInt.empty();
    }

    private static NoiseColumn column(Map<Long, NoiseColumn> cache, Columns columns, int x, int z) {
        return cache.computeIfAbsent(BlockPos.asLong(x, 0, z), ignored -> columns.get(x, z));
    }

    private static boolean supports(BlockState state) {
        return state.canOcclude() && state.getFluidState().isEmpty() && !state.is(Blocks.BEDROCK);
    }

    private static boolean clear(BlockState state) {
        return state.getFluidState().isEmpty() && (state.isAir() || state.canBeReplaced());
    }
}
