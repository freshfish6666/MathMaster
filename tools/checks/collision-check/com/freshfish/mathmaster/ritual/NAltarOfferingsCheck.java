package com.freshfish.mathmaster.ritual;

import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Real players and block interactions, with ordinary world generation for gateway guidance. */
public final class NAltarOfferingsCheck {
    private static int checks;
    private static final NAltarOfferings HANDLER = NAltarOfferings.INSTANCE;
    private static final BlockPos ALTAR = new BlockPos(165, 240, 165);

    public static int run(ServerLevel level) throws Exception {
        checks = 0;
        long originalTime = level.getGameTime();
        place(level, ALTAR);
        place(level, ALTAR.east(3));
        try (var fixture = new PlayerFixture(level)) {
            var player = fixture.player;
            player.getData(ModAttachments.INSIGHT_RESULTS).unlockNAltar();
            player.setPos(165.5, 240, 167);
            var intellect = IntelligenceManager.get(player);
            var pollution = player.getData(ModAttachments.DIGITAL_POLLUTION);
            intellect.setIq(50);
            long before = intellect.getTotalExperience();
            var fruit = new ItemStack(ModItems.COLLATZ_FRUIT.get(), 5);
            interact(level, player, ALTAR.above(), fruit, InteractionHand.OFF_HAND);
            require(fruit.getCount() == 4 && intellect.getTotalExperience() - before == 20 && pollution.getValue() == 3,
                    "fruit gives 20 experience, 3 pollution and consumes one");
            require(!player.hasEffect(com.freshfish.mathmaster.init.ModMobEffects.DIGITAL_POLLUTION), "direct pollution only");
            before = intellect.getTotalExperience();
            interact(level, player, ALTAR.east(3), fruit, InteractionHand.OFF_HAND);
            require(fruit.getCount() == 4 && intellect.getTotalExperience() == before && pollution.getValue() == 3, "fruit cooldown across altars");
            time(level, 99);
            interact(level, player, ALTAR, fruit, InteractionHand.OFF_HAND);
            require(fruit.getCount() == 4, "fruit cooldown just before expiry");
            time(level, 1);
            interact(level, player, ALTAR, fruit, InteractionHand.OFF_HAND);
            require(fruit.getCount() == 3 && intellect.getTotalExperience() == before + 20, "fruit cooldown exact expiry");
            HANDLER.onStop(null);
            intellect.setIq(200); pollution.reset();
            interact(level, player, ALTAR, fruit, InteractionHand.MAIN_HAND);
            require(fruit.getCount() == 3 && pollution.getValue() == 0 && fixture.lastKey().endsWith("iq_cap"), "cap rejects all costs");
            intellect.setIq(199); intellect.setExperience(intellect.getXpNeededForNextIq() - 5);
            before = intellect.getTotalExperience();
            interact(level, player, ALTAR, fruit, InteractionHand.MAIN_HAND);
            require(intellect.getIq() == 200 && intellect.getTotalExperience() == before + 5 && fruit.getCount() == 2
                    && pollution.getValue() == 3, "near cap follows existing experience clamp");

            Item[] desecration = {Items.GLOWSTONE, ModItems.EMPTY_SET.get(), Items.SEA_LANTERN, ModItems.THREE_CAT_MILK_POWDER.get()};
            int[] expectedCounts = {20, 25, 25, 20, 10};
            long[] seeds = seeds();
            for (Item item : desecration) {
                int[] counts = new int[5];
                for (int roll = 0; roll < 100; roll++) {
                    HANDLER.onStop(null);
                    pollution.reset(); player.removeAllEffects(); player.setHealth(20); player.invulnerableTime = 0;
                    var stack = new ItemStack(item, 4);
                    level.random.setSeed(seeds[roll]);
                    var kind = NAltarOfferings.punishment(roll);
                    counts[kind.ordinal()]++;
                    interact(level, player, roll % 2 == 0 ? ALTAR : ALTAR.above(), stack,
                            roll % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                    require(stack.getCount() == 3 && fixture.lastKey().endsWith("displeased"), "desecration cost and response");
                    switch (kind) {
                        case LIGHTNING -> require(player.getHealth() == 14 && pollution.getValue() == 0, "one six-point lightning hit");
                        case BLINDNESS -> require(player.getEffect(MobEffects.BLINDNESS).getDuration() == 200
                                && player.getEffect(MobEffects.BLINDNESS).getAmplifier() == 0, "blindness within ten seconds");
                        case SLOWNESS -> require(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() == 200
                                && player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 1, "slowness II within ten seconds");
                        case WEAKNESS -> require(player.getEffect(MobEffects.WEAKNESS).getDuration() == 200
                                && player.getEffect(MobEffects.WEAKNESS).getAmplifier() == 0, "weakness within ten seconds");
                        case POLLUTION -> require(pollution.getValue() == 8 && player.getHealth() == 20, "eight direct pollution");
                    }
                    clearLightning(level);
                }
                require(java.util.Arrays.equals(counts, expectedCounts), "exact punishment percentages " + item);
            }
            HANDLER.onStop(null); player.removeAllEffects(); pollution.reset();
            var bad = new ItemStack(Items.GLOWSTONE, 4);
            level.random.setSeed(seeds[99]);
            interact(level, player, ALTAR, bad, InteractionHand.MAIN_HAND);
            interact(level, player, ALTAR.east(3), bad, InteractionHand.MAIN_HAND);
            require(bad.getCount() == 3 && pollution.getValue() == 8, "shared desecration cooldown across altars");
            time(level, 199);
            interact(level, player, ALTAR, bad, InteractionHand.MAIN_HAND);
            require(bad.getCount() == 3, "desecration cooldown before expiry");
            time(level, 1); level.random.setSeed(seeds[99]);
            interact(level, player, ALTAR, bad, InteractionHand.MAIN_HAND);
            require(bad.getCount() == 2 && pollution.getValue() == 16, "desecration cooldown expiry");
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            NAltarOfferings.applyPunishment(player, NAltarOfferings.Punishment.SLOWNESS);
            require(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 3
                    && player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() == 40, "stronger external effect untouched");
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 500, 0));
            NAltarOfferings.applyPunishment(player, NAltarOfferings.Punishment.BLINDNESS);
            require(player.getEffect(MobEffects.BLINDNESS).getDuration() == 500, "longer external effect untouched");
            player.removeAllEffects(); player.setHealth(20); player.invulnerableTime = 0;
            level.setBlockAndUpdate(ALTAR.south(3), Blocks.OAK_PLANKS.defaultBlockState());
            NAltarOfferings.applyPunishment(player, NAltarOfferings.Punishment.LIGHTNING);
            for (LightningBolt bolt : level.getEntitiesOfClass(LightningBolt.class, new AABB(ALTAR).inflate(8))) {
                for (int tick = 0; tick < 10; tick++) bolt.tick();
            }
            require(player.getHealth() == 14 && !player.isOnFire()
                    && level.getBlockState(ALTAR.south(3)).is(Blocks.OAK_PLANKS), "visual lightning adds no repeated damage or fire");
            clearLightning(level);

            HANDLER.onStop(null); player.setHealth(20); pollution.reset(); intellect.setIq(50);
            level.setBlockAndUpdate(ALTAR.below(), ModBlocks.LINGXU_BLOCK.get().defaultBlockState());
            for (Item item : new Item[]{ModItems.COLLATZ_FRUIT.get(), ModItems.PRIME_CORE.get(), Items.GLOWSTONE}) {
                var stack = new ItemStack(item, 2);
                interact(level, player, ALTAR.above(), stack, InteractionHand.MAIN_HAND);
                require(stack.getCount() == 2, "blood mode rejects new offering");
            }
            level.setBlockAndUpdate(ALTAR.below(), Blocks.STONE.defaultBlockState());
            var dust = new ItemStack(Items.GLOWSTONE_DUST, 2);
            interact(level, player, ALTAR, dust, InteractionHand.MAIN_HAND);
            require(dust.getCount() == 2, "glowstone powder not classified");

            var axiomCase = new ItemStack(ModItems.AXIOM_CASE.get());
            axiomCase.set(net.minecraft.core.component.DataComponents.CONTAINER,
                    net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(
                            com.freshfish.mathmaster.item.StudyNoteItem.createAxiomNote(
                                    com.freshfish.mathmaster.axiom.AxiomDefinition.ADDITIVE_IDENTITY, 5))));
            var curios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow();
            curios.setEquippedCurio("axiom_case", 0, axiomCase);
            pollution.reset();
            for (int tick = 0; tick < 1200; tick++) pollution.tickAdditiveIdentity(5);
            HANDLER.onStop(null);
            var shieldFruit = new ItemStack(ModItems.COLLATZ_FRUIT.get(), 2);
            before = intellect.getTotalExperience();
            interact(level, player, ALTAR, shieldFruit, InteractionHand.MAIN_HAND);
            require(shieldFruit.getCount() == 1 && intellect.getTotalExperience() == before + 20
                    && pollution.getValue() == 1 && pollution.getIdentityShields() == 0,
                    "fruit respects identity shield without cancelling experience");
            for (int tick = 0; tick < 1200; tick++) pollution.tickAdditiveIdentity(5);
            HANDLER.onStop(null); level.random.setSeed(seeds[99]);
            var shieldBad = new ItemStack(Items.SEA_LANTERN, 2);
            interact(level, player, ALTAR, shieldBad, InteractionHand.MAIN_HAND);
            require(shieldBad.getCount() == 1 && pollution.getValue() == 1 && pollution.getIdentityShields() == 0,
                    "desecration respects identity shield");
            curios.setEquippedCurio("axiom_case", 0, ItemStack.EMPTY);
            HANDLER.onStop(null);

