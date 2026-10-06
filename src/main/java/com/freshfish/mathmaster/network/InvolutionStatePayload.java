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

public record InvolutionStatePayload(boolean active, int playerEntityId, int targetEntityId) implements CustomPacketPayload {
    public static final Type<InvolutionStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "involution_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InvolutionStatePayload> STREAM_CODEC =
            StreamCodec.of(InvolutionStatePayload::write, InvolutionStatePayload::read);
    private static volatile Consumer<InvolutionStatePayload> clientHandler = payload -> {};

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, InvolutionStatePayload::handle);
    }

    public static void setClientHandler(Consumer<InvolutionStatePayload> handler) {
        clientHandler = Objects.requireNonNull(handler);
    }

    private static void write(RegistryFriendlyByteBuf buffer, InvolutionStatePayload payload) {
        buffer.writeBoolean(payload.active);
        buffer.writeVarInt(payload.playerEntityId);
        buffer.writeVarInt(payload.targetEntityId);
    }

    private static InvolutionStatePayload read(RegistryFriendlyByteBuf buffer) {
        return new InvolutionStatePayload(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt());
    }

    private static void handle(InvolutionStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientHandler.accept(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
