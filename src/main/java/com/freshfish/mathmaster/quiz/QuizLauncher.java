package com.freshfish.mathmaster.quiz;

import com.freshfish.mathmaster.intelligence.IntelligenceData;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.integration.IntegrationManager;
import com.freshfish.mathmaster.menu.BookshelfQuizMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

import java.util.concurrent.ThreadLocalRandom;

/** Internal, server-only owner of quiz menu creation. */
public final class QuizLauncher {
    private static final Component TITLE = Component.translatable("menu.mathmaster.bookshelf_quiz");

    private QuizLauncher() {
    }

    public static boolean openBookshelfQuiz(ServerPlayer player, BlockPos bookshelfPos, QuizBank bank) {
        return openQuiz(player, bank, bookshelfPos, true);
    }

    public static boolean openRemoteQuiz(ServerPlayer player, ResourceLocation bookId) {
        QuizBank bank = QuizBank.byBookId(bookId);
        return bank != null && openQuiz(player, bank, BlockPos.ZERO, false);
    }

    public static boolean openRemoteQuiz(ServerPlayer player, QuizBank bank) {
        return openQuiz(player, bank, BlockPos.ZERO, false);
    }

    private static boolean openQuiz(
            ServerPlayer player,
            QuizBank bank,
            BlockPos bookshelfPos,
            boolean requiresBookshelf
    ) {
        QuizBank.SelectedQuiz selectedQuiz = bank.randomQuestion(player).orElse(null);
        if (selectedQuiz == null) {
            return false;
        }

        QuizQuestion question = selectedQuiz.question();
        ResourceLocation bookId = BuiltInRegistries.ITEM.getKey(bank.bookItem());
        if (!IntegrationManager.tryStartQuiz(player, bookId, bank.difficulty())) {
            return false;
        }

        int correctOption = ThreadLocalRandom.current().nextInt(4);
        IntelligenceData intelligence = IntelligenceManager.get(player);
        int effectiveIq = IntelligenceManager.getEffectiveIq(player);

        player.stopUsingItem();
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, ignoredPlayer) ->
                        new BookshelfQuizMenu(
                                containerId,
                                playerInventory,
                                bookshelfPos,
                                requiresBookshelf,
                                bank,
                                selectedQuiz.number(),
                                question,
                                correctOption,
                                bank.difficulty(),
                                effectiveIq,
                                intelligence.getExperience(),
                                intelligence.getXpNeededForNextIq()
                        ),
                TITLE
        ), buf -> {
            var english = question.text("en_us");
            buf.writeBlockPos(bookshelfPos);
            buf.writeBoolean(requiresBookshelf);
            buf.writeUtf(bank.dataId());
            buf.writeResourceLocation(question.id());
            buf.writeInt(selectedQuiz.number());
            buf.writeUtf(question.question());
            buf.writeUtf(question.correctAnswer());
            buf.writeUtf(question.wrongAnswers().get(0));
            buf.writeUtf(question.wrongAnswers().get(1));
            buf.writeUtf(question.wrongAnswers().get(2));
            buf.writeUtf(english.question());
            buf.writeUtf(english.correctAnswer());
            buf.writeUtf(english.wrongAnswers().get(0));
            buf.writeUtf(english.wrongAnswers().get(1));
            buf.writeUtf(english.wrongAnswers().get(2));
            buf.writeByte(correctOption);
            buf.writeInt(bank.difficulty());
            buf.writeInt(effectiveIq);
            buf.writeInt(intelligence.getExperience());
            buf.writeInt(intelligence.getXpNeededForNextIq());
        });
        return true;
    }
}
