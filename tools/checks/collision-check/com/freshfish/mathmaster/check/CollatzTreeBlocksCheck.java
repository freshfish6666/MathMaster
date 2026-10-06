package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Resource-backed item placement and loot checks, excluded from ordinary builds. */
public final class CollatzTreeBlocksCheck {
    private static int checks;
    public static int run(ServerLevel level) {
        checks = 0;
        BlockPos pos = new BlockPos(110,240,110);
        var blocks = new Block[]{ModBlocks.COLLATZ_ROOT.get(),ModBlocks.COLLATZ_LOG.get(),ModBlocks.COLLATZ_NODE.get(),
                ModBlocks.COLLATZ_LEAVES.get(),ModBlocks.FRUITING_COLLATZ_LEAVES.get()};
        var silk = new ItemStack(Items.DIAMOND_AXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        try {
            for(var block:blocks) {
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
                var item = (BlockItem)block.asItem();
                var stack = new ItemStack(item);
                var ctx = new BlockPlaceContext(level,null,InteractionHand.MAIN_HAND,stack,
                        new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
                require(item.place(ctx).consumesAction() && level.getBlockState(pos).is(block),"actual placement "+block);
                var state = level.getBlockState(pos);
                if(block instanceof LeavesBlock leaves) {
                    require(state.getValue(LeavesBlock.PERSISTENT),"placed foliage persists");
                    state.randomTick(level,pos,level.random);
                    require(level.getBlockState(pos).is(block),"no decay after hand placement");
                    var bareDrops=Block.getDrops(state,level,pos,null,null,ItemStack.EMPTY);
                    require(bareDrops.stream().allMatch(drop -> drop.is(Items.STICK)
                            || (block==ModBlocks.FRUITING_COLLATZ_LEAVES.get()&&drop.is(ModItems.COLLATZ_FRUIT.get()))),"ordinary foliage only drops sticks and appropriate fruit");
                    for(var tool:new ItemStack[]{new ItemStack(Items.SHEARS),silk}) {
                        var drops=Block.getDrops(state,level,pos,null,null,tool);
                        require(drops.size()==1 && drops.getFirst().is(item),"foliage recovery with shears/silk");
                    }
                    require(state.is(BlockTags.LEAVES) && state.is(BlockTags.MINEABLE_WITH_HOE),"foliage tags");
                } else {
                    var drops=Block.getDrops(state,level,pos,null);
                    require(drops.size()==1 && drops.getFirst().is(item),"woody block drops itself");
                    require(state.is(BlockTags.MINEABLE_WITH_AXE),"wood axe tag");
                }
            }
            verifyLeafLoot(level,pos);
            for(var face:new Direction[]{Direction.EAST,Direction.UP,Direction.NORTH}) {
                level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                var item=(BlockItem)ModItems.COLLATZ_LOG.get();
                var ctx=new BlockPlaceContext(level,null,InteractionHand.MAIN_HAND,new ItemStack(item),
                        new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false));
                require(item.place(ctx).consumesAction() && level.getBlockState(pos).getValue(RotatedPillarBlock.AXIS)==face.getAxis(),"actual log axis "+face.getAxis());
            }
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.below(),Blocks.DIRT.defaultBlockState());
            var sapling=(BlockItem)ModItems.COLLATZ_SAPLING.get();
            var saplingContext=new BlockPlaceContext(level,null,InteractionHand.MAIN_HAND,new ItemStack(sapling),
                    new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
            require(sapling.place(saplingContext).consumesAction(),"sapling plants on dirt");
            var saplingState=level.getBlockState(pos);
            require(saplingState.is(ModBlocks.COLLATZ_SAPLING.get()),"sapling placement state");
            var bonemeal=new ItemStack(Items.BONE_MEAL);
            require(!net.minecraft.world.item.BoneMealItem.applyBonemeal(bonemeal,level,pos,null)
                    && bonemeal.getCount()==1 && level.getBlockState(pos).equals(saplingState),"bonemeal does not grow or consume");
            var saplingDrops=Block.getDrops(saplingState,level,pos,null);
            require(saplingDrops.size()==1 && saplingDrops.getFirst().is(sapling),"sapling drops itself");
            level.setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());
            require(level.getBlockState(pos).is(ModBlocks.COLLATZ_SAPLING.get()),"sapling survives on farmland");
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            require(level.getBlockState(pos).isAir(),"sapling removed when soil disappears");
            require(!sapling.place(saplingContext).consumesAction(),"sapling cannot plant on stone");
            require(ModBlocks.COLLATZ_LOG.get().defaultBlockState().is(BlockTags.LOGS),"log participates in foliage distance");
            level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
            var leaf=(BlockItem)ModItems.COLLATZ_LEAVES.get();
            var ctx=new BlockPlaceContext(level,null,InteractionHand.MAIN_HAND,new ItemStack(leaf),
                    new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
            require(leaf.place(ctx).consumesAction() && level.getBlockState(pos).getValue(LeavesBlock.WATERLOGGED),"waterlogged foliage placement");
        } finally { level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState()); }
        return checks;
    }
    private static void verifyLeafLoot(ServerLevel level, BlockPos pos) {
        int samples=6000;
        double[] stickChances={.02,.022222223,.025,.033333335,.1};
        double[] fruitChances={.2,.25,.3,.35,.35};
        for(var block:new Block[]{ModBlocks.COLLATZ_LEAVES.get(),ModBlocks.FRUITING_COLLATZ_LEAVES.get()}) {
            var state=block.defaultBlockState();
            for(int fortune=0;fortune<=4;fortune++) {
                var tool=new ItemStack(Items.DIAMOND_HOE);
                if(fortune>0) tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),fortune);
                int sticks=0,fruits=0;boolean valid=true;
                for(int i=0;i<samples;i++) {
                    var drops=Block.getDrops(state,level,pos,null,null,tool);
                    for(var drop:drops) {
                        if(drop.is(Items.STICK)) { sticks++;valid &= drop.getCount()>=1&&drop.getCount()<=2; }
                        else if(drop.is(ModItems.COLLATZ_FRUIT.get())) { fruits++;valid &= block==ModBlocks.FRUITING_COLLATZ_LEAVES.get()&&drop.getCount()==1; }
                        else valid=false;
                    }
                }
                require(valid,"loot item boundaries and quantities, fortune "+fortune);
                require(Math.abs(sticks-samples*stickChances[fortune]) < 6*Math.sqrt(samples*stickChances[fortune]*(1-stickChances[fortune]))+5,"real stick loot chance, fortune "+fortune);
                if(block==ModBlocks.FRUITING_COLLATZ_LEAVES.get())
                    require(Math.abs(fruits-samples*fruitChances[fortune]) < 6*Math.sqrt(samples*fruitChances[fortune]*(1-fruitChances[fortune]))+5,"real fruit loot chance, fortune "+fortune);
                else require(fruits==0,"ordinary leaves never drop fruit");
            }
            for(boolean shears:new boolean[]{true,false}) {
                var tool=new ItemStack(shears?Items.SHEARS:Items.DIAMOND_HOE);
                if(!shears) tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
                tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),3);
                boolean onlyLeaf=true;
                for(int i=0;i<100;i++) {
                    var drops=Block.getDrops(state,level,pos,null,null,tool);
                    onlyLeaf &= drops.size()==1&&drops.getFirst().is(block.asItem())&&drops.getFirst().getCount()==1;
                }
                require(onlyLeaf,"recovery takes precedence over fortune, shears="+shears);
            }
        }
        require(ModItems.COLLATZ_FRUIT.get().getDefaultMaxStackSize()==64,"fruit stack size remains normal");
    }

    private static void require(boolean success,String description) {
        if(!success) throw new AssertionError("Collatz tree blocks: "+description);
        checks++;
    }
}
