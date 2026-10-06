package com.freshfish.mathmaster.tree;

import com.freshfish.mathmaster.init.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The existing root block determines the palette, including after loading old tree saves. */
public enum CollatzTreeFamily {
    RED, PURPLE, BLUE;

    public Block sapling() { return switch(this) {
        case RED -> ModBlocks.COLLATZ_SAPLING.get();
        case PURPLE -> ModBlocks.PURPLE_COLLATZ_SAPLING.get();
        case BLUE -> ModBlocks.BLUE_COLLATZ_SAPLING.get();
    }; }
    public Block root() { return switch(this) {
        case RED -> ModBlocks.COLLATZ_ROOT.get();
        case PURPLE -> ModBlocks.PURPLE_COLLATZ_ROOT.get();
        case BLUE -> ModBlocks.BLUE_COLLATZ_ROOT.get();
    }; }
    public Block log() { return switch(this) {
        case RED -> ModBlocks.COLLATZ_LOG.get();
        case PURPLE -> ModBlocks.PURPLE_COLLATZ_LOG.get();
        case BLUE -> ModBlocks.BLUE_COLLATZ_LOG.get();
    }; }
    public Block node() { return switch(this) {
        case RED -> ModBlocks.COLLATZ_NODE.get();
        case PURPLE -> ModBlocks.PURPLE_COLLATZ_NODE.get();
        case BLUE -> ModBlocks.BLUE_COLLATZ_NODE.get();
    }; }
    public Block leaves(boolean fruit) { return switch(this) {
        case RED -> (fruit ? ModBlocks.FRUITING_COLLATZ_LEAVES : ModBlocks.COLLATZ_LEAVES).get();
        case PURPLE -> (fruit ? ModBlocks.PURPLE_FRUITING_COLLATZ_LEAVES : ModBlocks.PURPLE_COLLATZ_LEAVES).get();
        case BLUE -> (fruit ? ModBlocks.BLUE_FRUITING_COLLATZ_LEAVES : ModBlocks.BLUE_COLLATZ_LEAVES).get();
    }; }
    public static CollatzTreeFamily fromSapling(BlockState state) {
        for(var family : values()) if(state.is(family.sapling())) return family;
        return null;
    }
    public static CollatzTreeFamily fromRoot(BlockState state) {
        for(var family : values()) if(state.is(family.root())) return family;
        return null;
    }
}
