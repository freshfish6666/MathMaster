package com.freshfish.mathmaster.mixin;

import com.freshfish.mathmaster.axiom.PrimeComboSkill;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerPrimeComboAttackMixin {
    @Redirect(method="attack", at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), require=1)
    private boolean mathmaster$primaryHit(Entity target, DamageSource source, float amount) {
        return PrimeComboSkill.hurtPrimary((Player)(Object)this,target,source,amount);
    }
}
