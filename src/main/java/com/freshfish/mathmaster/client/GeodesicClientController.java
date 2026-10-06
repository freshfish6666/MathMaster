package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.AxiomEffectManager;
import com.freshfish.mathmaster.axiom.GeodesicMovement;
import com.freshfish.mathmaster.network.GeodesicControlPayload;
import com.freshfish.mathmaster.network.GeodesicStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client prediction uses the same sweep as the server, without changing game mode or flight abilities. */
public final class GeodesicClientController {
    private static LocalPlayer controlledPlayer;
    private static int level;
    private static boolean originalNoGravity;

    private GeodesicClientController() {}

    public static void accept(GeodesicStatePayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) { reset(); return; }
        if (AxiomDefinition.GEODESIC.acceptsNoteLevel(payload.level())) {
            Minecraft minecraft = Minecraft.getInstance();
            if (!AxiomSkillKeyHandler.USE_SKILL.isDown() || minecraft.screen != null || !minecraft.isWindowActive()) {
                // A delayed activation must not restart flight after the player has released the key.
                PacketDistributor.sendToServer(new GeodesicControlPayload(false));
                reset();
                return;
            }
            if (controlledPlayer != player || level == 0) {
                reset();
                controlledPlayer = player;
                originalNoGravity = payload.originalNoGravity();
            }
            level = payload.level();
            player.setNoGravity(true);
        } else {
            reset();
            player.setNoGravity(payload.originalNoGravity());
            player.resetFallDistance();
            player.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static void tick(Minecraft minecraft) {
        if (level == 0) return;
        if (minecraft.player != controlledPlayer || !controlledPlayer.isAlive()
                || minecraft.screen != null || !minecraft.isWindowActive()
                || controlledPlayer.isPassenger() || controlledPlayer.isSpectator()
                || AxiomEffectManager.getEquippedLevel(controlledPlayer, AxiomDefinition.GEODESIC) != level) {
            stop();
        }
    }

    /** Replaces vanilla travel only for this client's local player during an authorized flight. */
    public static boolean travel(Player player) {
        if (level == 0 || player != controlledPlayer) return false;
        var result = GeodesicMovement.flyStep(player, level);
        if (result.blocked()) {
            player.displayClientMessage(Component.translatable("message.mathmaster.axiom.geodesic.blocked"), true);
            stop();
        }
        return true;
    }

    public static void stop() {
        if (level != 0 && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new GeodesicControlPayload(false));
        }
        reset();
    }

    private static void reset() {
        if (controlledPlayer != null) {
            controlledPlayer.setNoGravity(originalNoGravity);
            controlledPlayer.resetFallDistance();
            controlledPlayer.setDeltaMovement(Vec3.ZERO);
        }
        controlledPlayer = null;
        level = 0;
    }
}
