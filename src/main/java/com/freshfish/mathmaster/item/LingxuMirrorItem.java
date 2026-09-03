package com.freshfish.mathmaster.item;

import com.freshfish.mathmaster.intellect.EntityIntellectDefinition;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.intellect.InsightQuestionManager;
import com.freshfish.mathmaster.intellect.InsightTargeting;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.menu.InsightQuizMenu;
import com.freshfish.mathmaster.network.InsightFailurePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class LingxuMirrorItem extends Item {
    public static final int INSIGHT_DURATION_TICKS = 60;

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
            return InteractionResultHolder.pass(stack);
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
            livingEntity.releaseUsingItem();
            return;
        }

        LivingEntity target = InsightTargeting.findTarget(player);
        if (target == null) {
            player.releaseUsingItem();
            return;
        }

        EntityIntellectDefinition definition = EntityIntellectManager.get(target);
        if (definition == null
                || definition.intellect() - IntelligenceManager.getEffectiveIq(player) > 30) {
            player.releaseUsingItem();
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            LivingEntity target = InsightTargeting.findTarget(player);
            if (target != null) {
                EntityIntellectDefinition definition = EntityIntellectManager.get(target);
                if (definition != null
                        && definition.intellect() - IntelligenceManager.getEffectiveIq(player) <= 30) {
                    InsightQuizMenu.open(player, target, definition);
                    player.getCooldowns().addCooldown(this, 10);
                }
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return INSIGHT_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
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

        player.startUsingItem(usedHand);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
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
                Component.translatable("tooltip.mathmaster.lingxu_mirror.verse")
                        .withStyle(ChatFormatting.GOLD)
        );
    }
}
