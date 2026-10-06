package com.freshfish.mathmaster.ritual;

import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.oracle.OracleManager;
import com.freshfish.mathmaster.oracle.OracleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** The offering controls chance; this altar always requests the low oracle pool. */
public final class NAltarKnowledgeOffering {
    private NAltarKnowledgeOffering() {}

    public static int chance(ItemStack stack) {
        if (stack.is(Items.IRON_INGOT)) return 30;
        if (stack.is(Items.GOLD_INGOT)) return 50;
        if (stack.is(Items.EMERALD)) return 70;
        if (stack.is(Items.DIAMOND)) return 90;
        return 0;
    }

    public static ItemInteractionResult offer(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        int chance = chance(stack);
        if (chance == 0 || stack.isEmpty() || player.isSpectator() || !player.isAlive()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockPos lower = state.getValue(NAltarBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockState actual = level.getBlockState(lower);
        if (!actual.is(ModBlocks.N_ALTAR.get()) || actual.getValue(NAltarBlock.HALF) != DoubleBlockHalf.LOWER
                || level.getBlockState(lower.below()).is(ModBlocks.LINGXU_BLOCK.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (!NAltarAccess.allowOffering(player)) return ItemInteractionResult.CONSUME;
        if (OracleManager.getPool(OracleTier.LOW).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.mathmaster.n_altar.no_oracles"), false);
            return ItemInteractionResult.CONSUME;
        }
        // One accepted offering is spent even if the prayer receives no response.
        stack.shrink(1);
        if (level.random.nextInt(100) < chance) {
            OracleManager.draw(OracleTier.LOW, level.random).ifPresent(oracle ->
                    player.displayClientMessage(oracle.message(), false));
        } else {
            player.displayClientMessage(Component.translatable("message.mathmaster.n_altar.unanswered"), false);
        }
        return ItemInteractionResult.CONSUME;
    }
}
