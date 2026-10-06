package com.freshfish.mathmaster.ritual;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.intelligence.IntelligenceData;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player runtime cooldowns survive altar/dimension changes without adding save fields. */
public final class NAltarOfferings {
    public static final NAltarOfferings INSTANCE = new NAltarOfferings();
    private final Map<UUID, EnumMap<Category, Long>> cooldowns = new HashMap<>();
    private final ArrayDeque<Guidance> guidance = new ArrayDeque<>();

    private NAltarOfferings() {}

    enum Category { FRUIT, GUIDANCE, DESECRATION }
    enum Punishment { LIGHTNING, BLINDNESS, SLOWNESS, WEAKNESS, POLLUTION }

    static boolean isDesecration(ItemStack stack) {
        return stack.is(Items.GLOWSTONE) || stack.is(ModItems.EMPTY_SET.get())
                || stack.is(Items.SEA_LANTERN) || stack.is(ModItems.THREE_CAT_MILK_POWDER.get());
    }

    static Punishment punishment(int roll) {
        if (roll < 20) return Punishment.LIGHTNING;
        if (roll < 45) return Punishment.BLINDNESS;
        if (roll < 70) return Punishment.SLOWNESS;
        if (roll < 90) return Punishment.WEAKNESS;
        return Punishment.POLLUTION;
    }

    public static boolean normal(Level level, BlockPos lower) {
        if (!level.hasChunkAt(lower)) return false;
        BlockState actual = level.getBlockState(lower);
        return actual.is(ModBlocks.N_ALTAR.get()) && actual.getValue(NAltarBlock.HALF) == DoubleBlockHalf.LOWER
                && !level.getBlockState(lower.below()).is(ModBlocks.LINGXU_BLOCK.get());
    }

    public ItemInteractionResult offer(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand) {
        Category category = stack.is(ModItems.COLLATZ_FRUIT.get()) ? Category.FRUIT
                : stack.is(ModItems.PRIME_CORE.get()) ? Category.GUIDANCE
                : isDesecration(stack) ? Category.DESECRATION : null;
        if (category == null) return NAltarKnowledgeOffering.offer(stack, state, level, pos, player);
        BlockPos lower = state.getValue(NAltarBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (stack.isEmpty() || player.isSpectator() || !player.isAlive() || !normal(level, lower)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return ItemInteractionResult.CONSUME;
        if (!NAltarAccess.allowOffering(player)) return ItemInteractionResult.CONSUME;
        long now = clock(serverPlayer);
        long expires = cooldowns.getOrDefault(player.getUUID(), new EnumMap<>(Category.class)).getOrDefault(category, 0L);
        if (expires > now) {
            message(player, "cooldown", (expires - now + 19) / 20);
            return ItemInteractionResult.CONSUME;
        }
        switch (category) {
            case FRUIT -> {
                if (IntelligenceManager.get(player).getIq() >= IntelligenceData.MAX_IQ) {
                    message(player, "iq_cap");
                    return ItemInteractionResult.CONSUME;
                }
                stack.shrink(1);
                setCooldown(serverPlayer, category, 100);
                int awarded = IntelligenceManager.addExperienceCappedAtIq(player, 20, IntelligenceData.MAX_IQ);
                message(player, "fruit", awarded);
                DigitalPollutionManager.add(player, 3);
            }
            case DESECRATION -> {
                stack.shrink(1);
                setCooldown(serverPlayer, category, 200);
                message(player, "displeased");
                applyPunishment(serverPlayer, punishment(level.random.nextInt(100)));
            }
            case GUIDANCE -> {
                if (!level.dimension().equals(Level.OVERWORLD)) {
                    message(player, "overworld_only");
                    return ItemInteractionResult.CONSUME;
                }
                if (guidance.stream().anyMatch(request -> request.player.getUUID().equals(player.getUUID()))) {
                    message(player, "searching");
                    return ItemInteractionResult.CONSUME;
                }
                guidance.add(new Guidance(serverPlayer, (ServerLevel) level, lower.immutable(), hand, stack,
                        new GatewayGuidanceSearch((ServerLevel) level, player.blockPosition()), now));
                message(player, "searching");
            }
        }
        return ItemInteractionResult.CONSUME;
    }

    private static void message(Player player, String key, Object... arguments) {
        player.displayClientMessage(Component.translatable("message.mathmaster.n_altar." + key, arguments), false);
    }

    private static long clock(ServerPlayer player) { return player.server.overworld().getGameTime(); }

    private void setCooldown(ServerPlayer player, Category category, int ticks) {
        cooldowns.computeIfAbsent(player.getUUID(), key -> new EnumMap<>(Category.class)).put(category, clock(player) + ticks);
    }

    static void applyPunishment(ServerPlayer player, Punishment punishment) {
        switch (punishment) {
            case LIGHTNING -> {
                var bolt = EntityType.LIGHTNING_BOLT.create(player.serverLevel());
                if (bolt != null) {
                    bolt.moveTo(player.position());
                    bolt.setVisualOnly(true);
                    player.serverLevel().addFreshEntity(bolt);
                }
                player.hurt(player.damageSources().lightningBolt(), 6F);
            }
            case BLINDNESS -> applyEffect(player, MobEffects.BLINDNESS, 0);
            case SLOWNESS -> applyEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 1);
            case WEAKNESS -> applyEffect(player, MobEffects.WEAKNESS, 0);
            case POLLUTION -> DigitalPollutionManager.add(player, 8);
        }
    }

    private static void applyEffect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        var existing = player.getEffect(effect);
        if (existing == null || (existing.getAmplifier() <= amplifier && existing.getDuration() < 200)) {
            player.addEffect(new MobEffectInstance(effect, 200, amplifier));
        }
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        cooldowns.values().forEach(entries -> entries.values().removeIf(expiry -> expiry <= now));
        cooldowns.values().removeIf(Map::isEmpty);
        guidance.removeIf(request -> {
            if (request.valid() && now - request.started <= 1200) return false;
            if (request.player.server.getPlayerList().getPlayer(request.player.getUUID()) == request.player
                    && request.player.isAlive()) message(request.player, "search_cancelled");
            return true;
        });
        Guidance request = guidance.peek();
        if (request == null) return;
        try {
            if (!request.search.advance()) return;
            guidance.remove();
            BlockPos result = request.search.result();
            if (result == null) message(request.player, "not_found");
            else {
                request.stack.shrink(1);
                setCooldown(request.player, Category.GUIDANCE, 1200);
                message(request.player, "guidance", result.getX(), result.getZ());
            }
        } catch (RuntimeException error) {
            guidance.remove();
            MathMaster.LOGGER.warn("N altar gateway guidance failed", error);
            message(request.player, "search_failed");
        }
    }

    @SubscribeEvent
    public void onStop(ServerStoppedEvent event) { cooldowns.clear(); guidance.clear(); }

    private record Guidance(ServerPlayer player, ServerLevel level, BlockPos altar, InteractionHand hand,
                            ItemStack stack, GatewayGuidanceSearch search, long started) {
        boolean valid() {
            return player.server.getPlayerList().getPlayer(player.getUUID()) == player && player.isAlive()
                    && !player.isSpectator() && player.level() == level && normal(level, altar)
                    && player.getItemInHand(hand) == stack && !stack.isEmpty() && stack.is(ModItems.PRIME_CORE.get());
        }
    }
}
