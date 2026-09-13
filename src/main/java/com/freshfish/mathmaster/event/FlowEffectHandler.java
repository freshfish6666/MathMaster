package com.freshfish.mathmaster.event;

import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class FlowEffectHandler {
    private static final int GRANTED_EFFECT_DURATION_TICKS = 10;
    private static final int REFRESH_THRESHOLD_TICKS = 5;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        updateEquipmentGrantedFlow(player);
        updateFlowGrantedMentalRampage(player);
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity livingEntity
                && !(livingEntity instanceof Player)) {
            updateEquipmentGrantedFlow(livingEntity);
        }
    }

    private static void updateEquipmentGrantedFlow(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        int heldToolCount = (isLingxuTool(entity.getMainHandItem()) ? 1 : 0)
                + (isLingxuTool(entity.getOffhandItem()) ? 1 : 0);
        int armorCount = entity instanceof Player ? countLingxuArmor(entity) : 0;
        int grantedLevel = heldToolCount + armorCount + (armorCount == 4 ? 1 : 0);
        MobEffectInstance current = entity.getEffect(ModMobEffects.FLOW);

        if (grantedLevel == 0) {
            if (isShortGrantedEffect(current)) {
                entity.removeEffect(ModMobEffects.FLOW);
            }
            return;
        }

        int desiredAmplifier = grantedLevel - 1;
        if (current != null && isShortGrantedEffect(current)
                && current.getAmplifier() != desiredAmplifier) {
            entity.removeEffect(ModMobEffects.FLOW);
            current = null;
        }

        if (current == null
                || current.getAmplifier() < desiredAmplifier
                || isShortGrantedEffect(current) && current.getDuration() <= REFRESH_THRESHOLD_TICKS) {
            entity.addEffect(new MobEffectInstance(
                    ModMobEffects.FLOW,
                    GRANTED_EFFECT_DURATION_TICKS,
                    desiredAmplifier,
                    true,
                    false,
                    true
            ));
        }
    }

    private static void updateFlowGrantedMentalRampage(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        MobEffectInstance flow = player.getEffect(ModMobEffects.FLOW);
        int flowLevel = flow == null ? 0 : flow.getAmplifier() + 1;
        int desiredLevel = flowLevel / 5;
        MobEffectInstance current = player.getEffect(ModMobEffects.MENTAL_RAMPAGE);

        if (desiredLevel == 0) {
            if (isShortGrantedEffect(current)) {
                player.removeEffect(ModMobEffects.MENTAL_RAMPAGE);
            }
            return;
        }

        int desiredAmplifier = desiredLevel - 1;
        if (current != null && isShortGrantedEffect(current)
                && current.getAmplifier() != desiredAmplifier) {
            player.removeEffect(ModMobEffects.MENTAL_RAMPAGE);
            current = null;
        }

        if (current == null
                || current.getAmplifier() < desiredAmplifier
                || isShortGrantedEffect(current) && current.getDuration() <= REFRESH_THRESHOLD_TICKS) {
            player.addEffect(new MobEffectInstance(
                    ModMobEffects.MENTAL_RAMPAGE,
                    GRANTED_EFFECT_DURATION_TICKS,
                    desiredAmplifier,
                    true,
                    false,
                    true
            ));
        }
    }

    private static boolean isShortGrantedEffect(MobEffectInstance effect) {
        return effect != null
                && effect.isAmbient()
                && !effect.isVisible()
                && effect.showIcon()
                && effect.getDuration() <= GRANTED_EFFECT_DURATION_TICKS;
    }

    private static int countLingxuArmor(LivingEntity entity) {
        int count = 0;
        count += entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LINGXU_HELMET.get()) ? 1 : 0;
        count += entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.LINGXU_CHESTPLATE.get()) ? 1 : 0;
        count += entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.LINGXU_LEGGINGS.get()) ? 1 : 0;
        count += entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.LINGXU_BOOTS.get()) ? 1 : 0;
        return count;
    }

    private static boolean isLingxuTool(ItemStack stack) {
        return stack.is(ModItems.LINGXU_SWORD.get())
                || stack.is(ModItems.LINGXU_PICKAXE.get())
                || stack.is(ModItems.LINGXU_AXE.get())
                || stack.is(ModItems.LINGXU_SHOVEL.get())
                || stack.is(ModItems.LINGXU_HOE.get());
    }
}
