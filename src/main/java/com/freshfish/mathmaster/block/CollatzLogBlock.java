package com.freshfish.mathmaster.block;

import com.freshfish.mathmaster.tree.CollatzTreeGrowth;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class CollatzLogBlock extends RotatedPillarBlock {
    public static final MapCodec<CollatzLogBlock> CODEC = simpleCodec(CollatzLogBlock::new);

    public CollatzLogBlock(Properties properties) { super(properties); }

    @Override
    public MapCodec<CollatzLogBlock> codec() { return CODEC; }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock())) CollatzTreeGrowth.removed(level, pos);
        super.onRemove(state, level, pos, replacement, moved);
    }
}
