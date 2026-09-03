package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, MathMaster.MODID);

    public static final Supplier<Block> LINGXU_BLOCK =
            BLOCKS.register("lingxu_block",
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK)));

    private ModBlocks() {
    }
}
