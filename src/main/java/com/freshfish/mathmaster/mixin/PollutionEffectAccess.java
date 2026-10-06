package com.freshfish.mathmaster.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MobEffectInstance.class)
public interface PollutionEffectAccess {
    @Accessor("hiddenEffect") MobEffectInstance mathmaster$getHiddenEffect();
    @Accessor("hiddenEffect") void mathmaster$setHiddenEffect(MobEffectInstance effect);
    @Invoker("setDetailsFrom") void mathmaster$setDetailsFrom(MobEffectInstance effect);
}
