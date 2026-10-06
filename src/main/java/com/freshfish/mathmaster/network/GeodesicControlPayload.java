package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.axiom.GeodesicSkill;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Heartbeats keep an existing press alive; they can never start a new skill. */
public record GeodesicControlPayload(boolean held) implements CustomPacketPayload {
    public static final Type<GeodesicControlPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "geodesic_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GeodesicControlPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeBoolean(payload.held),
                    buffer -> new GeodesicControlPayload(buffer.readBoolean()));

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.held) GeodesicSkill.heartbeat(player);
                else GeodesicSkill.cancel(player);
            }
        }));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
