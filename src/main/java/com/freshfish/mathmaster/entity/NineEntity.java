package com.freshfish.mathmaster.entity;

import com.freshfish.mathmaster.init.ModSounds;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** A normally passive creature that calls nearby nines to retaliate against players. */
public class NineEntity extends PathfinderMob {
    private static final double ALERT_RADIUS = 32.0D;
    /** Model outer width is 1 block; leave a small targeting/collision margin. */
    public static final float HITBOX_WIDTH = 1.04F;
    /** Model decorations reach about 0.264 blocks deep; leave a small margin on both faces. */
    public static final float HITBOX_DEPTH = 0.34F;
    public static final float HITBOX_HEIGHT = 2.0F;

    public NineEntity(EntityType<? extends NineEntity> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ARMOR, 7.0)
                .add(Attributes.ATTACK_DAMAGE, 4.5)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, ALERT_RADIUS);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new DifficultyAwareMeleeAttackGoal(this));
        goalSelector.addGoal(2, new FiveSecondStareGoal(this));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F, 0.06F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        // The model turns with the head, so keep its narrow rectangular box aligned to that facing.
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        double yaw = Math.toRadians(getYHeadRot());
        double absCos = Math.abs(Math.cos(yaw));
        double absSin = Math.abs(Math.sin(yaw));
        double xRadius = (HITBOX_WIDTH * absCos + HITBOX_DEPTH * absSin) * 0.5D;
        double zRadius = (HITBOX_WIDTH * absSin + HITBOX_DEPTH * absCos) * 0.5D;
        return new AABB(
                getX() - xRadius,
                getY(),
                getZ() - zRadius,
                getX() + xRadius,
                getY() + HITBOX_HEIGHT,
                getZ() + zRadius);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && level().getDifficulty() != Difficulty.PEACEFUL && source.getEntity() instanceof Player player) {
            alertNearbyNines(player);
        }
        return hurt;
    }

    private void alertNearbyNines(Player player) {
        double alertRadiusSqr = ALERT_RADIUS * ALERT_RADIUS;
        for (NineEntity nine : level().getEntitiesOfClass(
                NineEntity.class,
                getBoundingBox().inflate(ALERT_RADIUS),
                candidate -> candidate.isAlive() && candidate.distanceToSqr(this) <= alertRadiusSqr)) {
            if (nine.getTarget() != player) {
                nine.setTarget(player);
                nine.playAlertSound();
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.NINE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.NINE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.NINE_DEATH.get();
    }

    @Override
    protected void playAttackSound() {
        playSound(ModSounds.NINE_ATTACK.get(), 1.0F, getVoicePitch());
    }

    protected void playAlertSound() {
        playSound(ModSounds.NINE_ALERT.get(), 1.0F, getVoicePitch());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        double damage = getAttackDamageForDifficulty();
        if (damage <= 0.0D) {
            return false;
        }

        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof ServerPlayer player) {
            int digitalNumber = this instanceof SevenEntity ? 7 : this instanceof EightEntity ? 8 : 9;
            DigitalPollutionManager.tryAddFromAttack(player, digitalNumber);
        }
        return hurt;
    }

    protected double getAttackDamageForDifficulty() {
        return switch (level().getDifficulty()) {
            case PEACEFUL -> 0.0D;
            case EASY -> 3.0D;
            case NORMAL -> 4.5D;
            case HARD -> 6.0D;
        };
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        return false;
    }

    private static final class DifficultyAwareMeleeAttackGoal extends MeleeAttackGoal {
        private final NineEntity nine;

        private DifficultyAwareMeleeAttackGoal(NineEntity nine) {
            super(nine, 1.15, true);
            this.nine = nine;
        }

        @Override
        public boolean canUse() {
            return canAttackInCurrentDifficulty() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return canAttackInCurrentDifficulty() && super.canContinueToUse();
        }

        private boolean canAttackInCurrentDifficulty() {
            if (nine.level().getDifficulty() == Difficulty.PEACEFUL) {
                nine.setTarget(null);
                return false;
            }
            return true;
        }
    }

    private static final class FiveSecondStareGoal extends Goal {
        private static final int DURATION_TICKS = 5 * 20;
        private static final int START_CHANCE = 200;
        private static final double LOOK_DISTANCE = 12.0D;

        private final NineEntity nine;
        @Nullable
        private Player player;
        private int remainingTicks;

        private FiveSecondStareGoal(NineEntity nine) {
            this.nine = nine;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (nine.getTarget() != null || nine.getRandom().nextInt(START_CHANCE) != 0) {
                return false;
            }
            player = nine.level().getNearestPlayer(nine, LOOK_DISTANCE);
            return player != null;
        }

        @Override
        public boolean canContinueToUse() {
            return remainingTicks > 0
                    && nine.getTarget() == null
                    && player != null
                    && player.isAlive()
                    && !player.isSpectator()
                    && nine.distanceToSqr(player) <= LOOK_DISTANCE * LOOK_DISTANCE;
        }

        @Override
        public void start() {
            remainingTicks = DURATION_TICKS;
        }

        @Override
        public void stop() {
            player = null;
            remainingTicks = 0;
        }

        @Override
        public void tick() {
            if (player != null) {
                nine.getLookControl().setLookAt(player.getX(), player.getEyeY(), player.getZ());
            }
            remainingTicks--;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }
}
