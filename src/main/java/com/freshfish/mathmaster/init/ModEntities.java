package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.EightEntity;
import com.freshfish.mathmaster.entity.FiveEntity;
import com.freshfish.mathmaster.entity.SixEntity;
import com.freshfish.mathmaster.entity.SevenEntity;
import com.freshfish.mathmaster.entity.NineEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MathMaster.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<NineEntity>> NINE =
            ENTITIES.register("nine", () -> EntityType.Builder.of(NineEntity::new, MobCategory.MONSTER)
                    .sized(NineEntity.HITBOX_WIDTH, NineEntity.HITBOX_HEIGHT)
                    .eyeHeight(1.8F).clientTrackingRange(8)
                    .build("mathmaster:nine"));
    public static final DeferredHolder<EntityType<?>, EntityType<FiveEntity>> FIVE =
            ENTITIES.register("five", () -> EntityType.Builder.of(FiveEntity::new, MobCategory.MONSTER)
                    .sized(FiveEntity.HITBOX_WIDTH, FiveEntity.HITBOX_HEIGHT)
                    .eyeHeight((float) FiveEntity.PLATFORM_HEIGHT).clientTrackingRange(32)
                    .build("mathmaster:five"));
    public static final DeferredHolder<EntityType<?>, EntityType<EightEntity>> EIGHT =
            ENTITIES.register("eight", () -> EntityType.Builder.of(EightEntity::new, MobCategory.MONSTER)
                    .sized(NineEntity.HITBOX_WIDTH, NineEntity.HITBOX_HEIGHT)
                    .eyeHeight(1.8F).clientTrackingRange(8)
                    .build("mathmaster:eight"));

    public static final DeferredHolder<EntityType<?>, EntityType<SixEntity>> SIX =
            ENTITIES.register("six", () -> EntityType.Builder.of(SixEntity::new, MobCategory.MONSTER)
                    .sized(SixEntity.HITBOX_WIDTH, SixEntity.HITBOX_HEIGHT)
                    .eyeHeight(2.65F).clientTrackingRange(10)
                    .build("mathmaster:six"));

    public static final DeferredHolder<EntityType<?>, EntityType<SevenEntity>> SEVEN =
            ENTITIES.register("seven", () -> EntityType.Builder.of(SevenEntity::new, MobCategory.MONSTER)
                    .sized(SevenEntity.COLLISION_WIDTH, NineEntity.HITBOX_HEIGHT)
                    .eyeHeight(1.8F).clientTrackingRange(8)
                    .build("mathmaster:seven"));

    private ModEntities() {}

    public static final DeferredHolder<EntityType<?>, EntityType<com.freshfish.mathmaster.entity.GeometryHolderEntity>> GEOMETRY_HOLDER =
            ENTITIES.register("geometry_holder", () -> EntityType.Builder.of(
                    com.freshfish.mathmaster.entity.GeometryHolderEntity::new, MobCategory.MISC)
                    .sized(com.freshfish.mathmaster.entity.GeometryHolderEntity.WIDTH,
                            com.freshfish.mathmaster.entity.GeometryHolderEntity.HEIGHT)
                    .eyeHeight(2.75F).clientTrackingRange(10).updateInterval(1).build("mathmaster:geometry_holder"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(FIVE.get(), FiveEntity.createAttributes().build());
        event.put(NINE.get(), NineEntity.createAttributes().build());
        event.put(EIGHT.get(), EightEntity.createAttributes().build());
        event.put(SIX.get(), SixEntity.createAttributes().build());
        event.put(SEVEN.get(), SevenEntity.createAttributes().build());
        event.put(GEOMETRY_HOLDER.get(), com.freshfish.mathmaster.entity.GeometryHolderEntity.createAttributes().build());
        event.put(GEOMETRY_CONSTRUCT.get(), com.freshfish.mathmaster.entity.GeometryConstructEntity.createAttributes().build());
    }

    public static final DeferredHolder<EntityType<?>, EntityType<com.freshfish.mathmaster.entity.GeometryConstructEntity>> GEOMETRY_CONSTRUCT =
            ENTITIES.register("geometry_construct", () -> EntityType.Builder.of(
                    com.freshfish.mathmaster.entity.GeometryConstructEntity::new, MobCategory.MONSTER)
                    .sized(com.freshfish.mathmaster.entity.GeometryConstructEntity.SIZE,
                            com.freshfish.mathmaster.entity.GeometryConstructEntity.SIZE)
                    .eyeHeight(.6875F).clientTrackingRange(8).updateInterval(1).build("mathmaster:geometry_construct"));

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                NINE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntities::checkDigitalMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
                EIGHT.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntities::checkDigitalMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    private static <T extends NineEntity> boolean checkDigitalMobSpawnRules(
            EntityType<T> type,
            net.minecraft.world.level.ServerLevelAccessor level,
            MobSpawnType spawnType,
            net.minecraft.core.BlockPos pos,
            net.minecraft.util.RandomSource random
    ) {
        // Authored trial encounters must not depend on nearby Peano equipment or ambient light.
        // Keep the existing overworld/difficulty rules and all natural-spawn conditions.
        if (spawnType == MobSpawnType.TRIAL_SPAWNER) {
            return Level.OVERWORLD.equals(level.getLevel().dimension())
                    && level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                    && Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
        }
        return Level.OVERWORLD.equals(level.getLevel().dimension())
                && level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                && level.getMaxLocalRawBrightness(pos) <= 5
                && com.freshfish.mathmaster.axiom.AxiomEffectManager
                        .getPeanoSpawnMultiplier(level.getLevel(), pos) > 0.0D
                && Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
    }
}
