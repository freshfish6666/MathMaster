package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.menu.MathMasterGuideMenu;
import com.freshfish.mathmaster.quiz.QuizBank;
import com.freshfish.mathmaster.quiz.QuizLauncher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

public class BookshelfInteractionHandler {
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        QuizBank lecternBank = findLecternQuizBank(level, pos);
        if (lecternBank != null) {
            if (event.getEntity().isShiftKeyDown()) {
                ItemStack heldStack = event.getItemStack();
                QuizBank heldBank = QuizBank.byItem(heldStack.getItem());
                if (!heldStack.isEmpty() && heldBank == null) {
                    return;
                }
                if (!level.isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer) {
                    takeOrReplaceLecternBook(serverPlayer, event.getHand(), pos, heldBank);
                }
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }

            if (!level.isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer) {
                MathMasterGuideMenu.open(serverPlayer, lecternBank);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (event.getEntity().isShiftKeyDown()) {
            return;
        }

        if (!level.getBlockState(pos).is(Blocks.BOOKSHELF)) {
            return;
        }

        if (level.isClientSide) {
            if (hasAdjacentChiseledBookshelf(level, pos)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)
                || !openQuiz(serverPlayer, pos)) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    public static boolean openQuiz(ServerPlayer serverPlayer, BlockPos bookshelfPos) {
        if (!serverPlayer.level().getBlockState(bookshelfPos).is(Blocks.BOOKSHELF)) {
            return false;
        }

        List<QuizBank> banks = findAdjacentQuizBanks(serverPlayer.level(), bookshelfPos);
        if (banks.isEmpty()) {
            return false;
        }

        QuizBank bank = banks.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(banks.size()));
        return QuizLauncher.openBookshelfQuiz(serverPlayer, bookshelfPos, bank);
    }

    private boolean hasAdjacentChiseledBookshelf(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.CHISELED_BOOKSHELF)) {
                return true;
            }
        }
        return false;
    }

    private static List<QuizBank> findAdjacentQuizBanks(Level level, BlockPos pos) {
        List<QuizBank> banks = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.getBlockState(neighbor).is(Blocks.CHISELED_BOOKSHELF)) {
                continue;
            }

            if (level.getBlockEntity(neighbor) instanceof ChiseledBookShelfBlockEntity bookshelf) {
                for (int slot = 0; slot < 6; slot++) {
                    QuizBank bank = QuizBank.byItem(bookshelf.getItem(slot).getItem());
                    if (bank != null && bank.hasQuestions()) {
                        banks.add(bank);
                    }
                }
            }
        }

        return banks;
    }

    private static QuizBank findLecternQuizBank(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.LECTERN)
                || !(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)) {
            return null;
        }
        return QuizBank.byItem(lectern.getBook().getItem());
    }

    private static void takeOrReplaceLecternBook(
            ServerPlayer player,
            net.minecraft.world.InteractionHand hand,
            BlockPos pos,
            QuizBank replacementBank
    ) {
        Level level = player.level();
        if (!(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)) {
            return;
        }

        ItemStack oldBook = lectern.getBook().copy();
        ItemStack heldStack = player.getItemInHand(hand);
        if (replacementBank == null) {
            lectern.clearContent();
            LecternBlock.resetBookState(player, level, pos, level.getBlockState(pos), false);
            player.setItemInHand(hand, oldBook);
            return;
        }

        ItemStack replacement = heldStack.consumeAndReturn(1, player);
        lectern.setBook(replacement, player);
        LecternBlock.resetBookState(player, level, pos, level.getBlockState(pos), true);

        if (heldStack.isEmpty()) {
            player.setItemInHand(hand, oldBook);
        } else if (!player.getInventory().add(oldBook)) {
            player.drop(oldBook, false);
        }
    }
}
