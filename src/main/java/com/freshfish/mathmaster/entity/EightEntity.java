package com.freshfish.mathmaster.entity;

import com.freshfish.mathmaster.init.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** The iron-armored sibling of Nine; AI and retaliation stay consistent. */
public final class EightEntity extends NineEntity {
    public EightEntity(EntityType<? extends EightEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected double getAttackDamageForDifficulty() {
        return switch (level().getDifficulty()) {
            case PEACEFUL -> 0.0D;
            case EASY -> 4.0D;
            case NORMAL -> 6.0D;
            case HARD -> 8.0D;
        };
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.EIGHT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.EIGHT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.EIGHT_DEATH.get();
    }

    @Override
    protected void playAttackSound() {
        playSound(ModSounds.EIGHT_ATTACK.get(), 1.0F, getVoicePitch());
    }

    @Override
    protected void playAlertSound() {
        playSound(ModSounds.EIGHT_ALERT.get(), 1.0F, getVoicePitch());
    }
}
