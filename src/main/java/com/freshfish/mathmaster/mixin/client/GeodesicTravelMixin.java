package com.freshfish.mathmaster.mixin.client;

import com.freshfish.mathmaster.client.GeodesicClientController;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class GeodesicTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void mathmaster$geodesicTravel(Vec3 input, CallbackInfo callback) {
        if (GeodesicClientController.travel((Player) (Object) this)) callback.cancel();
    }
}
