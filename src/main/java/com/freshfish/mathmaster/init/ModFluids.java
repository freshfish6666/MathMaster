package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.fluid.PollutedWaterFluidType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MathMaster.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, MathMaster.MODID);
    public static final Supplier<FluidType> DIGITALLY_POLLUTED_WATER_TYPE =
            FLUID_TYPES.register("digitally_polluted_water", PollutedWaterFluidType::new);
    public static final Supplier<FlowingFluid> DIGITALLY_POLLUTED_WATER =
            FLUIDS.register("digitally_polluted_water", () -> new BaseFlowingFluid.Source(properties()));
    public static final Supplier<FlowingFluid> FLOWING_DIGITALLY_POLLUTED_WATER =
            FLUIDS.register("flowing_digitally_polluted_water", () -> new BaseFlowingFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(DIGITALLY_POLLUTED_WATER_TYPE,
                DIGITALLY_POLLUTED_WATER, FLOWING_DIGITALLY_POLLUTED_WATER)
                .bucket(ModItems.DIGITALLY_POLLUTED_WATER_BUCKET)
                .block(ModBlocks.DIGITALLY_POLLUTED_WATER)
                .slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30).explosionResistance(100);
    }

    private ModFluids() {}
}
