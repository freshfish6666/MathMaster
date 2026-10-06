package com.freshfish.mathmaster.mixin;

import com.freshfish.mathmaster.block.DigitallyCorruptedBlock;
import com.freshfish.mathmaster.init.ModMobEffects;
import com.freshfish.mathmaster.pollution.PollutionExposure;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla merging changes source-identifying flags even for a weaker added effect. */
@Mixin(MobEffectInstance.class)
public abstract class PollutionEffectMergeMixin {
    @Inject(method="update",at=@At("HEAD"),cancellable=true)
    private void mathmaster$mergeExternalPollution(MobEffectInstance added, CallbackInfoReturnable<Boolean> result) {
        MobEffectInstance current = (MobEffectInstance)(Object)this;
        if (!current.is(ModMobEffects.DIGITAL_POLLUTION) || !added.is(current.getEffect())
                || !DigitallyCorruptedBlock.isGrantedEffect(current)
                || DigitallyCorruptedBlock.isGrantedEffect(added)) return;
        MobEffectInstance external = PollutionExposure.externalEffect(current);
        if (external == null) external = added;
        else external.update(added);
        MobEffectInstance combined = external.getAmplifier() >= current.getAmplifier()
                ? external : PollutionExposure.blockEffect(current.getAmplifier()+1,current.getDuration(),external);
        PollutionEffectAccess access = (PollutionEffectAccess)(Object)current;
        access.mathmaster$setDetailsFrom(combined);
        access.mathmaster$setHiddenEffect(((PollutionEffectAccess)(Object)combined).mathmaster$getHiddenEffect());
        result.setReturnValue(true);
    }
}
