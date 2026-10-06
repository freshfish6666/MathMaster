package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModItems;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Double-block placement and removal are exercised against real level updates. */
public final class NAltarCheck {
    private static int checks;
    private static final java.util.List<ItemEntity> observedDrops=new java.util.ArrayList<>();
    public static int run(ServerLevel level) {
        checks=0;
        var pos=new BlockPos(125,240,125);
        var altar=ModBlocks.N_ALTAR.get();
        boolean[] creative={false};
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"NAltarCheck"),ClientInformation.createDefault()) {
            @Override public boolean isCreative() { return creative[0]; }
        };
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockDropsEvent> observer=event -> {
            if(event.getLevel()==level && event.getState().is(altar)) observedDrops.addAll(event.getDrops());
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(observer);
        try {
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            for(float yaw:new float[]{0,90,180,270}) {
                player.setYRot(yaw);
                var stack=new ItemStack(ModItems.N_ALTAR.get(),2);
                require(place(level,pos,player,stack),"actual BlockItem placement");
                var lower=level.getBlockState(pos); var upper=level.getBlockState(pos.above());
                require(lower.is(altar)&&upper.is(altar),"both blocks placed");
                require(lower.getValue(NAltarBlock.HALF)==DoubleBlockHalf.LOWER && upper.getValue(NAltarBlock.HALF)==DoubleBlockHalf.UPPER,"correct halves");
                require(lower.getValue(NAltarBlock.FACING)==player.getDirection().getOpposite()
                        && upper.getValue(NAltarBlock.FACING)==lower.getValue(NAltarBlock.FACING),"both halves face player");
                require(stack.getCount()==1,"only one item consumed");
                require(!lower.getCollisionShape(level,pos).isEmpty() && !upper.getCollisionShape(level,pos.above()).isEmpty(),"solid collision both halves");
                require(lower.getPistonPushReaction()==PushReaction.BLOCK,"piston cannot split relic");
                for(boolean top:new boolean[]{false,true}) {
                    if(!level.getBlockState(pos).is(altar)) require(place(level,pos,player,new ItemStack(ModItems.N_ALTAR.get())),"replace for destruction");
                    clearDrops(level,pos);
                    level.destroyBlock(top?pos.above():pos,true);
                    require(level.getBlockState(pos).isAir()&&level.getBlockState(pos.above()).isAir(),"destroy either half clears both");
                    require(dropCount(level,pos)==1,"destroy half="+top+" count="+dropCount(level,pos));
                }
            }
            clearDrops(level,pos);
            level.setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());
            var blocked=new ItemStack(ModItems.N_ALTAR.get());
            require(!place(level,pos,player,blocked)&&blocked.getCount()==1&&level.getBlockState(pos).isAir(),"blocked upper placement atomic");
            level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
            require(place(level,pos,player,new ItemStack(ModItems.N_ALTAR.get())),"support test placement");
            level.destroyBlock(pos.below(),false);
            require(level.getBlockState(pos).is(altar)&&level.getBlockState(pos.above()).is(altar)&&dropCount(level,pos)==0,"support removed keeps relic without drops");
            level.destroyBlock(pos,true);
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            for(boolean top:new boolean[]{false,true}) {
                clearDrops(level,pos);
                require(place(level,pos,player,new ItemStack(ModItems.N_ALTAR.get())),"creative test placement");
                creative[0]=true;
                var target=top?pos.above():pos; var state=level.getBlockState(target);
                altar.playerWillDestroy(level,target,state,player);
                level.removeBlock(target,false);
                require(level.getBlockState(pos).isAir()&&level.getBlockState(pos.above()).isAir()&&dropCount(level,pos)==0,"creative no orphan or drops");
                creative[0]=false;
            }
            var upper=altar.defaultBlockState().setValue(NAltarBlock.HALF,DoubleBlockHalf.UPPER);
            require(Block.getDrops(upper,level,pos,null).isEmpty(),"upper loot cannot duplicate lower");
            verifyModes(level,pos,player);
            return checks;
        } finally {
            level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());
            clearDrops(level,pos);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(observer);
        }
    }
    private static void verifyModes(ServerLevel level,BlockPos pos,ServerPlayer player) {
        clearDrops(level,pos);
        require(place(level,pos,player,new ItemStack(ModItems.N_ALTAR.get())),"normal mode placement");
        require(!level.getBlockState(pos).getValue(NAltarBlock.BLOOD_SACRIFICE)
                && !level.getBlockState(pos.above()).getValue(NAltarBlock.BLOOD_SACRIFICE),"stone base normal on both halves");
        require(level.getBlockState(pos).getLightEmission(level,pos)==0,"normal mode unlit");
        level.setBlockAndUpdate(pos.below(),ModBlocks.LINGXU_BLOCK.get().defaultBlockState());
        require(level.getBlockState(pos).getValue(NAltarBlock.BLOOD_SACRIFICE)
                && level.getBlockState(pos.above()).getValue(NAltarBlock.BLOOD_SACRIFICE),"lingxu replacement immediately enables both halves");
        require(level.getBlockState(pos).getLightEmission(level,pos)==7
                && level.getBlockState(pos.above()).getLightEmission(level,pos.above())==0,"only glyph base emits world light");
        for(var p:new BlockPos[]{pos,pos.above()}) {
            var state=level.getBlockState(p);
            var encoded=net.minecraft.world.level.block.state.BlockState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,state).getOrThrow();
            require(net.minecraft.world.level.block.state.BlockState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow()==state,"blood block state save roundtrip");
        }
        level.setBlockAndUpdate(pos.below(),Blocks.DIAMOND_BLOCK.defaultBlockState());
        require(!level.getBlockState(pos).getValue(NAltarBlock.BLOOD_SACRIFICE)
                && !level.getBlockState(pos.above()).getValue(NAltarBlock.BLOOD_SACRIFICE)
                && level.getBlockState(pos).getLightEmission(level,pos)==0,"other mineral resets both halves and light");
        level.setBlockAndUpdate(pos.east(),ModBlocks.LINGXU_BLOCK.get().defaultBlockState());
        require(!level.getBlockState(pos).getValue(NAltarBlock.BLOOD_SACRIFICE),"side lingxu does not enable blood mode");
        level.setBlockAndUpdate(pos.east(),Blocks.AIR.defaultBlockState());
        level.destroyBlock(pos,true);
        clearDrops(level,pos);
        level.setBlockAndUpdate(pos.below(),ModBlocks.LINGXU_BLOCK.get().defaultBlockState());
        require(place(level,pos,player,new ItemStack(ModItems.N_ALTAR.get())),"placement directly on lingxu");
        require(level.getBlockState(pos).getValue(NAltarBlock.BLOOD_SACRIFICE)
                && level.getBlockState(pos.above()).getValue(NAltarBlock.BLOOD_SACRIFICE),"placed blood state initialized on both halves");
        level.destroyBlock(pos.above(),true);
        require(level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && dropCount(level,pos)==1,"blood altar removal still drops exactly one");
        clearDrops(level,pos);
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        require(place(level,pos,player,new ItemStack(ModItems.N_ALTAR.get())),"re-place on normal base");
        require(!level.getBlockState(pos).getValue(NAltarBlock.BLOOD_SACRIFICE),"item does not retain former blood mode");
    }
    private static boolean place(ServerLevel level,BlockPos pos,ServerPlayer player,ItemStack stack) {
        var ctx=new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,stack,
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        return ((BlockItem)stack.getItem()).place(ctx).consumesAction();
    }
    private static int dropCount(ServerLevel level,BlockPos pos) {
        return observedDrops.stream()
                .filter(e->e.getItem().is(ModItems.N_ALTAR.get())).mapToInt(e->e.getItem().getCount()).sum();
    }
    private static void clearDrops(ServerLevel level,BlockPos pos) {
        for(var e:observedDrops) e.discard();
        observedDrops.clear();
        for(var e:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3))) e.discard();
    }
    private static void require(boolean ok,String message) {
        if(!ok) throw new AssertionError("N altar: "+message);
        checks++;
    }
}
