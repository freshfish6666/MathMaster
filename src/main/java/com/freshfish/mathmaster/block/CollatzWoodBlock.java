package com.freshfish.mathmaster.block;

import com.freshfish.mathmaster.tree.CollatzTreeGrowth;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Root/node removal permanently cancels the associated active tree. */
public final class CollatzWoodBlock extends Block {
    public static final MapCodec<CollatzWoodBlock> CODEC = simpleCodec(CollatzWoodBlock::new);

    public CollatzWoodBlock(Properties properties) { super(properties); }

    @Override
    protected MapCodec<CollatzWoodBlock> codec() { return CODEC; }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock())) CollatzTreeGrowth.removed(level, pos);
        super.onRemove(state, level, pos, replacement, moved);
    }
}
