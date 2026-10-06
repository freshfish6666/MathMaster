package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.axiom.ReturnAnchorData.Anchor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Only available slots are sent to the owning player; hidden positions remain on the server. */
public record ReturnAnchorsPayload(boolean open, int level, int selected, List<Optional<Anchor>> anchors)
        implements CustomPacketPayload {
    public ReturnAnchorsPayload { anchors = List.copyOf(anchors); }
    public static final Type<ReturnAnchorsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "return_anchors"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReturnAnchorsPayload> STREAM_CODEC =
            StreamCodec.of(ReturnAnchorsPayload::write, ReturnAnchorsPayload::read);
    private static volatile Consumer<ReturnAnchorsPayload> clientHandler = payload -> {};

    private static void write(RegistryFriendlyByteBuf buffer, ReturnAnchorsPayload payload) {
        buffer.writeBoolean(payload.open);
        buffer.writeVarInt(payload.level);
        buffer.writeVarInt(payload.selected);
        buffer.writeVarInt(payload.anchors.size());
        for (var anchor : payload.anchors) {
            buffer.writeBoolean(anchor.isPresent());
            if (anchor.isPresent()) {
                var value = anchor.get();
                buffer.writeResourceLocation(value.dimension());
                buffer.writeDouble(value.x()); buffer.writeDouble(value.y()); buffer.writeDouble(value.z());
            }
        }
    }

    private static ReturnAnchorsPayload read(RegistryFriendlyByteBuf buffer) {
        boolean open = buffer.readBoolean();
        int level = buffer.readVarInt(), selected = buffer.readVarInt(), size = buffer.readVarInt();
        if (level < 0 || level == 1 || level > 5 || size != level || selected < -1 || selected >= size) {
            throw new IllegalArgumentException("Invalid return snapshot");
        }
        var anchors = new ArrayList<Optional<Anchor>>(size);
        for (int i = 0; i < size; i++) {
            anchors.add(buffer.readBoolean() ? Optional.of(new Anchor(buffer.readResourceLocation(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble())) : Optional.empty());
        }
        return new ReturnAnchorsPayload(open, level, selected, anchors);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> clientHandler.accept(payload)));
    }

    public static void setClientHandler(Consumer<ReturnAnchorsPayload> handler) { clientHandler = Objects.requireNonNull(handler); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
