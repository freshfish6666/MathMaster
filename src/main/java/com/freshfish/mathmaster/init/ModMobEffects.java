package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.effect.FlowMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MathMaster.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> FLOW =
            MOB_EFFECTS.register("flow", FlowMobEffect::new);

    private ModMobEffects() {
    }
}
