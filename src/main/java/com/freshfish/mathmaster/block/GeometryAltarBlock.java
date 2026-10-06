package com.freshfish.mathmaster.block;

import com.freshfish.mathmaster.block.entity.GeometryAltarBlockEntity;
import com.freshfish.mathmaster.init.ModBlockEntities;
import com.freshfish.mathmaster.init.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A reusable socket altar. Its block state also drives the socket/core model and light. */
public final class GeometryAltarBlock extends BaseEntityBlock {
    public static final MapCodec<GeometryAltarBlock> CODEC = simpleCodec(GeometryAltarBlock::new);
    public static final BooleanProperty SUMMONING = BooleanProperty.create("summoning");
    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 2, 16), box(2, 2, 2, 14, 7, 14),
            box(0, 2, 0, 4, 14, 4), box(12, 2, 0, 16, 14, 4),
            box(0, 2, 12, 4, 14, 16), box(12, 2, 12, 16, 14, 16),
            box(1, 2, 1, 15, 12, 4), box(1, 2, 12, 15, 12, 15),
            box(1, 2, 4, 4, 12, 12), box(12, 2, 4, 15, 12, 12));

    public GeometryAltarBlock(Properties properties) {
        super(properties.noOcclusion().pushReaction(PushReaction.BLOCK)
                .lightLevel(state -> state.getValue(SUMMONING) ? 10 : 0));
        registerDefaultState(stateDefinition.any().setValue(SUMMONING, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SUMMONING);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeometryAltarBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.GEOMETRY_ALTAR.get(),
                GeometryAltarBlockEntity::serverTick);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(SUMMONING) && !stack.is(ModItems.GEOMETRY_CORE.get()))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GeometryAltarBlockEntity altar) {
            if (altar.isSummoning()) altar.cancel(player);
            else altar.begin(player, stack);
        }
        // Consuming the interaction prevents the same click from also using the other hand/item.
        return level.isClientSide ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!state.getValue(SUMMONING)) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GeometryAltarBlockEntity altar)
            altar.cancel(player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof GeometryAltarBlockEntity altar) altar.dropOffering();
        super.onRemove(state, level, pos, newState, moved);
    }
}
