package com.freshfish.mathmaster.mixin;

import com.freshfish.mathmaster.axiom.PrimeComboSkill;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Valid held-slot packets interrupt immediately, even if switched back before the next tick. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PrimeComboHeldSlotMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleSetCarriedItem",at=@At(value="FIELD",
            target="Lnet/minecraft/world/entity/player/Inventory;selected:I",opcode=Opcodes.PUTFIELD),require=1)
    private void mathmaster$slotChanged(ServerboundSetCarriedItemPacket packet,CallbackInfo ci) {
        if (player.getInventory().selected!=packet.getSlot()) PrimeComboSkill.stop(player);
    }
}
