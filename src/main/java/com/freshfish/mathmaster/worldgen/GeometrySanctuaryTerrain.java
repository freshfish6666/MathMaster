package com.freshfish.mathmaster.worldgen;

import java.util.Arrays;
import java.util.OptionalInt;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Nine predicted terrain columns; no neighboring chunks are loaded during start selection. */
public final class GeometrySanctuaryTerrain {
    public static final int MAX_HEIGHT_DIFFERENCE = 6;

    @FunctionalInterface public interface Columns { NoiseColumn get(int x, int z); }
    private GeometrySanctuaryTerrain() {}

    public static OptionalInt findFloor(BoundingBox footprint, int templateHeight,
                                       int minBuildY, int maxBuildY, Columns columns) {
        if (templateHeight <= 0 || footprint.getXSpan() < 3 || footprint.getZSpan() < 3) return OptionalInt.empty();
        int[] heights = new int[9];
        int index = 0;
        for (int x : new int[]{footprint.minX(), (footprint.minX() + footprint.maxX()) >> 1, footprint.maxX()}) {
            for (int z : new int[]{footprint.minZ(), (footprint.minZ() + footprint.maxZ()) >> 1, footprint.maxZ()}) {
                var column = columns.get(x, z);
                int y = maxBuildY - 1;
                while (y >= minBuildY && column.getBlock(y).isAir()) y--;
                if (y < minBuildY) return OptionalInt.empty();
                var surface = column.getBlock(y);
                if (!surface.getFluidState().isEmpty() || !surface.canOcclude()) return OptionalInt.empty();
                heights[index++] = y;
            }
        }
        Arrays.sort(heights);
        if (heights[8] - heights[0] > MAX_HEIGHT_DIFFERENCE) return OptionalInt.empty();
        // Match existing trials' one-block embedding, using the median to tolerate small slopes.
        int floor = heights[4];
        if (floor < minBuildY || floor + templateHeight > maxBuildY) return OptionalInt.empty();
        return OptionalInt.of(floor);
    }
}
