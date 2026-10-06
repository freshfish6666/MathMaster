package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.block.entity.AxiomDeductionTableBlockEntity;
import com.freshfish.mathmaster.block.entity.NAltarBlockEntity;
import com.freshfish.mathmaster.block.entity.GeometryAltarBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MathMaster.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AxiomDeductionTableBlockEntity>>
            AXIOM_DEDUCTION_TABLE = BLOCK_ENTITY_TYPES.register(
                    "axiom_deduction_table",
                    () -> BlockEntityType.Builder.of(
                            AxiomDeductionTableBlockEntity::new,
                            ModBlocks.AXIOM_DEDUCTION_TABLE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NAltarBlockEntity>> N_ALTAR =
            BLOCK_ENTITY_TYPES.register("n_altar", () -> BlockEntityType.Builder.of(
                    NAltarBlockEntity::new, ModBlocks.N_ALTAR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeometryAltarBlockEntity>> GEOMETRY_ALTAR =
            BLOCK_ENTITY_TYPES.register("geometry_altar", () -> BlockEntityType.Builder.of(
                    GeometryAltarBlockEntity::new, ModBlocks.GEOMETRY_ALTAR.get()).build(null));

    private ModBlockEntities() {
    }
}
