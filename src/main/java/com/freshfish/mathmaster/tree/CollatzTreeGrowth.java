package com.freshfish.mathmaster.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** One saved controller per tree; ordinary wood and leaves do not tick independently. */
public final class CollatzTreeGrowth {
    public static final int NODE_WAIT_TICKS = 40;
    public static final int LOG_INTERVAL_TICKS = 4;
    public static final int MAX_LOGS = 16384;
    public static final int MAX_ENDPOINTS = 50;
    public static final int MAX_RADIUS = 96;

    @SubscribeEvent
    public void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) CollatzTreeSavedData.get(level).tick(level);
    }

    public static void removed(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) CollatzTreeSavedData.get(server).removeAt(pos);
    }

    public static void root(ServerLevel level, BlockPos sapling) {
        var family = CollatzTreeFamily.fromSapling(level.getBlockState(sapling));
        if (family == null
                || !level.getBlockState(sapling).canSurvive(level, sapling)) return;
        // Starting only replaces the supporting soil and the sapling itself.
        var soil = level.getBlockState(sapling.below());
        if (!soil.is(net.minecraft.tags.BlockTags.DIRT)
                && !(soil.getBlock() instanceof net.minecraft.world.level.block.FarmBlock)) return;
        // Replace the sapling first, avoiding a soil-change survival update dropping a duplicate sapling.
        level.setBlockAndUpdate(sapling, family.log().defaultBlockState());
        level.setBlockAndUpdate(sapling.below(), family.root().defaultBlockState());
        CollatzTreeSavedData.get(level).start(sapling.below(), 5 + level.random.nextInt(6), level.getGameTime());
    }

    static Direction choose(ServerLevel level, CollatzTreeSavedData.Tree tree, CollatzTreeSavedData.Branch branch) {
        if (branch.firstStep) {
            BlockPos next = branch.tip.relative(branch.direction);
            return available(level, tree.root, next) ? branch.direction : null;
        }
        var directions = new java.util.ArrayList<Direction>();
        for (Direction d : Direction.values()) if (d != branch.direction.getOpposite()) directions.add(d);
        java.util.Collections.shuffle(directions, new java.util.Random(level.random.nextLong()));
        // The first segment starts vertically; later segments occasionally turn without an obstacle.
        if (branch.node.equals(tree.root) || level.random.nextFloat() >= 0.25F) {
            directions.remove(branch.direction);
            directions.addFirst(branch.direction);
        }
        for (Direction d : directions) {
            BlockPos next = branch.tip.relative(d);
            if (level.isOutsideBuildHeight(next) || !level.getWorldBorder().isWithinBounds(next)
                    || Math.abs(next.getX() - tree.root.getX()) > MAX_RADIUS
                    || Math.abs(next.getZ() - tree.root.getZ()) > MAX_RADIUS
                    || Math.abs(next.getY() - tree.root.getY()) > MAX_RADIUS) continue;
            // Never force-load a chunk or reroute merely because that chunk is unloaded.
            if (!level.hasChunkAt(next)) return null;
            if (level.getBlockState(next).isAir()) return d;
        }
        return null;
    }

    private static boolean available(ServerLevel level, BlockPos root, BlockPos next) {
        return !level.isOutsideBuildHeight(next) && level.getWorldBorder().isWithinBounds(next)
                && Math.abs(next.getX()-root.getX()) <= MAX_RADIUS
                && Math.abs(next.getY()-root.getY()) <= MAX_RADIUS
                && Math.abs(next.getZ()-root.getZ()) <= MAX_RADIUS
                && level.hasChunkAt(next) && level.getBlockState(next).isAir();
    }

    public static int forkCount(int roll, int budget) {
        int requested = roll < 60 ? 1 : roll < 90 ? 2 : 3;
        return Math.min(requested, 1 + budget);
    }

    static void leaves(ServerLevel level, BlockPos pos, Direction forward, boolean fruit, CollatzTreeFamily family) {
        for (Direction side : Direction.values()) {
            if (!fruit && (side == forward || side == forward.getOpposite() || level.random.nextInt(3) != 0)) continue;
            BlockPos leaf = pos.relative(side);
            if (!level.isOutsideBuildHeight(leaf) && level.getWorldBorder().isWithinBounds(leaf)
                    && level.hasChunkAt(leaf) && level.getBlockState(leaf).isAir()) {
                level.setBlockAndUpdate(leaf, family.leaves(fruit).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
            }
        }
    }

    static void log(ServerLevel level, BlockPos pos, Direction direction, CollatzTreeFamily family) {
        level.setBlockAndUpdate(pos, family.log().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, direction.getAxis()));
    }
}
