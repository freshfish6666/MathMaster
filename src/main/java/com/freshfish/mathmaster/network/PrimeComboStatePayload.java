package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Only the owner's bonus and idle countdown are sent; zero ends the visual. */
public record PrimeComboStatePayload(int prime, int remainingTicks) implements CustomPacketPayload {
    public static final Type<PrimeComboStatePayload> TYPE=new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID,"prime_combo_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PrimeComboStatePayload> STREAM_CODEC=StreamCodec.of(
            (buffer,payload) -> { buffer.writeVarInt(payload.prime); buffer.writeVarInt(payload.remainingTicks); },
            buffer -> new PrimeComboStatePayload(buffer.readVarInt(),buffer.readVarInt()));
    private static volatile Consumer<PrimeComboStatePayload> clientHandler=payload -> {};
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE,STREAM_CODEC,
                (payload,context) -> context.enqueueWork(() -> clientHandler.accept(payload)));
    }
    public static void setClientHandler(Consumer<PrimeComboStatePayload> handler) { clientHandler=Objects.requireNonNull(handler); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
