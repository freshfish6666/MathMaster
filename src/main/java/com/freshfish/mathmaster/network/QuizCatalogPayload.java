package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.api.MathMasterApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative snapshot of mathematical books currently available for quizzes. */
public record QuizCatalogPayload(List<MathMasterApi.QuizBook> books) implements CustomPacketPayload {
    private static final int MAX_BOOKS = 64;
    private static final int MAX_TRANSLATION_KEY_LENGTH = 256;

    public static final Type<QuizCatalogPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "quiz_catalog")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, QuizCatalogPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public QuizCatalogPayload decode(RegistryFriendlyByteBuf buffer) {
                    int size = buffer.readVarInt();
                    if (size < 0 || size > MAX_BOOKS) {
                        throw new IllegalArgumentException("Invalid quiz catalog size " + size);
                    }
                    List<MathMasterApi.QuizBook> books = new ArrayList<>(size);
                    for (int index = 0; index < size; index++) {
                        books.add(new MathMasterApi.QuizBook(
                                buffer.readResourceLocation(),
                                buffer.readUtf(MAX_TRANSLATION_KEY_LENGTH),
                                buffer.readVarInt()
                        ));
                    }
                    return new QuizCatalogPayload(books);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, QuizCatalogPayload payload) {
                    List<MathMasterApi.QuizBook> books = payload.books();
                    if (books.size() > MAX_BOOKS) {
                        throw new IllegalArgumentException("Quiz catalog exceeds " + MAX_BOOKS + " entries");
                    }
                    buffer.writeVarInt(books.size());
                    for (MathMasterApi.QuizBook book : books) {
                        buffer.writeResourceLocation(book.id());
                        buffer.writeUtf(book.translationKey(), MAX_TRANSLATION_KEY_LENGTH);
                        buffer.writeVarInt(book.difficulty());
                    }
                }
            };

    public QuizCatalogPayload {
        books = List.copyOf(books);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
