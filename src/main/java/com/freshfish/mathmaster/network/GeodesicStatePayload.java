package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Zero stops flight (or resets fall distance after a dash); 2..5 enables flight. */
public record GeodesicStatePayload(int level, boolean originalNoGravity) implements CustomPacketPayload {
    public static final Type<GeodesicStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "geodesic_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GeodesicStatePayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeVarInt(payload.level);
                buffer.writeBoolean(payload.originalNoGravity);
            }, buffer -> new GeodesicStatePayload(buffer.readVarInt(), buffer.readBoolean()));
    private static volatile Consumer<GeodesicStatePayload> clientHandler = payload -> {};

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientHandler.accept(payload)));
    }

    public static void setClientHandler(Consumer<GeodesicStatePayload> handler) {
        clientHandler = Objects.requireNonNull(handler);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
