package com.freshfish.mathmaster.oracle;

import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.ritual.NAltarKnowledgeOffering;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Real block interactions enumerate every percentage roll, then exercise reload and tier isolation. */
public final class OracleOfferingCheck {
    private static int checks;

    public static int run(ServerLevel level) {
        checks = 0;
        Map<ResourceLocation, JsonElement> original = new HashMap<>();
        for (OracleTier tier : OracleTier.values()) {
            original.put(id("baseline/" + tier.id()), collection(tier, OracleManager.getPool(tier)));
        }
        List<OracleEntry> low = OracleManager.getPool(OracleTier.LOW);
        require(low.size() == 17, "approved replacement entries loaded through server reload listener");
        require(low.stream().map(OracleEntry::text).toList().equals(java.util.List.of(
                "道可道，非常道。",
                "天地不仁，以万物为刍狗；圣人不仁，以百姓为刍狗。",
                "死生，命也，其有夜旦之常，天也。",
                "生也有涯，而知也无涯。",
                "使生如夏花之绚烂，死如秋叶之静美。",
                "人跟树是一样，越是向往高处的阳光，它的根就越要伸向黑暗的地底。",
                "纷总总兮九州，何寿夭兮在予！",
                "生者为过客，死者为归人。天地一逆旅，同悲万古尘。",
                "你出自黑色深渊，或降自星辰？",
                "从我，是进入悲惨之城的道路；从我，是进入永恒的痛苦的道路。",
                "如同蜘蛛吐丝，如同草生长在地上……从那不朽者生出一切事物……",
                "方生方死，方死方生。",
                "路漫漫其修远兮，吾将上下而求索。",
                "当你凝视深渊时，深渊也在凝视你。",
                "你们走进来的，把一切希望抛在后面吧。",
                "这里必须根绝一切犹豫；这里任何怯懦都无济于事。",
                "以心为弓，以奥义为箭，以专注为弦，射向那不朽的目标。")), "exact replacement lines and order");
        require(OracleManager.getPool(OracleTier.MEDIUM).isEmpty()
                && OracleManager.getPool(OracleTier.HIGH).isEmpty(), "reserved tiers initially empty");
        try {
            low.add(new OracleEntry(id("mutation"), "must not be stored"));
            throw new AssertionError("Mutable oracle pool");
        } catch (UnsupportedOperationException expected) { checks++; }

        BlockPos pos = new BlockPos(145, 240, 145);
        var messages = new ArrayList<Component>();
        boolean[] spectator = {false};
        var player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "OracleOfferingCheck"), ClientInformation.createDefault()) {
            @Override public void displayClientMessage(Component message, boolean overlay) {
                require(!overlay, "oracle is a chat message");
                messages.add(message);
            }
            @Override public boolean isSpectator() { return spectator[0]; }
        };
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        player.getData(com.freshfish.mathmaster.init.ModAttachments.INSIGHT_RESULTS).unlockNAltar();
        level.setBlockAndUpdate(pos, ModBlocks.N_ALTAR.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), ModBlocks.N_ALTAR.get().defaultBlockState()
                .setValue(NAltarBlock.HALF, DoubleBlockHalf.UPPER));
        long[] seeds = seedsForRolls();
        try {
            Item[] items = {Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD, Items.DIAMOND};
            int[] chances = {30, 50, 70, 90};
            for (int item = 0; item < items.length; item++) {
                int successes = 0;
                for (int roll = 0; roll < 100; roll++) {
                    var expectedRandom = RandomSource.create(seeds[roll]);
                    require(expectedRandom.nextInt(100) == roll, "controlled percentage roll");
                    Component expected = roll < chances[item]
                            ? low.get(expectedRandom.nextInt(low.size())).message()
                            : Component.translatable("message.mathmaster.n_altar.unanswered");
                    var stack = new ItemStack(items[item], 5);
                    messages.clear();
                    level.random.setSeed(seeds[roll]);
                    require(interact(level, player, roll % 2 == 0 ? pos : pos.above(), stack,
                            roll % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND).consumesAction(), "accepted interaction");
                    require(stack.getCount() == 4 && messages.size() == 1, "exactly one item and one response");
                    require(messages.getFirst().equals(expected), "chance threshold, one draw, low pool and response");
                    if (roll < chances[item]) successes++;
                }
                require(successes == chances[item] && NAltarKnowledgeOffering.chance(new ItemStack(items[item])) == chances[item],
                        "exact success percentage for " + items[item]);
            }
            for (Item item : new Item[]{Items.COAL, Items.COPPER_INGOT, Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.DIRT}) {
                reject(level, player, pos, new ItemStack(item, 5), messages);
            }
            reject(level, player, pos, ItemStack.EMPTY, messages);
            spectator[0] = true;
            reject(level, player, pos, new ItemStack(Items.DIAMOND, 5), messages);
            spectator[0] = false;
            level.setBlockAndUpdate(pos.below(), ModBlocks.LINGXU_BLOCK.get().defaultBlockState());
            reject(level, player, pos, new ItemStack(Items.DIAMOND, 5), messages);
            reject(level, player, pos.above(), new ItemStack(Items.DIAMOND, 5), messages);
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());

            var extended = new HashMap<>(original);
            OracleEntry addition = new OracleEntry(id("low/addition"), "扩展测试神谕");
            extended.put(id("extra/low"), collection(OracleTier.LOW, List.of(addition, low.getFirst())));
            extended.put(id("extra/medium"), collection(OracleTier.MEDIUM, List.of(new OracleEntry(id("medium/test"), "中级"))));
            extended.put(id("extra/high"), collection(OracleTier.HIGH, List.of(new OracleEntry(id("high/test"), "高级"))));
            reload(level, extended);
            require(OracleManager.getPool(OracleTier.LOW).size() == low.size()+1, "append and ID deduplication");
            require(OracleManager.getPool(OracleTier.MEDIUM).size() == 1 && OracleManager.getPool(OracleTier.HIGH).size() == 1,
                    "independent tier pools");
            require(addition.message().getString().equals(addition.text()), "new entry needs no language file");
            var drawn = new HashSet<ResourceLocation>();
            var drawRandom = RandomSource.create(6172049);
            for (int draw = 0; draw < 200; draw++) {
                drawn.add(OracleManager.draw(OracleTier.LOW, drawRandom).orElseThrow().id());
            }
            require(drawn.size() == low.size()+1, "all appended low entries reachable");
            messages.clear();
            level.random.setSeed(seeds[0]);
            var stack = new ItemStack(Items.DIAMOND, 2);
            interact(level, player, pos, stack, InteractionHand.MAIN_HAND);
            require(OracleManager.getPool(OracleTier.LOW).stream().anyMatch(entry -> entry.message().equals(messages.getFirst())),
                    "altar stays low when other tiers populated");

            var malformed = new HashMap<>(original);
            JsonObject invalid = collection(OracleTier.LOW, List.of(addition));
            JsonObject bad = new JsonObject(); bad.addProperty("id", "invalid id"); bad.addProperty("text", "invalid");
            invalid.getAsJsonArray("entries").add(bad);
            malformed.put(id("extra/invalid"), invalid);
            reload(level, malformed);
            require(OracleManager.getPool(OracleTier.LOW).equals(low), "invalid collection is skipped atomically");
            reload(level, Map.of());
            require(OracleManager.getPool(OracleTier.LOW).isEmpty()
                    && OracleManager.draw(OracleTier.LOW, RandomSource.create(1)).isEmpty(), "reload clears removed entries");
            messages.clear();
            stack = new ItemStack(Items.DIAMOND, 2);
            interact(level, player, pos, stack, InteractionHand.MAIN_HAND);
            require(stack.getCount() == 2 && messages.size() == 1
                    && messages.getFirst().equals(Component.translatable("message.mathmaster.n_altar.no_oracles")), "empty pool preserves offering");
        } finally {
            reload(level, original);
            level.removeBlock(pos.above(), false);
            level.removeBlock(pos, false);
            level.removeBlock(pos.below(), false);
        }
        return checks;
    }

    private static ItemInteractionResult interact(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack, InteractionHand hand) {
        player.setItemInHand(hand, stack);
        return level.getBlockState(pos).useItemOn(stack, level, player, hand,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
    }

    private static void reject(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack stack, List<Component> messages) {
        int before = stack.getCount();
        messages.clear();
        level.random.setSeed(123);
        long expected = RandomSource.create(123).nextLong();
        require(!interact(level, player, pos, stack, InteractionHand.MAIN_HAND).consumesAction(), "rejected offering");
        require(stack.getCount() == before && messages.isEmpty() && level.random.nextLong() == expected,
                "rejection spends no item, message or RNG");
    }

    private static long[] seedsForRolls() {
        long[] seeds = new long[100];
        java.util.Arrays.fill(seeds, -1);
        for (long seed = 0; seed < 100000; seed++) {
            int roll = RandomSource.create(seed).nextInt(100);
            if (seeds[roll] == -1) seeds[roll] = seed;
        }
        for (long seed : seeds) if (seed == -1) throw new AssertionError("Missing percentage seed");
        return seeds;
    }

    private static JsonObject collection(OracleTier tier, List<OracleEntry> entries) {
        JsonObject result = new JsonObject(); result.addProperty("tier", tier.id());
        JsonArray array = new JsonArray();
        for (OracleEntry entry : entries) {
            JsonObject object = new JsonObject();
            object.addProperty("id", entry.id().toString()); object.addProperty("text", entry.text()); array.add(object);
        }
        result.add("entries", array); return result;
    }

    private static void reload(ServerLevel level, Map<ResourceLocation, JsonElement> resources) {
        OracleManager.INSTANCE.apply(resources, level.getServer().getResourceManager(), InactiveProfiler.INSTANCE);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mathmaster", path); }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("Oracle offering: " + message);
        checks++;
    }
}
