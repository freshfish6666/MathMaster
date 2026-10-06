package com.freshfish.mathmaster.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One dimension-local controller per tree, with independent saved growing branches. */
public final class CollatzTreeSavedData extends SavedData {
    private static final Factory<CollatzTreeSavedData> FACTORY = new Factory<>(CollatzTreeSavedData::new, CollatzTreeSavedData::load);
    private final Map<BlockPos, Tree> trees = new LinkedHashMap<>();

    public static CollatzTreeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "mathmaster_collatz_trees");
    }

    public void start(BlockPos root, int initial, long now) {
        Tree tree = new Tree(root);
        Branch branch = new Branch(root, initial, Direction.UP);
        branch.tip = root.above(); branch.remaining--; branch.segment.add(branch.tip);
        branch.nextTick = now + CollatzTreeGrowth.LOG_INTERVAL_TICKS;
        tree.branches.add(branch); tree.wood.add(root); tree.wood.add(branch.tip); tree.total = 1;
        trees.put(root, tree); setDirty();
    }

    public boolean isGrowing(BlockPos root) { return trees.containsKey(root); }

    public void removeAt(BlockPos pos) {
        if (trees.values().removeIf(t -> t.wood.contains(pos))) setDirty();
    }

    public void tick(ServerLevel level) {
        long now = level.getGameTime();
        var iterator = trees.values().iterator();
        while (iterator.hasNext()) {
            Tree tree = iterator.next();
            if (!level.hasChunkAt(tree.root)) {
                for (Branch b : tree.branches) pause(b, now);
                continue;
            }
            var family = CollatzTreeFamily.fromRoot(level.getBlockState(tree.root));
            if (family == null) {
                iterator.remove(); setDirty(); continue;
            }
            var children = new ArrayList<Branch>();
            var branches = tree.branches.iterator();
            boolean invalid = false;
            while (branches.hasNext()) {
                Branch b = branches.next();
                if (!level.hasChunkAt(b.node) || b.segment.stream().anyMatch(p -> !level.hasChunkAt(p))) {
                    pause(b, now); continue;
                }
                if (b.pausedAt >= 0) {
                    b.nextTick += Math.max(0, now - b.pausedAt); b.pausedAt = -1; setDirty();
                }
                if (now < b.nextTick) continue;
                if ((!b.node.equals(tree.root) && !level.getBlockState(b.node).is(family.node()))
                        || b.segment.stream().anyMatch(p -> !level.getBlockState(p).is(family.log()))) {
                    invalid = true; break;
                }
                if (b.remaining == -1) {
                    int count = CollatzTreeGrowth.forkCount(level.random.nextInt(100), tree.forkBudget);
                    tree.forkBudget -= count - 1;
                    var directions = new ArrayList<Direction>();
                    for (Direction d : Direction.values()) if (d != b.direction.getOpposite()) directions.add(d);
                    for (int i = directions.size()-1; i > 0; i--) java.util.Collections.swap(directions, i, level.random.nextInt(i+1));
                    int value = b.value % 2 == 0 ? b.value / 2 : b.value * 3 + 1;
                    for (int i=0; i<count; i++) {
                        Branch child = new Branch(b.node, value, directions.get(i));
                        child.firstStep = count > 1; child.nextTick = now;
                        if (!advance(level, tree, child, now, family)) children.add(child);
                    }
                    branches.remove(); setDirty();
                } else if (advance(level, tree, b, now, family)) branches.remove();
            }
            tree.branches.addAll(children);
            if (invalid || tree.branches.isEmpty() || tree.total >= CollatzTreeGrowth.MAX_LOGS) {
                iterator.remove(); setDirty();
            }
        }
    }

    private void pause(Branch b, long now) {
        if (b.pausedAt < 0) { b.pausedAt = now; setDirty(); }
    }

    /** Returns true when this branch has reached one and produced its terminal node. */
    private boolean advance(ServerLevel level, Tree tree, Branch b, long now, CollatzTreeFamily family) {
        b.nextTick = now + CollatzTreeGrowth.LOG_INTERVAL_TICKS;
        setDirty();
        if (tree.total >= CollatzTreeGrowth.MAX_LOGS && b.remaining > 0) return false;
        Direction direction = CollatzTreeGrowth.choose(level, tree, b);
        if (direction == null) return false;
        b.firstStep = false; b.direction = direction; b.tip = b.tip.relative(direction);
        tree.wood.add(b.tip);
        if (b.remaining > 0) {
            CollatzTreeGrowth.log(level, b.tip, direction, family); b.segment.add(b.tip); b.remaining--; tree.total++;
            if (tree.total % 3 == 0) CollatzTreeGrowth.leaves(level, b.tip, direction, false, family);
        } else {
            level.setBlockAndUpdate(b.tip, family.node().defaultBlockState()); b.node = b.tip;
            if (b.value == 1) {
                CollatzTreeGrowth.leaves(level, b.node, direction, true, family); tree.completed++; return true;
            }
            b.remaining = -1; b.nextTick = now + CollatzTreeGrowth.NODE_WAIT_TICKS;
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Tree tree : trees.values()) {
            CompoundTag t = new CompoundTag();
            t.putLong("root", tree.root.asLong()); t.putInt("total", tree.total);
            t.putInt("fork_budget", tree.forkBudget); t.putInt("completed", tree.completed);
            t.putLongArray("wood", tree.wood.stream().mapToLong(BlockPos::asLong).toArray());
            ListTag branches = new ListTag();
            for (Branch b : tree.branches) branches.add(saveBranch(b));
            t.put("branches", branches);
            // Retain the former single-tip fields as a readable snapshot of the first active branch.
            if (!tree.branches.isEmpty()) t.merge(saveBranch(tree.branches.getFirst()));
            list.add(t);
        }
        tag.put("trees", list); return tag;
    }

    private static CompoundTag saveBranch(Branch b) {
        CompoundTag t = new CompoundTag();
        t.putLong("node", b.node.asLong()); t.putLong("tip", b.tip.asLong());
        t.putInt("value", b.value); t.putInt("remaining", b.remaining);
        t.putInt("direction", b.direction.ordinal()); t.putLong("next_tick", b.nextTick);
        t.putLong("paused_at", b.pausedAt); t.putBoolean("first_step", b.firstStep);
        t.putLongArray("segment", b.segment.stream().mapToLong(BlockPos::asLong).toArray());
        return t;
    }

    public static CollatzTreeSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        var data = new CollatzTreeSavedData();
        var list = tag.getList("trees", Tag.TAG_COMPOUND);
        for (int i=0; i<list.size(); i++) {
            var t = list.getCompound(i);
            Tree tree = new Tree(BlockPos.of(t.getLong("root")));
            tree.total = t.getInt("total");
            tree.forkBudget = t.contains("fork_budget") ? Math.clamp(t.getInt("fork_budget"), 0, 49) : 49;
            tree.completed = Math.clamp(t.getInt("completed"), 0, 50);
            for (long pos : t.getLongArray("wood")) tree.wood.add(BlockPos.of(pos));
            if (t.contains("branches", Tag.TAG_LIST)) {
                var branches = t.getList("branches", Tag.TAG_COMPOUND);
                for (int j=0; j<branches.size(); j++) {
                    Branch b = loadBranch(branches.getCompound(j));
                    if (b != null) tree.branches.add(b);
                }
            } else {
                // Migrate existing single-branch trees without resetting their current segment or timer.
                Branch b = loadBranch(t); if (b != null) tree.branches.add(b);
            }
            if (tree.total < 1 || tree.total > CollatzTreeGrowth.MAX_LOGS || tree.branches.isEmpty()
                    || tree.branches.size() + tree.completed > 50 - tree.forkBudget) continue;
            data.trees.put(tree.root, tree);
        }
        return data;
    }

    private static Branch loadBranch(CompoundTag t) {
        int value = t.getInt("value"), remaining = t.getInt("remaining");
        if (value < 1 || value > CollatzTreeGrowth.MAX_LOGS || remaining < -1 || remaining > value) return null;
        Branch b = new Branch(BlockPos.of(t.getLong("node")), value, Direction.from3DDataValue(t.getInt("direction")));
        b.tip = BlockPos.of(t.getLong("tip")); b.remaining = remaining; b.nextTick = t.getLong("next_tick");
        b.pausedAt = t.contains("paused_at") ? t.getLong("paused_at") : -1; b.firstStep = t.getBoolean("first_step");
        for (long pos : t.getLongArray("segment")) b.segment.add(BlockPos.of(pos));
        return b;
    }

    static final class Tree {
        final BlockPos root;
        int total, forkBudget = CollatzTreeGrowth.MAX_ENDPOINTS - 1, completed;
        final List<Branch> branches = new ArrayList<>();
        final Set<BlockPos> wood = new HashSet<>();
        Tree(BlockPos root) { this.root = root.immutable(); }
    }

    static final class Branch {
        BlockPos node, tip;
        int value, remaining;
        long nextTick, pausedAt = -1;
        Direction direction;
        boolean firstStep;
        final Set<BlockPos> segment = new HashSet<>();
        Branch(BlockPos node, int value, Direction direction) {
            this.node = node.immutable(); this.tip = this.node; this.value = value; this.remaining = value; this.direction = direction;
        }
    }
}
