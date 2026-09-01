package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.menu.BookshelfQuizMenu;
import com.freshfish.mathmaster.intelligence.IntelligenceData;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.quiz.QuizBank;
import com.freshfish.mathmaster.quiz.QuizQuestion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

public class BookshelfInteractionHandler {
    private static final Component TITLE = Component.translatable("menu.mathmaster.bookshelf_quiz");

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown()) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();

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

        List<QuizBank> banks = findAdjacentQuizBanks(level, pos);
        if (banks.isEmpty()) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.stopUsingItem();

            QuizBank bank = banks.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(banks.size()));
            QuizBank.SelectedQuiz selectedQuiz = bank.randomQuestion().orElse(null);
            if (selectedQuiz == null) {
                return;
            }
            QuizQuestion question = selectedQuiz.question();
            int questionNumber = selectedQuiz.number();
            int correctOption = java.util.concurrent.ThreadLocalRandom.current().nextInt(4);
            IntelligenceData intelligence = IntelligenceManager.get(serverPlayer);

            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, player) ->
                            new BookshelfQuizMenu(
                                    containerId,
                                    playerInventory,
                                    questionNumber,
                                    question,
                                    correctOption,
                                    bank.difficulty(),
                                    intelligence.getIq(),
                                    intelligence.getExperience(),
                                    intelligence.getXpNeededForNextIq()
                            ),
                    TITLE
            ), buf -> {
                buf.writeInt(questionNumber);
                buf.writeUtf(question.question());
                buf.writeUtf(question.correctAnswer());
                buf.writeUtf(question.wrongAnswers().get(0));
                buf.writeUtf(question.wrongAnswers().get(1));
                buf.writeUtf(question.wrongAnswers().get(2));
                buf.writeByte(correctOption);
                buf.writeInt(bank.difficulty());
                buf.writeInt(intelligence.getIq());
                buf.writeInt(intelligence.getExperience());
                buf.writeInt(intelligence.getXpNeededForNextIq());
            });
        }
    }

    private boolean hasAdjacentChiseledBookshelf(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.CHISELED_BOOKSHELF)) {
                return true;
            }
        }
        return false;
    }

    private List<QuizBank> findAdjacentQuizBanks(Level level, BlockPos pos) {
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
}
