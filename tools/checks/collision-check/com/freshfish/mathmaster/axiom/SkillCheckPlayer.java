package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.network.InvolutionStatePayload;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;

/** Real players with a recording connection, registered only for the isolated check's lifetime. */
final class SkillCheckPlayer implements AutoCloseable {
    final ServerPlayer player;
    final List<InvolutionStatePayload> involutionPackets = new ArrayList<>();
    final List<com.freshfish.mathmaster.network.ReturnAnchorsPayload> returnPackets = new ArrayList<>();
    final List<com.freshfish.mathmaster.network.PrimeComboStatePayload> primeComboPackets = new ArrayList<>();
    final List<net.minecraft.network.chat.Component> chatMessages = new ArrayList<>();
    private final List<ServerPlayer> serverPlayers;
    private final Map<UUID, ServerPlayer> playersById;

    @SuppressWarnings("unchecked")
    SkillCheckPlayer(ServerLevel level, String name) throws Exception {
        player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(level.getServer(),
                new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            // Probe connections have no Netty channel; vanilla dimension travel queries negotiated channels.
            @Override public boolean hasChannel(net.minecraft.resources.ResourceLocation payloadId) { return false; }
            @Override public void send(Packet<?> packet) { record(packet); }
            @Override public void send(Packet<?> packet, PacketSendListener callback) { record(packet); }
            private void record(Packet<?> packet) {
                if (packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat) {
                    chatMessages.add(chat.content());
                }
                if (packet instanceof ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof InvolutionStatePayload state) {
                    involutionPackets.add(state);
                }
                if (packet instanceof ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof com.freshfish.mathmaster.network.PrimeComboStatePayload state) {
                    primeComboPackets.add(state);
                }
                if (packet instanceof ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof com.freshfish.mathmaster.network.ReturnAnchorsPayload state) {
                    returnPackets.add(state);
                }
            }
        };
        var playersField = PlayerList.class.getDeclaredField("players");
        playersField.setAccessible(true);
        serverPlayers = (List<ServerPlayer>) playersField.get(level.getServer().getPlayerList());
        var idsField = PlayerList.class.getDeclaredField("playersByUUID");
        idsField.setAccessible(true);
        playersById = (Map<UUID, ServerPlayer>) idsField.get(level.getServer().getPlayerList());
        player.moveTo(4, 240, 4, 0, 0);
        level.addNewPlayer(player);
        serverPlayers.add(player);
        playersById.put(player.getUUID(), player);
    }

    @Override public void close() {
        serverPlayers.remove(player);
        playersById.remove(player.getUUID());
        player.discard();
    }
}
