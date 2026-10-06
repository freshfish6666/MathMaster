package com.freshfish.mathmaster.axiom;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Independent attack/defense rolls on direct combat damage, before vanilla mitigation. */
public final class ShannonEntropySkill {
    private static final int[] ATTACK_HALF = {24, 32, 40, 46, 50};
    private static final int[] ATTACK_DOUBLE = {16, 24, 32, 39, 45};
    private static final int[] DEFENSE_HALF = {24, 36, 48, 56, 64};

    public static int attackHalfChance(int level) { return ATTACK_HALF[index(level)]; }
    public static int attackDoubleChance(int level) { return ATTACK_DOUBLE[index(level)]; }
    public static int attackNormalChance(int level) { return 100 - attackHalfChance(level) - attackDoubleChance(level); }
    public static int defenseHalfChance(int level) { return DEFENSE_HALF[index(level)]; }
    public static int defenseDoubleChance(int level) { return defenseHalfChance(level) / 2; }
    public static int defenseNormalChance(int level) { return 100 - defenseHalfChance(level) - defenseDoubleChance(level); }

    private static int index(int level) { return Math.max(1, Math.min(5, level)) - 1; }

    static float multiplier(int level, boolean attack, int roll) {
        int half = attack ? attackHalfChance(level) : defenseHalfChance(level);
        int normal = attack ? attackNormalChance(level) : defenseNormalChance(level);
        return roll < half ? 0.5F : roll < half + normal ? 1.0F : 2.0F;
    }

    static boolean isDirectDamage(DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return source.getDirectEntity() instanceof Projectile && !source.is(DamageTypes.INDIRECT_MAGIC);
        }
        return source.getDirectEntity() == source.getEntity()
                && (source.is(DamageTypeTags.IS_PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || source.is(DamageTypes.STING));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        float amount = event.getAmount();
        if (event.isCanceled() || event.getEntity().level().isClientSide()
                || amount <= 0.0F || !Float.isFinite(amount) || !isDirectDamage(event.getSource())) {
            return;
        }
        LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
        LivingEntity target = event.getEntity();
        int attackLevel = AxiomEffectManager.getEquippedLevel(attacker, AxiomDefinition.SHANNON_ENTROPY);
        int defenseLevel = AxiomEffectManager.getEquippedLevel(target, AxiomDefinition.SHANNON_ENTROPY);
        if (attackLevel <= 0 && defenseLevel <= 0) return;
        float factor = 1.0F;
        if (attackLevel > 0) factor *= multiplier(attackLevel, true, attacker.getRandom().nextInt(100));
        if (defenseLevel > 0) factor *= multiplier(defenseLevel, false, target.getRandom().nextInt(100));
        float modified = amount * factor;
        if (Float.isFinite(modified)) event.setAmount(modified);
    }
}
