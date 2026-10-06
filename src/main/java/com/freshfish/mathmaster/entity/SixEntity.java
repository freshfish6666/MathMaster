package com.freshfish.mathmaster.entity;

import com.freshfish.mathmaster.init.ModSounds;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** A violent, failed flesh conversion that hunts players instead of joining the 8/9 herd. */
public final class SixEntity extends Monster {
    public static final float HITBOX_WIDTH = 1.45F;
    public static final float HITBOX_HEIGHT = 2.9F;

    public SixEntity(EntityType<? extends SixEntity> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.12D, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        double damage = switch (level().getDifficulty()) {
            case PEACEFUL -> 0.0D;
            case EASY -> 12.0D;
            case NORMAL -> 25.0D;
            case HARD -> 39.0D;
        };
        if (damage <= 0.0D) {
            return false;
        }
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        boolean hurt = super.doHurtTarget(target);
        if (target instanceof Player player) {
            // Apply the vanilla cooldown even when the blow is fully blocked.
            player.getCooldowns().addCooldown(Items.SHIELD, 100);
        }
        if (hurt && target instanceof ServerPlayer player) {
            DigitalPollutionManager.tryAddFromAttack(player, 6);
        }
        return hurt;
    }

    @Override
    public boolean canDisableShield() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SIX_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SIX_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SIX_DEATH.get();
    }

    @Override
    protected void playAttackSound() {
        playSound(ModSounds.SIX_ATTACK.get(), 1.15F, getVoicePitch());
    }

    @Override
    public float getVoicePitch() {
        return 0.76F + (random.nextFloat() - random.nextFloat()) * 0.035F;
    }
}
