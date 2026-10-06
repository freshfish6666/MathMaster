package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.AxiomEffectManager;
import com.freshfish.mathmaster.entity.EightEntity;
import com.freshfish.mathmaster.entity.NineEntity;
import com.freshfish.mathmaster.entity.SevenEntity;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMobEffects;
import com.freshfish.mathmaster.intellect.EntityIntellectDefinition;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class AxiomPassiveHandler {
    private static final int MAX_NEARBY_DIGITAL_MOBS = 12;

    @SubscribeEvent
    public void onNaturalDigitalMob(FinalizeSpawnEvent event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL
                || !(event.getEntity() instanceof NineEntity original)) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        double multiplier = AxiomEffectManager.getPeanoSpawnMultiplier(level, original.blockPosition());
        double wholeMultiplier = Math.floor(multiplier);
        int extraCount = Math.max(0, (int) wholeMultiplier - 1);
        double fraction = multiplier - wholeMultiplier;
        if (level.random.nextDouble() < fraction) {
            extraCount++;
        }
        int nearby = level.getEntitiesOfClass(
                NineEntity.class,
                original.getBoundingBox().inflate(96.0D),
                NineEntity::isAlive
        ).size();
        extraCount = Math.min(extraCount, Math.max(0, MAX_NEARBY_DIGITAL_MOBS - nearby - 1));
        for (int index = 0; index < extraCount; index++) {
            trySpawnDigitalMob(level, original.blockPosition(), level.random);
        }
    }

    @SubscribeEvent
    public void onDigitalMobKilled(LivingDeathEvent event) {
        var target = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        EntityType<? extends NineEntity> successorType;
        if (target instanceof SevenEntity) {
            successorType = ModEntities.EIGHT.get();
        } else if (target instanceof EightEntity) {
            successorType = ModEntities.NINE.get();
        } else {
            return;
        }
        int axiomLevel = AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.PEANO_AXIOMS);
        if (axiomLevel <= 0 || player.getRandom().nextFloat() >= 0.2F * axiomLevel) {
            return;
        }
        NineEntity successor = successorType.create(level);
        if (successor == null) {
            return;
        }
        successor.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        EventHooks.finalizeMobSpawn(
                successor,
                level,
                level.getCurrentDifficultyAt(successor.blockPosition()),
                MobSpawnType.TRIGGERED,
                null
        );
        level.addFreshEntity(successor);
    }

    @SubscribeEvent
    public void onIntelligentCreatureKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getEntity().getKillCredit() != player
                || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        var target = event.getEntity();
        if (target instanceof Player) {
            return;
        }
        EntityIntellectDefinition intellect = EntityIntellectManager.get(target);
        int axiomLevel = AxiomEffectManager.getEquippedLevel(
                player,
                AxiomDefinition.FUNDAMENTAL_THEOREM_OF_ARITHMETIC
        );
        boolean primeMarked = target.hasEffect(ModMobEffects.PRIME_MARK)
                || target.getData(ModAttachments.PRIME_MARK).isActive(level.getGameTime());
        if (intellect == null
                || axiomLevel <= 0
                || primeMarked
                || player.getRandom().nextFloat() >= 0.2F * axiomLevel) {
            return;
        }

        int dropCount = (countPrimeFactors(intellect.intellect()) + 1) / 2;
        if (dropCount <= 0) {
            return;
        }
        if (target.spawnAtLocation(new ItemStack(ModItems.PRIME_CORE.get(), dropCount)) == null) {
            return;
        }
        DigitalPollutionManager.add(player, axiomLevel);
    }

    static int countPrimeFactors(int value) {
        int remaining = Math.max(0, value);
        int count = 0;
        for (int divisor = 2; (long) divisor * divisor <= remaining; divisor++) {
            while (remaining % divisor == 0) {
                remaining /= divisor;
                count++;
            }
        }
        if (remaining > 1) {
            count++;
        }
        return count;
    }

    private static void trySpawnDigitalMob(ServerLevel level, BlockPos origin, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = origin.getX() + random.nextInt(9) - 4;
            int z = origin.getZ() + random.nextInt(9) - 4;
            BlockPos pos = findNearbyFloor(level, new BlockPos(x, origin.getY() + 3, z));
            if (pos == null) {
                continue;
            }
            // findNearbyFloor already validated this position; no world mutation occurs in between.
            EntityType<? extends NineEntity> type = random.nextFloat() < 0.8F
                    ? ModEntities.NINE.get()
                    : ModEntities.EIGHT.get();
            NineEntity mob = type.create(level);
            if (mob == null) {
                return;
            }
            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(mob) || !mob.checkSpawnObstruction(level)) {
                continue;
            }
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null);
            level.addFreshEntityWithPassengers(mob);
            return;
        }
    }

    private static BlockPos findNearbyFloor(ServerLevel level, BlockPos start) {
        for (int offset = 0; offset <= 6; offset++) {
            BlockPos candidate = start.below(offset);
            if (canSpawnAt(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean canSpawnAt(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getWorldBorder().isWithinBounds(pos)
                && level.getMaxLocalRawBrightness(pos) <= 5
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.above()).isEmpty()
                && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
