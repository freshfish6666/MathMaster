package com.freshfish.mathmaster.entity;

import com.freshfish.mathmaster.init.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Diamond-armored Seven shares Eight's inherited movement and group retaliation AI. */
public final class SevenEntity extends NineEntity {
    /** Enclose every facing of the thin model without resizing during movement or knockback. */
    public static final float COLLISION_WIDTH = (float) Math.hypot(HITBOX_WIDTH, HITBOX_DEPTH);

    public SevenEntity(EntityType<? extends SevenEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected AABB makeBoundingBox() {
        // The inherited tick refresh must not expand a rotated box into a wall after collide().
        double radius = COLLISION_WIDTH * 0.5D;
        return new AABB(getX() - radius, getY(), getZ() - radius,
                getX() + radius, getY() + HITBOX_HEIGHT, getZ() + radius);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NineEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D);
    }

    @Override
    protected double getAttackDamageForDifficulty() {
        return super.getAttackDamageForDifficulty() * 2.0D;
    }

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.SEVEN_AMBIENT.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.SEVEN_HURT.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.SEVEN_DEATH.get(); }

    @Override
    protected void playAttackSound() { playSound(ModSounds.SEVEN_ATTACK.get(), 1.0F, getVoicePitch()); }

    @Override
    protected void playAlertSound() { playSound(ModSounds.SEVEN_ALERT.get(), 1.0F, getVoicePitch()); }
}
