package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.block.AxiomDeductionTableBlock;
import com.freshfish.mathmaster.block.CollatzSaplingBlock;
import com.freshfish.mathmaster.block.CollatzWoodBlock;
import com.freshfish.mathmaster.block.CollatzLogBlock;
import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.block.GeometryAltarBlock;
import com.freshfish.mathmaster.block.DigitallyCorruptedBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import net.minecraft.world.level.block.LiquidBlock;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, MathMaster.MODID);

    public static final Supplier<Block> LINGXU_BLOCK =
            BLOCKS.register("lingxu_block",
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK)));

    public static final Supplier<Block> PRIME_BLOCK =
            BLOCKS.register("prime_block",
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK)));

    public static final Supplier<Block> AXIOM_DEDUCTION_TABLE =
            BLOCKS.register("axiom_deduction_table",
                    () -> new AxiomDeductionTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LECTERN)));

    public static final Supplier<Block> DIGITALLY_CORRUPTED_BLOCK =
            BLOCKS.register("digitally_corrupted_block",
                    () -> new DigitallyCorruptedBlock(
                            UniformInt.of(1, 5),
                            BlockBehaviour.Properties.ofFullCopy(Blocks.SCULK)
                    ));

    public static final Supplier<LiquidBlock> DIGITALLY_POLLUTED_WATER =
            BLOCKS.register("digitally_polluted_water", () -> new LiquidBlock(
                    ModFluids.DIGITALLY_POLLUTED_WATER.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));

    public static final Supplier<CollatzSaplingBlock> COLLATZ_SAPLING =
            BLOCKS.register("collatz_sapling", () -> new CollatzSaplingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH).randomTicks()));

    public static final Supplier<Block> COLLATZ_ROOT =
            BLOCKS.register("collatz_root", () -> new CollatzWoodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MANGROVE_ROOTS)));

    public static final Supplier<RotatedPillarBlock> COLLATZ_LOG =
            BLOCKS.register("collatz_log", () -> new CollatzLogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LOG)));

    public static final Supplier<Block> COLLATZ_NODE =
            BLOCKS.register("collatz_node", () -> new CollatzWoodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_WOOD)));

    public static final Supplier<LeavesBlock> COLLATZ_LEAVES =
            BLOCKS.register("collatz_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES)));

    public static final Supplier<LeavesBlock> FRUITING_COLLATZ_LEAVES =
            BLOCKS.register("fruiting_collatz_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES)));

    public static final Supplier<Block> COLLATZ_PLANKS =
            BLOCKS.register("collatz_planks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PLANKS)));

    public static final Supplier<Block> COLLATZ_SLAB =
            BLOCKS.register("collatz_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SLAB)));

    public static final Supplier<Block> COLLATZ_STAIRS =
            BLOCKS.register("collatz_stairs", () -> new StairBlock(COLLATZ_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_STAIRS)));

    public static final Supplier<Block> COLLATZ_FENCE =
            BLOCKS.register("collatz_fence", () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_FENCE)));

    public static final Supplier<Block> COLLATZ_FENCE_GATE =
            BLOCKS.register("collatz_fence_gate", () -> new FenceGateBlock(WoodType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_FENCE_GATE)));

    public static final Supplier<Block> COLLATZ_DOOR =
            BLOCKS.register("collatz_door", () -> new DoorBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_DOOR)));

    public static final Supplier<Block> COLLATZ_TRAPDOOR =
            BLOCKS.register("collatz_trapdoor", () -> new TrapDoorBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_TRAPDOOR)));

    public static final Supplier<Block> COLLATZ_BUTTON =
            BLOCKS.register("collatz_button", () -> new ButtonBlock(BlockSetType.CHERRY, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_BUTTON)));

    public static final Supplier<Block> COLLATZ_PRESSURE_PLATE =
            BLOCKS.register("collatz_pressure_plate", () -> new PressurePlateBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PRESSURE_PLATE)));

    public static final Supplier<NAltarBlock> N_ALTAR =
            BLOCKS.register("n_altar", () -> new NAltarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(3.0F, 6.0F)));

    public static final Supplier<CollatzSaplingBlock> PURPLE_COLLATZ_SAPLING =
            BLOCKS.register("purple_collatz_sapling", () -> new CollatzSaplingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH).randomTicks()));

    public static final Supplier<Block> PURPLE_COLLATZ_ROOT =
            BLOCKS.register("purple_collatz_root", () -> new CollatzWoodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MANGROVE_ROOTS)));

    public static final Supplier<RotatedPillarBlock> PURPLE_COLLATZ_LOG =
            BLOCKS.register("purple_collatz_log", () -> new CollatzLogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LOG)));

    public static final Supplier<Block> PURPLE_COLLATZ_NODE =
            BLOCKS.register("purple_collatz_node", () -> new CollatzWoodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_WOOD)));

    public static final Supplier<LeavesBlock> PURPLE_COLLATZ_LEAVES =
            BLOCKS.register("purple_collatz_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES)));

    public static final Supplier<LeavesBlock> PURPLE_FRUITING_COLLATZ_LEAVES =
            BLOCKS.register("purple_fruiting_collatz_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES)));

    public static final Supplier<Block> PURPLE_COLLATZ_PLANKS =
            BLOCKS.register("purple_collatz_planks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PLANKS)));

    public static final Supplier<Block> PURPLE_COLLATZ_SLAB =
            BLOCKS.register("purple_collatz_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SLAB)));

    public static final Supplier<Block> PURPLE_COLLATZ_STAIRS =
            BLOCKS.register("purple_collatz_stairs", () -> new StairBlock(PURPLE_COLLATZ_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_STAIRS)));

    public static final Supplier<Block> PURPLE_COLLATZ_FENCE =
            BLOCKS.register("purple_collatz_fence", () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_FENCE)));

    public static final Supplier<Block> PURPLE_COLLATZ_FENCE_GATE =
            BLOCKS.register("purple_collatz_fence_gate", () -> new FenceGateBlock(WoodType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_FENCE_GATE)));

    public static final Supplier<Block> PURPLE_COLLATZ_DOOR =
            BLOCKS.register("purple_collatz_door", () -> new DoorBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_DOOR)));

    public static final Supplier<Block> PURPLE_COLLATZ_TRAPDOOR =
            BLOCKS.register("purple_collatz_trapdoor", () -> new TrapDoorBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_TRAPDOOR)));

    public static final Supplier<Block> PURPLE_COLLATZ_BUTTON =
            BLOCKS.register("purple_collatz_button", () -> new ButtonBlock(BlockSetType.CHERRY, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_BUTTON)));

    public static final Supplier<Block> PURPLE_COLLATZ_PRESSURE_PLATE =
            BLOCKS.register("purple_collatz_pressure_plate", () -> new PressurePlateBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PRESSURE_PLATE)));

    public static final Supplier<CollatzSaplingBlock> BLUE_COLLATZ_SAPLING =
            BLOCKS.register("blue_collatz_sapling", () -> new CollatzSaplingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH).randomTicks()));

    public static final Supplier<Block> BLUE_COLLATZ_ROOT =
            BLOCKS.register("blue_collatz_root", () -> new CollatzWoodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MANGROVE_ROOTS)));

    public static final Supplier<RotatedPillarBlock> BLUE_COLLATZ_LOG =
            BLOCKS.register("blue_collatz_log", () -> new CollatzLogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LOG)));

    public static final Supplier<Block> BLUE_COLLATZ_NODE =
            BLOCKS.register("blue_collatz_node", () -> new CollatzWoodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_WOOD)));

    public static final Supplier<LeavesBlock> BLUE_COLLATZ_LEAVES =
            BLOCKS.register("blue_collatz_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES)));

    public static final Supplier<LeavesBlock> BLUE_FRUITING_COLLATZ_LEAVES =
            BLOCKS.register("blue_fruiting_collatz_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES)));

    public static final Supplier<Block> BLUE_COLLATZ_PLANKS =
            BLOCKS.register("blue_collatz_planks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PLANKS)));

    public static final Supplier<Block> BLUE_COLLATZ_SLAB =
            BLOCKS.register("blue_collatz_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SLAB)));

    public static final Supplier<Block> BLUE_COLLATZ_STAIRS =
            BLOCKS.register("blue_collatz_stairs", () -> new StairBlock(BLUE_COLLATZ_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_STAIRS)));

    public static final Supplier<Block> BLUE_COLLATZ_FENCE =
            BLOCKS.register("blue_collatz_fence", () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_FENCE)));

    public static final Supplier<Block> BLUE_COLLATZ_FENCE_GATE =
            BLOCKS.register("blue_collatz_fence_gate", () -> new FenceGateBlock(WoodType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_FENCE_GATE)));

    public static final Supplier<Block> BLUE_COLLATZ_DOOR =
            BLOCKS.register("blue_collatz_door", () -> new DoorBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_DOOR)));

    public static final Supplier<Block> BLUE_COLLATZ_TRAPDOOR =
            BLOCKS.register("blue_collatz_trapdoor", () -> new TrapDoorBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_TRAPDOOR)));

    public static final Supplier<Block> BLUE_COLLATZ_BUTTON =
            BLOCKS.register("blue_collatz_button", () -> new ButtonBlock(BlockSetType.CHERRY, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_BUTTON)));

    public static final Supplier<Block> BLUE_COLLATZ_PRESSURE_PLATE =
            BLOCKS.register("blue_collatz_pressure_plate", () -> new PressurePlateBlock(BlockSetType.CHERRY, BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PRESSURE_PLATE)));

    public static final Supplier<GeometryAltarBlock> GEOMETRY_ALTAR = BLOCKS.register("geometry_altar",
            () -> new GeometryAltarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(-1.0F, 3600000.0F)));

    private ModBlocks() {
    }
}
