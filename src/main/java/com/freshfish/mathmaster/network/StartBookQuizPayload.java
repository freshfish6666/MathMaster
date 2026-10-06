package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client intent to start a server-validated quiz for one mathematical book. */
public record StartBookQuizPayload(ResourceLocation bookId) implements CustomPacketPayload {
    public static final Type<StartBookQuizPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "start_book_quiz")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StartBookQuizPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    StartBookQuizPayload::bookId,
                    StartBookQuizPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
