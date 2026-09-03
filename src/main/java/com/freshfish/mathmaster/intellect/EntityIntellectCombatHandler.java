package com.freshfish.mathmaster.intellect;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class EntityIntellectCombatHandler {
    private static final TagKey<DamageType> NO_INTELLECT_DAMAGE_BONUS = TagKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "no_intellect_damage_bonus")
    );

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_PLAYER_ATTACK)
                || event.getSource().is(NO_INTELLECT_DAMAGE_BONUS)
                || !(event.getSource().getEntity() instanceof Player player)
                || event.getSource().getDirectEntity() != player) {
            return;
        }

        EntityIntellectDefinition definition = EntityIntellectManager.get(event.getEntity());
        if (definition == null) {
            return;
        }

        int intellectLead = IntelligenceManager.getEffectiveIq(player) - definition.intellect();
        float bonusRate = MathMasterConfig.damageBonusRate(intellectLead);
        float currentDamage = event.getAmount();
        if (bonusRate <= 0.0F || currentDamage <= 0.0F || !Float.isFinite(currentDamage)) {
            return;
        }

        float modifiedDamage = currentDamage * (1.0F + bonusRate);
        if (Float.isFinite(modifiedDamage)) {
            event.setAmount(modifiedDamage);
        }
    }

}
