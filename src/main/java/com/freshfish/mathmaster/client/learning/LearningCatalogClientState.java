package com.freshfish.mathmaster.client.learning;

import com.freshfish.mathmaster.api.MathMasterApi;
import com.freshfish.mathmaster.network.QuizCatalogRequestPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Small client cache populated only when the learning application is opened. */
public final class LearningCatalogClientState {
    private static volatile List<MathMasterApi.QuizBook> books = List.of();
    private static volatile boolean loading;

    private LearningCatalogClientState() {
    }

    public static void request() {
        books = List.of();
        loading = true;
        PacketDistributor.sendToServer(new QuizCatalogRequestPayload());
    }

    public static void accept(List<MathMasterApi.QuizBook> catalog) {
        books = List.copyOf(catalog);
        loading = false;
    }

    public static List<MathMasterApi.QuizBook> books() {
        return books;
    }

    public static boolean isLoading() {
        return loading;
    }
}