            // Compare normal/giant starts in candidate order using real generation.
            var candidates = GatewayGuidanceSearch.candidates(level, player.blockPosition());
            if (candidates.isEmpty()) {
                // The collision world intentionally disables structure generation.
                var emptyCore = new ItemStack(ModItems.PRIME_CORE.get(), 3);
                interact(level, player, ALTAR, emptyCore, InteractionHand.MAIN_HAND);
                HANDLER.onTick(new ServerTickEvent.Post(() -> true, level.getServer()));
                require(pending() == 0 && emptyCore.getCount() == 3 && fixture.lastKey().endsWith("not_found"),
                        "no structures means no cost or cooldown");
            } else {
            long previous = -1;
            for (var candidate : candidates) {
                require(candidate.distance() >= previous && candidate.distance() <= 4096L * 4096,
                        "candidate order and exact horizontal radius");
                previous = candidate.distance();
            }
            BlockPos expected = null;
            int references = 0;
            net.minecraft.world.level.levelgen.structure.StructureStart selected = null;
            for (var candidate : candidates) {
                var chunk = level.getChunkSource().getChunk(candidate.chunk().x, candidate.chunk().z, ChunkStatus.STRUCTURE_STARTS, true);
                var start = chunk.getStartForStructure(candidate.structure().value());
                if (start != null && start.isValid()) {
                    expected = candidate.locate(); selected = start; references = start.getReferences(); break;
                }
            }
            require(expected != null, "a naturally generated gateway within radius");
            var core = new ItemStack(ModItems.PRIME_CORE.get(), 3);
            interact(level, player, ALTAR.above(), core, InteractionHand.OFF_HAND);
            require(core.getCount() == 3 && fixture.lastKey().endsWith("searching"), "guidance starts without spending");
            interact(level, player, ALTAR, core, InteractionHand.OFF_HAND);
            require(pending() == 1 && core.getCount() == 3, "duplicate request rejected");
            for (int step = 0; step < 1000 && pending() > 0; step++) HANDLER.onTick(new ServerTickEvent.Post(() -> true, level.getServer()));
            require(pending() == 0 && core.getCount() == 2 && fixture.lastKey().endsWith("guidance"), "async guidance completes with one cost");
            Object[] coordinates = ((TranslatableContents) fixture.messages.getLast().getContents()).getArgs();
            require(coordinates[0].equals(expected.getX()) && coordinates[1].equals(expected.getZ())
                    && selected.getReferences() == references, "nearest coordinate and explored eligibility unchanged");
            interact(level, player, ALTAR.east(3), core, InteractionHand.OFF_HAND);
            require(core.getCount() == 2 && pending() == 0 && fixture.lastKey().endsWith("cooldown"), "guidance cooldown across altars");
            time(level, 1199); interact(level, player, ALTAR, core, InteractionHand.OFF_HAND);
            require(pending() == 0, "guidance before expiry");
            time(level, 1); interact(level, player, ALTAR, core, InteractionHand.OFF_HAND);
            require(pending() == 1 && core.getCount() == 2, "guidance exact cooldown expiry");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            HANDLER.onTick(new ServerTickEvent.Post(() -> true, level.getServer()));
            require(pending() == 0 && core.getCount() == 2 && fixture.lastKey().endsWith("search_cancelled"), "switching held stack cancels without cost");
            }

