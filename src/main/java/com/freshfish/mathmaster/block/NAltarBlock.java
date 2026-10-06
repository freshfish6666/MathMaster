package com.freshfish.mathmaster.block;

import com.mojang.serialization.MapCodec;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModBlockEntities;
import com.freshfish.mathmaster.block.entity.NAltarBlockEntity;
import com.freshfish.mathmaster.ritual.NAltarOfferings;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-block relic statue; the lower half owns the blood-sacrifice controller. */
public final class NAltarBlock extends DoublePlantBlock implements EntityBlock {
    public static final MapCodec<NAltarBlock> CODEC = simpleCodec(NAltarBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty BLOOD_SACRIFICE = BooleanProperty.create("blood_sacrifice");
    public static final IntegerProperty SACRIFICE_GLOW = IntegerProperty.create("sacrifice_glow",0,15);
    private static final VoxelShape LOWER = Shapes.or(Block.box(1,0,1,15,3,15),Block.box(3,3,3,13,16,13));
    private static final VoxelShape UPPER = Block.box(3,0,3,13,16,13);

    public NAltarBlock(Properties properties) {
        super(properties.noOcclusion().pushReaction(PushReaction.BLOCK)
                .lightLevel(state -> state.getValue(HALF)==DoubleBlockHalf.LOWER
                        ? Math.max(state.getValue(BLOOD_SACRIFICE) ? 7 : 0,state.getValue(SACRIFICE_GLOW)) : 0));
        registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH)
                .setValue(BLOOD_SACRIFICE,false).setValue(SACRIFICE_GLOW,0));
    }

    @Override public MapCodec<NAltarBlock> codec() { return CODEC; }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,
                                                        BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        return NAltarOfferings.INSTANCE.offer(stack,state,level,pos,player,hand);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,
                                                        Player player,BlockHitResult hit) {
        // Keep unknown held items and off-hand interactions on their existing vanilla path.
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        BlockPos lower=state.getValue(HALF)==DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (!player.isAlive() || player.isSpectator() || !NAltarOfferings.normal(level,lower)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.PASS;
        return com.freshfish.mathmaster.ritual.NAltarAccess.allowOffering(player)
                ? InteractionResult.PASS : InteractionResult.CONSUME;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(FACING,context.getHorizontalDirection().getOpposite())
                .setValue(BLOOD_SACRIFICE,context.getLevel().getBlockState(context.getClickedPos().below()).is(ModBlocks.LINGXU_BLOCK.get()));
    }

    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,
                                               LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        BlockState updated=super.updateShape(state,direction,neighbor,level,pos,neighborPos);
        if(!updated.is(this)) return updated;
        if(updated.getValue(HALF)==DoubleBlockHalf.LOWER) {
            return updated.setValue(BLOOD_SACRIFICE,level.getBlockState(pos.below()).is(ModBlocks.LINGXU_BLOCK.get()));
        }
        return updated.setValue(BLOOD_SACRIFICE,level.getBlockState(pos.below()).getValue(BLOOD_SACRIFICE));
    }

    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        level.setBlock(pos.above(),state.setValue(HALF,DoubleBlockHalf.UPPER),3);
    }

    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        if(state.getValue(HALF)==DoubleBlockHalf.LOWER) {
            return true;
        }
        BlockState below=level.getBlockState(pos.below());
        return below.is(this) && below.getValue(HALF)==DoubleBlockHalf.LOWER
                && below.getValue(FACING)==state.getValue(FACING);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return state.getValue(HALF)==DoubleBlockHalf.LOWER ? new NAltarBlockEntity(pos,state) : null;
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(HALF)==DoubleBlockHalf.UPPER || type!=ModBlockEntities.N_ALTAR.get()
                ? null : (world,pos,current,entity) -> NAltarBlockEntity.serverTick(world,pos,current,(NAltarBlockEntity)entity);
    }

    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(HALF)==DoubleBlockHalf.LOWER ? LOWER : UPPER;
    }

    @Override protected BlockState rotate(BlockState state,Rotation rotation) {
        return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));
    }

    @Override protected BlockState mirror(BlockState state,Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING,BLOOD_SACRIFICE,SACRIFICE_GLOW);
    }
}
