package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class FlowEffectHandler {
    private static final int TOOL_EFFECT_DURATION_TICKS = 10;
    private static final int REFRESH_THRESHOLD_TICKS = 5;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        updateToolGrantedFlow(event.getEntity());
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity livingEntity
                && !(livingEntity instanceof Player)) {
            updateToolGrantedFlow(livingEntity);
        }
    }

    private static void updateToolGrantedFlow(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        int heldToolCount = (isLingxuTool(entity.getMainHandItem()) ? 1 : 0)
                + (isLingxuTool(entity.getOffhandItem()) ? 1 : 0);
        MobEffectInstance current = entity.getEffect(ModMobEffects.FLOW);

        if (heldToolCount == 0) {
            if (isToolGrantedEffect(current)) {
                entity.removeEffect(ModMobEffects.FLOW);
            }
            return;
        }

        int desiredAmplifier = heldToolCount - 1;
        if (current != null && isToolGrantedEffect(current)
                && current.getAmplifier() != desiredAmplifier) {
            entity.removeEffect(ModMobEffects.FLOW);
            current = null;
        }

        if (current == null
                || current.getAmplifier() < desiredAmplifier
                || isToolGrantedEffect(current) && current.getDuration() <= REFRESH_THRESHOLD_TICKS) {
            entity.addEffect(new MobEffectInstance(
                    ModMobEffects.FLOW,
                    TOOL_EFFECT_DURATION_TICKS,
                    desiredAmplifier,
                    true,
                    false,
                    true
            ));
        }
    }

    private static boolean isToolGrantedEffect(MobEffectInstance effect) {
        return effect != null
                && effect.isAmbient()
                && !effect.isVisible()
                && effect.showIcon()
                && effect.getDuration() <= TOOL_EFFECT_DURATION_TICKS;
    }

    private static boolean isLingxuTool(ItemStack stack) {
        return stack.is(ModItems.LINGXU_SWORD.get())
                || stack.is(ModItems.LINGXU_PICKAXE.get())
                || stack.is(ModItems.LINGXU_AXE.get())
                || stack.is(ModItems.LINGXU_SHOVEL.get())
                || stack.is(ModItems.LINGXU_HOE.get());
    }
}
