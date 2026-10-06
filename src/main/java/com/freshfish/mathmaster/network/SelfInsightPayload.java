package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;
import java.util.function.Consumer;

public record SelfInsightPayload(
        String playerName,
        String uuid,
        int intellect,
        int experience,
        int experienceNeeded,
        int pollution
) implements CustomPacketPayload {
    public static final Type<SelfInsightPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "self_insight")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelfInsightPayload> STREAM_CODEC =
            StreamCodec.of(SelfInsightPayload::write, SelfInsightPayload::read);

    private static volatile Consumer<SelfInsightPayload> clientHandler = payload -> {};

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, SelfInsightPayload::handle);
    }

    public static void setClientHandler(Consumer<SelfInsightPayload> handler) {
        clientHandler = Objects.requireNonNull(handler);
    }

    private static void write(RegistryFriendlyByteBuf buffer, SelfInsightPayload payload) {
        buffer.writeUtf(payload.playerName);
        buffer.writeUtf(payload.uuid);
        buffer.writeVarInt(payload.intellect);
        buffer.writeVarInt(payload.experience);
        buffer.writeVarInt(payload.experienceNeeded);
        buffer.writeVarInt(payload.pollution);
    }

    private static SelfInsightPayload read(RegistryFriendlyByteBuf buffer) {
        return new SelfInsightPayload(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    private static void handle(SelfInsightPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientHandler.accept(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
