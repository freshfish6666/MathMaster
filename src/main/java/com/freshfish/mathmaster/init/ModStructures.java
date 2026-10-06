package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.worldgen.UnfinishedNetherGatewayStructure;
import com.freshfish.mathmaster.worldgen.GeometrySanctuaryStructure;
import com.freshfish.mathmaster.worldgen.GeometrySanctuaryPoolElement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MathMaster.MODID);
    public static final DeferredHolder<StructureType<?>, StructureType<UnfinishedNetherGatewayStructure>>
            UNFINISHED_NETHER_GATEWAY = STRUCTURE_TYPES.register("unfinished_nether_gateway",
                    () -> () -> UnfinishedNetherGatewayStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<GeometrySanctuaryStructure>>
            GEOMETRY_SANCTUARY = STRUCTURE_TYPES.register("geometry_sanctuary", () -> () -> GeometrySanctuaryStructure.CODEC);
    public static final DeferredRegister<StructurePoolElementType<?>> POOL_ELEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_POOL_ELEMENT, MathMaster.MODID);
    public static final DeferredHolder<StructurePoolElementType<?>, StructurePoolElementType<GeometrySanctuaryPoolElement>>
            GEOMETRY_SANCTUARY_ELEMENT = POOL_ELEMENT_TYPES.register("geometry_sanctuary_pool_element",
                    () -> () -> GeometrySanctuaryPoolElement.CODEC);

    private ModStructures() {}
}
