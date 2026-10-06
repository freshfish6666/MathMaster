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

public record DigitalPollutionMeterPayload(boolean equipped, int pollution)
        implements CustomPacketPayload {
    public static final Type<DigitalPollutionMeterPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "digital_pollution_meter")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DigitalPollutionMeterPayload> STREAM_CODEC =
            StreamCodec.of(DigitalPollutionMeterPayload::write, DigitalPollutionMeterPayload::read);

    private static volatile Consumer<DigitalPollutionMeterPayload> clientHandler = payload -> {
    };

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, DigitalPollutionMeterPayload::handle);
    }

    public static void setClientHandler(Consumer<DigitalPollutionMeterPayload> handler) {
        clientHandler = Objects.requireNonNull(handler);
    }

    private static void write(RegistryFriendlyByteBuf buffer, DigitalPollutionMeterPayload payload) {
        buffer.writeBoolean(payload.equipped());
        buffer.writeVarInt(payload.pollution());
    }

    private static DigitalPollutionMeterPayload read(RegistryFriendlyByteBuf buffer) {
        return new DigitalPollutionMeterPayload(buffer.readBoolean(), buffer.readVarInt());
    }

    private static void handle(DigitalPollutionMeterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientHandler.accept(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
