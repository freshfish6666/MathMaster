package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.block.entity.NAltarBlockEntity;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMobEffects;
import com.freshfish.mathmaster.intellect.InsightResultData;
import com.freshfish.mathmaster.intellect.InsightResultManager;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.ritual.NAltarAccess;
import com.freshfish.mathmaster.ritual.NAltarBloodDifficulty;
import com.freshfish.mathmaster.ritual.NAltarOfferings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.ArrayList;
import java.util.List;

/** Uses actual server players, block interactions, entity effects and player attachment persistence. */
public final class NAltarIntellectCheck {
    private static int checks;
    private static final BlockPos POS = new BlockPos(400, 240, 400);
    private static final Vec3 CENTER = Vec3.atCenterOf(POS);
    private static final String[] SPECIES = {"zombie", "cow", "pig", "sheep", "skeleton"};

    public static int run(ServerLevel level) throws Exception {
        checks = 0;
        long oldTime = level.getGameTime();
        var savedBlocks = List.of(level.getBlockState(POS.below()), level.getBlockState(POS), level.getBlockState(POS.above()));
        var mobs = new ArrayList<Mob>();
        try (var first = new SkillCheckPlayer(level, "AltarIntellect1");
             var second = new SkillCheckPlayer(level, "AltarIntellect2")) {
            var player = first.player;
            var other = second.player;
            player.setPos(CENTER.add(2, 0, 0));
            other.setPos(CENTER.add(30, 0, 0));
            player.setGameMode(GameType.SURVIVAL);
            other.setGameMode(GameType.SURVIVAL);
            level.setBlockAndUpdate(POS.below(), ModBlocks.LINGXU_BLOCK.get().defaultBlockState());
            level.setBlockAndUpdate(POS, ModBlocks.N_ALTAR.get().defaultBlockState());
            level.setBlockAndUpdate(POS.above(), ModBlocks.N_ALTAR.get().defaultBlockState().setValue(NAltarBlock.HALF, DoubleBlockHalf.UPPER));
            var altar = (NAltarBlockEntity) level.getBlockEntity(POS);
            var zombie = EntityType.ZOMBIE.create(level);
            var nine = ModEntities.NINE.get().create(level);
            var cow = EntityType.COW.create(level);
            var outside = EntityType.ZOMBIE.create(level);
            for (var mob : new Mob[]{zombie, nine, cow, outside}) {
                mobs.add(mob); mob.setNoAi(true); mob.setNoGravity(true);
                mob.setPos(CENTER.add(mob == outside ? 10.01 : 3, 0, 0));
                level.addFreshEntity(mob);
            }
            double baseAttack = zombie.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
            double baseSpeed = zombie.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            int[] iqCases = {49, 50, 100, 101, 159, 160, 200};
            for (int iq : iqCases) {
                IntelligenceManager.get(player).setIq(iq);
                mobs.forEach(Mob::removeAllEffects);
                refresh(level, altar);
                for (var mob : new Mob[]{zombie, nine}) {
                    require(mob.hasEffect(MobEffects.DAMAGE_BOOST) == (iq < 50), "strength boundary " + iq);
                    require(mob.hasEffect(MobEffects.MOVEMENT_SPEED) == (iq <= 100), "speed boundary " + iq);
                    require(mob.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) == (iq > 100), "slow boundary " + iq);
                    require(mob.hasEffect(MobEffects.WEAKNESS) == (iq >= 160), "weakness boundary " + iq);
                    for (var effect : mob.getActiveEffects()) require(effect.getAmplifier() == 0 && effect.getDuration() == 25, "tier level I duration");
                }
                require(cow.getActiveEffects().isEmpty() && outside.getActiveEffects().isEmpty(), "passive/outside not affected");
            }
            require(zombie.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() == baseAttack
                    && zombie.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() == baseSpeed, "base attributes unchanged");

            other.setPos(CENTER.add(12, 0, 0));
            IntelligenceManager.get(player).setIq(40); IntelligenceManager.get(other).setIq(160);
            require(NAltarBloodDifficulty.averageIq(level, CENTER) == 100, "mean includes exact 12 boundary");
            IntelligenceManager.get(player).setIq(100); IntelligenceManager.get(other).setIq(101);
            require(NAltarBloodDifficulty.averageIq(level, CENTER) == 100.5, "fractional mean not truncated");
            mobs.forEach(Mob::removeAllEffects); refresh(level, altar);
            require(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !zombie.hasEffect(MobEffects.MOVEMENT_SPEED), "fractional mean selects slow");
            other.setPos(CENTER.add(12.01, 0, 0));
            require(NAltarBloodDifficulty.averageIq(level, CENTER) == 100, "outside 12 excluded");
            other.setPos(CENTER.add(9, 9, 0));
            require(NAltarBloodDifficulty.averageIq(level, CENTER) == 100, "radius is spherical");
            other.setPos(CENTER.add(12, 0, 0));
            other.setGameMode(GameType.CREATIVE);
            require(NAltarBloodDifficulty.averageIq(level, CENTER) == 100, "creative excluded");
            other.setGameMode(GameType.SPECTATOR);
            require(NAltarBloodDifficulty.averageIq(level, CENTER) == 100, "spectator excluded");
            other.setGameMode(GameType.SURVIVAL); other.setHealth(0);
            require(NAltarBloodDifficulty.averageIq(level, CENTER) == 100, "dead excluded");
            other.setHealth(20); other.setPos(CENTER.add(30, 0, 0));
            player.setPos(CENTER.add(30, 0, 0));
            require(NAltarBloodDifficulty.averageIq(level, CENTER) == 0, "empty player radius defaults low");
            player.setPos(CENTER.add(2, 0, 0));
            IntelligenceManager.get(player).setIq(150);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GRADUATION_CAP.get()));
            require(NAltarBloodDifficulty.averageIq(level, CENTER) == 160, "equipment effective IQ");
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.addEffect(new MobEffectInstance(ModMobEffects.FLOW, 200, 1));
            require(NAltarBloodDifficulty.averageIq(level, CENTER) == 160, "Flow effective IQ");
            player.removeAllEffects();

            // Vanilla short effects expire naturally when a tier changes; external effects are never removed.
            IntelligenceManager.get(player).setIq(40); mobs.forEach(Mob::removeAllEffects); refresh(level, altar);
            IntelligenceManager.get(player).setIq(160);
            for (int i = 0; i < 20; i++) zombie.tick();
            refresh(level, altar);
            for (int i = 0; i < 5; i++) zombie.tick();
            require(!zombie.hasEffect(MobEffects.DAMAGE_BOOST) && !zombie.hasEffect(MobEffects.MOVEMENT_SPEED)
                    && zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && zombie.hasEffect(MobEffects.WEAKNESS), "old tier naturally expires");
            for (var effect : List.of(MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS)) {
                zombie.addEffect(new MobEffectInstance(effect, 600, 3));
            }
            for (int iq : iqCases) {
                IntelligenceManager.get(player).setIq(iq); refresh(level, altar);
                for (var effect : zombie.getActiveEffects()) require(effect.getAmplifier() == 3 && effect.getDuration() == 600, "external stronger/longer preserved " + iq);
            }

            // All four categories reject before any resource or cooldown changes.
            level.setBlockAndUpdate(POS.below(), Blocks.STONE.defaultBlockState());
            IntelligenceManager.get(player).setIq(51);
            var records = player.getData(ModAttachments.INSIGHT_RESULTS);
            for (int i = 0; i < 4; i++) records.record(id(SPECIES[i]), 1, 1);
            require(!NAltarAccess.tryUnlock(player), "four species insufficient");
            records.record(id("zombie"), 1, 1);
            records.record(id("skeleton"), 0, 1);
            records.record(id("skeleton"), 1, 2);
            records.record(id("player"), 0, 0);
            require(records.getDistinctEntityTypeCount() == 4, "duplicates failed partial and zero-question sessions do not count");
            var rejected = List.of(Items.IRON_INGOT, ModItems.COLLATZ_FRUIT.get(), ModItems.PRIME_CORE.get(), Items.GLOWSTONE);
            long experience = IntelligenceManager.get(player).getTotalExperience();
            var pollution = player.getData(ModAttachments.DIGITAL_POLLUTION);
            pollution.reset();
            for (var item : rejected) for (var pos : List.of(POS, POS.above())) for (var hand : InteractionHand.values()) {
                var stack = new ItemStack(item, 3);
                first.chatMessages.clear();
                level.random.setSeed(49103L);
                require(interact(level, player, pos, stack, hand) == ItemInteractionResult.CONSUME && stack.getCount() == 3, "locked rejects freely " + item);
                require(level.random.nextLong() == net.minecraft.util.RandomSource.create(49103L).nextLong(), "locked does not consume offering random draws");
                require(pollution.getValue() == 0 && IntelligenceManager.get(player).getTotalExperience() == experience, "locked no xp/pollution");
                require(first.chatMessages.size() == 1 && first.chatMessages.getFirst().getContents() instanceof TranslatableContents hint
                        && hint.getKey().equals("message.mathmaster.n_altar.locked")
                        && java.util.Arrays.equals(hint.getArgs(), new Object[]{51, 4, 50, 5}), "progress hint arguments");
            }
            var unknown = new ItemStack(Items.STICK);
            require(interact(level, player, POS, unknown, InteractionHand.MAIN_HAND) == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION, "unknown item keeps vanilla fallback");
            first.chatMessages.clear();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            level.getBlockState(POS.above()).useWithoutItem(level, player, new BlockHitResult(CENTER, Direction.UP, POS.above(), false));
            require(first.chatMessages.size() == 1, "empty hand also shows locked hint");
            level.setBlockAndUpdate(POS.below(), ModBlocks.LINGXU_BLOCK.get().defaultBlockState());
            var iron = new ItemStack(Items.IRON_INGOT, 2);
            require(interact(level, player, POS, iron, InteractionHand.MAIN_HAND) == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                    && iron.getCount() == 2, "blood does not require normal access");
            level.setBlockAndUpdate(POS.below(), Blocks.STONE.defaultBlockState());

            records.record(id("skeleton"), 1, 1); IntelligenceManager.get(player).setIq(50);
            require(!NAltarAccess.tryUnlock(player), "IQ exactly 50 cannot unlock with five species");
            // Equipment can grant the threshold, and removing it cannot revoke permanent access.
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GRADUATION_CAP.get()));
            first.chatMessages.clear(); new NAltarAccess().onPlayerTick(new PlayerTickEvent.Post(player));
            require(records.hasUnlockedNAltar() && first.chatMessages.size() == 1, "equipment tick permanently unlocks once");
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); IntelligenceManager.get(player).setIq(1);
            new NAltarAccess().onPlayerTick(new PlayerTickEvent.Post(player));
            require(NAltarAccess.tryUnlock(player) && first.chatMessages.size() == 1, "IQ reduction keeps access and no repeated notification");
            var fruit = new ItemStack(ModItems.COLLATZ_FRUIT.get(), 2);
            require(interact(level, player, POS, fruit, InteractionHand.MAIN_HAND) == ItemInteractionResult.CONSUME
                    && fruit.getCount() == 1 && pollution.getValue() == 3, "rejection did not start fruit cooldown");
            require(!NAltarAccess.tryUnlock(other), "player unlock is not altar-wide");

            var saved = records.serializeNBT(level.registryAccess());
            var loaded = new InsightResultData(); loaded.deserializeNBT(level.registryAccess(), saved);
            require(loaded.hasUnlockedNAltar() && loaded.getDistinctEntityTypeCount() == 5
                    && loaded.getCompletedInsights() == records.getCompletedInsights(), "save reload retains flag and historical records");
            saved.remove("n_altar_unlocked");
            loaded.deserializeNBT(level.registryAccess(), saved);
            require(!loaded.hasUnlockedNAltar() && loaded.getDistinctEntityTypeCount() == 5, "legacy missing flag keeps records and starts locked");
            other.setData(ModAttachments.INSIGHT_RESULTS, loaded); IntelligenceManager.get(other).setIq(50);
            require(!NAltarAccess.tryUnlock(other), "legacy records obey strict IQ boundary");
            IntelligenceManager.setIq(other, 51);
            require(loaded.hasUnlockedNAltar(), "IQ mutation immediately unlocks legacy records");
            // Confirm copy-on-death through the real respawn copy path.
            try (var respawn = new SkillCheckPlayer(level, "AltarRespawn")) {
                respawn.player.restoreFrom(player, false);
                require(respawn.player.getData(ModAttachments.INSIGHT_RESULTS).hasUnlockedNAltar(), "actual death clone keeps permanent flag");
            }
            loaded = new InsightResultData();
            for (int i = 0; i < 4; i++) loaded.record(id(SPECIES[i]), 1, 1);
            other.setData(ModAttachments.INSIGHT_RESULTS, loaded); IntelligenceManager.get(other).setIq(51);
            InsightResultManager.record(other, id("skeleton"), 0, 1, 1);
            require(loaded.hasUnlockedNAltar(), "fifth successful insight unlocks immediately even without xp award");
        } finally {
            mobs.forEach(Mob::discard);
            NAltarOfferings.INSTANCE.onStop(null);
            for (int i = 0; i < 3; i++) level.setBlockAndUpdate(POS.offset(0, i - 1, 0), savedBlocks.get(i));
            ((ServerLevelData) level.getLevelData()).setGameTime(oldTime);
        }
        return checks;
    }

    private static ResourceLocation id(String path) { return ResourceLocation.withDefaultNamespace(path); }
    private static void refresh(ServerLevel level, NAltarBlockEntity altar) {
        long phase = Math.floorMod(-POS.asLong(), 20);
        ((ServerLevelData) level.getLevelData()).setGameTime(phase);
        NAltarBlockEntity.serverTick(level, POS, level.getBlockState(POS), altar);
    }
    private static ItemInteractionResult interact(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack, InteractionHand hand) {
        player.setItemInHand(hand, stack);
        return level.getBlockState(pos).useItemOn(stack, level, player, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
    private static void require(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError("N altar intellect: " + message);
    }
}