            HANDLER.onStop(null);
            var core = new ItemStack(ModItems.PRIME_CORE.get(), 2);
            var nether = level.getServer().getLevel(Level.NETHER);
            place(nether, ALTAR);
            player.setServerLevel(nether);
            interact(nether, player, ALTAR, core, InteractionHand.MAIN_HAND);
            require(core.getCount() == 2 && pending() == 0 && fixture.lastKey().endsWith("overworld_only"), "guidance rejected outside overworld");
            player.setServerLevel(level);
            nether.removeBlock(ALTAR.above(), false); nether.removeBlock(ALTAR, false);
            for (boolean useFruit : new boolean[]{true, false}) {
                HANDLER.onStop(null);
                try (var doomed = new PlayerFixture(level)) {
                    var victim = doomed.player;
                    victim.getData(ModAttachments.INSIGHT_RESULTS).unlockNAltar();
                    victim.setPos(165.5, 240, 167);
                    IntelligenceManager.get(victim).setIq(50);
                    victim.getData(ModAttachments.DIGITAL_POLLUTION).add(useFruit ? 98 : 93);
                    var lethalStack = new ItemStack(useFruit ? ModItems.COLLATZ_FRUIT.get() : Items.GLOWSTONE, 2);
                    level.random.setSeed(seeds[99]);
                    interact(level, victim, ALTAR, lethalStack, InteractionHand.MAIN_HAND);
                    require(lethalStack.getCount() == 1 && !victim.isAlive()
                            && victim.getData(ModAttachments.DIGITAL_POLLUTION).getValue() == 0,
                            "offering pollution retains existing lethal threshold and reset");
                }
            }
        } finally {
            HANDLER.onStop(null);
            level.getServer().getWorldData().overworldData().setGameTime(originalTime);
            clearLightning(level);
            for (var pos : new BlockPos[]{ALTAR, ALTAR.east(3)}) {
                level.removeBlock(pos.above(), false); level.removeBlock(pos, false); level.removeBlock(pos.below(), false);
            }
            level.removeBlock(ALTAR.south(3), false);
        }
        return checks;
    }

    private static void place(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, ModBlocks.N_ALTAR.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), ModBlocks.N_ALTAR.get().defaultBlockState().setValue(NAltarBlock.HALF, DoubleBlockHalf.UPPER));
    }
    private static void interact(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack, InteractionHand hand) {
        player.setItemInHand(hand, stack);
        level.getBlockState(pos).useItemOn(stack, level, player, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
    }
    private static void time(ServerLevel level, int ticks) {
        level.getServer().getWorldData().overworldData().setGameTime(level.getGameTime() + ticks);
    }
    private static void clearLightning(ServerLevel level) {
        level.getEntitiesOfClass(LightningBolt.class, new AABB(ALTAR).inflate(8)).forEach(LightningBolt::discard);
    }
    private static int pending() throws Exception {
        var field = NAltarOfferings.class.getDeclaredField("guidance"); field.setAccessible(true);
        return ((ArrayDeque<?>) field.get(HANDLER)).size();
    }
    private static long[] seeds() {
        long[] seeds = new long[100]; java.util.Arrays.fill(seeds, -1);
        for (long seed = 0; seed < 100000; seed++) {
            int roll = RandomSource.create(seed).nextInt(100); if (seeds[roll] == -1) seeds[roll] = seed;
        }
        return seeds;
    }
    private static void require(boolean condition, String text) {
        if (!condition) throw new AssertionError("N altar offerings: " + text); checks++;
    }

    private static final class PlayerFixture implements AutoCloseable {
        final List<Component> messages = new ArrayList<>();
        final ServerPlayer player;
        final List<ServerPlayer> players;
        final Map<UUID, ServerPlayer> ids;
        @SuppressWarnings("unchecked") PlayerFixture(ServerLevel level) throws Exception {
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "NOfferingCheck"), ClientInformation.createDefault()) {
                @Override public void displayClientMessage(Component message, boolean overlay) { messages.add(message); }
            };
            player.connection = new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) {}
                @Override public void send(Packet<?> packet, PacketSendListener listener) {}
            };
            var immunity = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            immunity.setAccessible(true); immunity.setInt(player, 0);
            var field = PlayerList.class.getDeclaredField("players"); field.setAccessible(true); players = (List<ServerPlayer>) field.get(level.getServer().getPlayerList());
            field = PlayerList.class.getDeclaredField("playersByUUID"); field.setAccessible(true); ids = (Map<UUID, ServerPlayer>) field.get(level.getServer().getPlayerList());
            level.addNewPlayer(player); players.add(player); ids.put(player.getUUID(), player);
        }
        String lastKey() { return ((TranslatableContents) messages.getLast().getContents()).getKey(); }
        @Override public void close() { players.remove(player); ids.remove(player.getUUID()); player.discard(); }
    }
}
