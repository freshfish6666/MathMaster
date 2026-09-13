package com.freshfish.mathmaster.item;

import com.freshfish.mathmaster.intellect.EntityIntellectDefinition;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.intellect.InsightQuestionManager;
import com.freshfish.mathmaster.intellect.InsightTargeting;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.menu.InsightQuizMenu;
import com.freshfish.mathmaster.network.InsightFailurePayload;
import com.freshfish.mathmaster.network.SelfInsightPayload;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.intelligence.IntelligenceData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class LingxuMirrorItem extends Item {
    public static final int INSIGHT_DURATION_TICKS = 60;
    public static final int SELF_INSIGHT_DURATION_TICKS = 40;
    private static final int ACTIVE_USE_DURATION_TICKS = 72_000;
    private static final Map<ServerPlayer, InsightCharge> ACTIVE_INSIGHTS = new WeakHashMap<>();

    public LingxuMirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        LivingEntity target = InsightTargeting.findTarget(player);
        if (target == null) {
            return new InteractionResultHolder<>(tryStartSelfInsight(player, usedHand), stack);
        }

        InteractionResult result = tryStartInsight(player, usedHand, target);
        return new InteractionResultHolder<>(result, stack);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity interactionTarget,
            InteractionHand usedHand
    ) {
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        return tryStartInsight(player, usedHand, interactionTarget);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) {
            return;
        }

        if (!(livingEntity instanceof ServerPlayer player)
                || player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            if (livingEntity instanceof ServerPlayer player) {
                ACTIVE_INSIGHTS.remove(player);
            }
            livingEntity.releaseUsingItem();
            return;
        }

        InsightCharge charge = ACTIVE_INSIGHTS.get(player);
        if (charge == null) {
            player.releaseUsingItem();
            return;
        }

        LivingEntity target = InsightTargeting.findTarget(player);
        if (charge.isSelfInsight()) {
            if (target != null) {
                player.releaseUsingItem();
                return;
            }

            charge.increaseSelf();
            if (charge.progressTicks() >= SELF_INSIGHT_DURATION_TICKS) {
                ACTIVE_INSIGHTS.remove(player);
                player.releaseUsingItem();
                openSelfInsight(player);
                player.getCooldowns().addCooldown(this, 10);
            }
            return;
        }

        if (target == null) {
            charge.decrease();
            return;
        }

        EntityIntellectDefinition definition = EntityIntellectManager.get(target);
        if (definition == null
                || definition.intellect() - IntelligenceManager.getEffectiveIq(player) > 30) {
            player.releaseUsingItem();
            return;
        }

        if (!InsightQuestionManager.canCreateSession(definition.difficulty())) {
            player.releaseUsingItem();
            player.displayClientMessage(
                    Component.translatable("message.mathmaster.insight.no_questions"),
                    true
            );
            return;
        }

        charge.increase(target.getType());
        if (charge.progressTicks() >= INSIGHT_DURATION_TICKS) {
            ACTIVE_INSIGHTS.remove(player);
            player.releaseUsingItem();
            InsightQuizMenu.open(player, target, definition);
            player.getCooldowns().addCooldown(this, 10);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            ACTIVE_INSIGHTS.remove(player);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ACTIVE_USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            ACTIVE_INSIGHTS.remove(player);
        }
    }

    private InteractionResult tryStartInsight(
            Player player,
            InteractionHand usedHand,
            LivingEntity target
    ) {
        if (player.level().isClientSide) {
            return InteractionResult.PASS;
        }

        EntityIntellectDefinition definition = EntityIntellectManager.get(target);
        if (definition == null) {
            return InteractionResult.PASS;
        }

        int playerIq = IntelligenceManager.getEffectiveIq(player);
        if (definition.intellect() - playerIq > 30) {
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new InsightFailurePayload(definition.intellect(), playerIq)
                );
                serverPlayer.playNotifySound(
                        SoundEvents.VILLAGER_NO,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
            player.getCooldowns().addCooldown(this, 20);
            return InteractionResult.FAIL;
        }

        if (!InsightQuestionManager.canCreateSession(definition.difficulty())) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.mathmaster.insight.no_questions"),
                        true
                );
            }
            return InteractionResult.FAIL;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ACTIVE_INSIGHTS.put(serverPlayer, InsightCharge.forEntity(target.getType()));
        }
        player.startUsingItem(usedHand);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    public InteractionResult tryStartSelfInsight(Player player, InteractionHand usedHand) {
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ACTIVE_INSIGHTS.put(serverPlayer, InsightCharge.forSelf());
        }
        player.startUsingItem(usedHand);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    private static void openSelfInsight(ServerPlayer player) {
        IntelligenceData intelligence = player.getData(ModAttachments.INTELLIGENCE);
        PacketDistributor.sendToPlayer(player, new SelfInsightPayload(
                player.getGameProfile().getName(),
                player.getUUID().toString(),
                IntelligenceManager.getEffectiveIq(player),
                intelligence.getExperience(),
                intelligence.getXpNeededForNextIq(),
                player.getData(ModAttachments.DIGITAL_POLLUTION).getValue()
        ));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        tooltipComponents.add(
                Component.translatable("tooltip.mathmaster.lingxu_mirror.insight")
                        .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
                Component.translatable("tooltip.mathmaster.lingxu_mirror.self_insight")
                        .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
                Component.translatable("tooltip.mathmaster.lingxu_mirror.verse")
                        .withStyle(ChatFormatting.GOLD)
        );
    }

    private static final class InsightCharge {
        private final boolean selfInsight;
        private EntityType<?> entityType;
        private int progressTicks;

        private InsightCharge(boolean selfInsight, EntityType<?> entityType) {
            this.selfInsight = selfInsight;
            this.entityType = entityType;
        }

        private static InsightCharge forEntity(EntityType<?> entityType) {
            return new InsightCharge(false, entityType);
        }

        private static InsightCharge forSelf() {
            return new InsightCharge(true, null);
        }

        private boolean isSelfInsight() {
            return selfInsight;
        }

        private void increase(EntityType<?> currentEntityType) {
            if (this.entityType != currentEntityType) {
                this.entityType = currentEntityType;
                this.progressTicks = 0;
            }
            this.progressTicks = Math.min(INSIGHT_DURATION_TICKS, this.progressTicks + 1);
        }

        private void decrease() {
            this.progressTicks = Math.max(0, this.progressTicks - 1);
        }

        private void increaseSelf() {
            this.progressTicks = Math.min(SELF_INSIGHT_DURATION_TICKS, this.progressTicks + 1);
        }

        private int progressTicks() {
            return this.progressTicks;
        }
    }
}
