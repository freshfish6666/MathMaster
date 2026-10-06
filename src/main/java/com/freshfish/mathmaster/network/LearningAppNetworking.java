package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.api.MathMasterApi;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Network boundary used by the optional MCphone learning application. */
public final class LearningAppNetworking {
    private static final String MCPHONE_MOD_ID = "mcphone";
    private static volatile Consumer<List<MathMasterApi.QuizBook>> catalogClientHandler = books -> {
    };

    private LearningAppNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                QuizCatalogRequestPayload.TYPE,
                QuizCatalogRequestPayload.STREAM_CODEC,
                LearningAppNetworking::handleCatalogRequest
        );
        registrar.playToClient(
                QuizCatalogPayload.TYPE,
                QuizCatalogPayload.STREAM_CODEC,
                LearningAppNetworking::handleCatalog
        );
        registrar.playToServer(
                StartBookQuizPayload.TYPE,
                StartBookQuizPayload.STREAM_CODEC,
                LearningAppNetworking::handleStartQuiz
        );
    }

    public static void setCatalogClientHandler(Consumer<List<MathMasterApi.QuizBook>> handler) {
        catalogClientHandler = Objects.requireNonNull(handler);
    }

    private static void handleCatalogRequest(QuizCatalogRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ModList.get().isLoaded(MCPHONE_MOD_ID)
                    && context.player() instanceof ServerPlayer) {
                context.reply(new QuizCatalogPayload(MathMasterApi.getAvailableQuizBooks()));
            }
        });
    }

    private static void handleCatalog(QuizCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> catalogClientHandler.accept(payload.books()));
    }

    private static void handleStartQuiz(StartBookQuizPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ModList.get().isLoaded(MCPHONE_MOD_ID)
                    && context.player() instanceof ServerPlayer player) {
                MathMasterApi.startQuiz(player, payload.bookId());
            }
        });
    }
}
