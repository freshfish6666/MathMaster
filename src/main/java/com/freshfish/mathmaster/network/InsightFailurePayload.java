package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;
import java.util.function.BiConsumer;

public record InsightFailurePayload(int targetIntellect, int playerIq)
        implements CustomPacketPayload {
    public static final Type<InsightFailurePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "insight_failure")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, InsightFailurePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    InsightFailurePayload::targetIntellect,
                    ByteBufCodecs.VAR_INT,
                    InsightFailurePayload::playerIq,
                    InsightFailurePayload::new
            );

    private static volatile BiConsumer<Integer, Integer> clientHandler = (target, player) -> {
    };

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, InsightFailurePayload::handle);
    }

    public static void setClientHandler(BiConsumer<Integer, Integer> handler) {
        clientHandler = Objects.requireNonNull(handler);
    }

    private static void handle(InsightFailurePayload payload, IPayloadContext context) {
        clientHandler.accept(payload.targetIntellect(), payload.playerIq());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
