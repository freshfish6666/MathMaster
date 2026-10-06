package com.freshfish.mathmaster.mixin;

import com.freshfish.mathmaster.axiom.GeodesicSkill;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class GeodesicMovementPacketMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;
    @Shadow private Vec3 awaitingPositionFromClient;

    @Inject(method = "tick", at = @At("HEAD"))
    private void mathmaster$allowGeodesicFlight(CallbackInfo callback) {
        if (GeodesicSkill.isFlying(this.player)) {
            // Exempt only the authorized skill from vanilla's floating kick, without granting mayfly.
            this.clientIsFloating = false;
            this.player.resetFallDistance();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void mathmaster$validateGeodesic(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListenerImpl) (Object) this,
                this.player.serverLevel());
        if (!GeodesicSkill.isFlying(this.player)) return;
        this.player.resetFallDistance();
        if (this.awaitingPositionFromClient == null && packet.hasPosition()
                && !GeodesicSkill.validateMovement(this.player, new Vec3(
                        packet.getX(this.player.getX()), packet.getY(this.player.getY()), packet.getZ(this.player.getZ())))) {
            callback.cancel();
        }
    }
}
