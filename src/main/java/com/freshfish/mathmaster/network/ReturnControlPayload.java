package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.axiom.ReturnSkill;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Clients send operations and slot indices, never destination coordinates. */
public record ReturnControlPayload(int action, int slot) implements CustomPacketPayload {
    public static final int SELECT = 0, RECORD = 1, DELETE = 2, CLOSE = 3, HEARTBEAT = 4, CANCEL = 5;
    public static final Type<ReturnControlPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "return_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReturnControlPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeVarInt(payload.action); buffer.writeVarInt(payload.slot); },
            buffer -> new ReturnControlPayload(buffer.readVarInt(), buffer.readVarInt()));

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) ReturnSkill.control(player, payload.action, payload.slot);
        }));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
