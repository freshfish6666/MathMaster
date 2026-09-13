package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.effect.DigitalPollutionMobEffect;
import com.freshfish.mathmaster.effect.FlowMobEffect;
import com.freshfish.mathmaster.effect.MentalRampageMobEffect;
import com.freshfish.mathmaster.effect.PrimeMarkMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MathMaster.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> FLOW =
            MOB_EFFECTS.register("flow", FlowMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> MENTAL_RAMPAGE =
            MOB_EFFECTS.register("mental_rampage", MentalRampageMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> DIGITAL_POLLUTION =
            MOB_EFFECTS.register("digital_pollution", DigitalPollutionMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> PRIME_MARK =
            MOB_EFFECTS.register("prime_mark", PrimeMarkMobEffect::new);

    private ModMobEffects() {
    }
}
