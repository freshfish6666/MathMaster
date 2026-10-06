package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client intent to fetch the current server-owned mathematical book catalog. */
public record QuizCatalogRequestPayload() implements CustomPacketPayload {
    public static final Type<QuizCatalogRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "quiz_catalog_request")
    );
    public static final StreamCodec<ByteBuf, QuizCatalogRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new QuizCatalogRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
