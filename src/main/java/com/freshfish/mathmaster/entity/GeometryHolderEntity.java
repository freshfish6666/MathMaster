package com.freshfish.mathmaster.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Floating movement and a player-attributed single hit box. Close-range skills use the cube as a visual casting focus. */
public final class GeometryHolderEntity extends PathfinderMob {
    public static final float WIDTH = 1.75F;
    public static final float HEIGHT = 3.625F;
    public static final double ACQUIRE_RANGE = 32;
    public static final double CHASE_RANGE = 16;
    public static final double LOSE_RANGE = 48;
    public static final int IDLE = 0, HALO = 1, PRISON = 2, RECOVERY = 3, PRISON_ACTIVE = 4;
    public static final int PRISON_CHARGE_TICKS = 40, PRISON_DAMAGE_TICKS = 30, PRISON_STUN_TICKS = 30;
    public static final double PRISON_SIZE = 7;
    // Normal-difficulty damage before vanilla armor and other mitigation, identical in both phases.
    public static final float HALO_DAMAGE = 12, PRISON_DAMAGE = 6;
    private boolean clientDeathConfirmed;
    public boolean clientDeathConfirmed() { return clientDeathConfirmed; }
    @Override public void handleEntityEvent(byte event) {
        if (event==3 && level().isClientSide) clientDeathConfirmed=true;
        super.handleEntityEvent(event);
    }
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_TICKS = SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<org.joml.Vector3f> RANGED_DIRECTION=SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.VECTOR3);
    static final EntityDataAccessor<org.joml.Vector3f> RANGED_BOMB_OFFSET=SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.VECTOR3);
    static final EntityDataAccessor<org.joml.Vector3f> RANGED_LENGTHS=SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.VECTOR3);
    static final EntityDataAccessor<Float> RANGED_FOURTH_LENGTH=SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SECOND_PHASE = SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<org.joml.Vector3f> RANGED_BOMB_OFFSET_2=SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.VECTOR3);
    static final EntityDataAccessor<org.joml.Vector3f> RANGED_BOMB_OFFSET_3=SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.VECTOR3);
    static final EntityDataAccessor<Integer> RANGED_BOMB_MASK=SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.INT);
    static final java.util.List<EntityDataAccessor<org.joml.Vector3f>> ULTIMATE_OFFSETS =
            java.util.stream.IntStream.range(0, 12).mapToObj(i -> SynchedEntityData.defineId(
                    GeometryHolderEntity.class, EntityDataSerializers.VECTOR3)).toList();
    static final EntityDataAccessor<Integer> ULTIMATE_MASK = SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.INT);
    // Keep the existing twelve offsets and mask IDs; append the eight additional positions.
    static final java.util.List<EntityDataAccessor<org.joml.Vector3f>> ULTIMATE_EXTRA_OFFSETS =
            java.util.stream.IntStream.range(0, 8).mapToObj(i -> SynchedEntityData.defineId(
                    GeometryHolderEntity.class, EntityDataSerializers.VECTOR3)).toList();
    // Append new channel data after all existing IDs; old registrations/NBT/custom payloads stay intact.
    static final EntityDataAccessor<Integer> RANGED_ATTACK = SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> RANGED_TICKS = SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<org.joml.Vector3f> PRISON_OFFSET = SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.VECTOR3);
    static final EntityDataAccessor<Integer> ULTIMATE_TICKS = SynchedEntityData.defineId(GeometryHolderEntity.class, EntityDataSerializers.INT);
    private int rangedCooldown = 20, lastCastStart = -1000, rangedSoundStartedTick = -1;
    private boolean prisonRecovering;
    private int soundStartedTick = -1;
    private int attackCooldown = 20;
    private final GeometryHolderSkillPool nearSkills = new GeometryHolderSkillPool(HALO, PRISON);
    private final GeometryHolderRangedAttacks ranged = new GeometryHolderRangedAttacks(this);
    private final GeometryHolderUltimate ultimate = new GeometryHolderUltimate(this);
    private final net.minecraft.server.level.ServerBossEvent bossBar = new net.minecraft.server.level.ServerBossEvent(
            getDisplayName(), net.minecraft.world.BossEvent.BossBarColor.BLUE, net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS) {
        @Override public java.util.UUID getId() { return GeometryHolderEntity.this.getUUID(); }
    };
    private final java.util.Set<net.minecraft.server.level.ServerPlayer> barTrackers = new java.util.HashSet<>();
    public GeometryHolderRangedAttacks rangedAttacks() { return ranged; }
    public GeometryHolderUltimate ultimateAttack() { return ultimate; }
    public int skillCooldownTicks() { return isSecondPhase() ? 32 : 40; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK, IDLE);
        builder.define(ATTACK_TICKS, 0);
        GeometryHolderRangedAttacks.define(builder);
        builder.define(SECOND_PHASE, false);
        GeometryHolderUltimate.define(builder);
        builder.define(RANGED_ATTACK, IDLE);
        builder.define(RANGED_TICKS, 0);
        builder.define(PRISON_OFFSET, new org.joml.Vector3f());
        builder.define(ULTIMATE_TICKS, 0);
    }

    public boolean isSecondPhase() { return entityData.get(SECOND_PHASE) || getHealth() < getMaxHealth() * 0.4F; }
    public double prisonSize() { return isSecondPhase() ? 8 : PRISON_SIZE; }
    public double haloRadius() { return isSecondPhase() ? 4 : 3; }
    public int bodyTint() { return isSecondPhase() ? 0xFF78242C : 0xFFFFFFFF; }
    public int glowTint() { return isSecondPhase() ? 0xFFFF2030 : 0xFFFFFFFF; }
    public static final int PHASE_FADE_TICKS = 40;
    private int phaseVisualTicks, previousPhaseVisualTicks;
    private boolean phaseVisualInitialized;
    public float phaseBlend(float partial) {
        if (!phaseVisualInitialized) return isSecondPhase() ? 1 : 0;
        float t = Mth.lerp(Mth.clamp(partial, 0, 1), previousPhaseVisualTicks, phaseVisualTicks) / PHASE_FADE_TICKS;
        return t * t * (3 - 2 * t);
    }
    public int bodyTint(float partial) {
        return net.minecraft.util.FastColor.ARGB32.lerp(phaseBlend(partial), 0xFFFFFFFF, 0xFF78242C);
    }
    public int glowTint(float partial) {
        return net.minecraft.util.FastColor.ARGB32.lerp(phaseBlend(partial), 0xFFFFFFFF, 0xFFFF2030);
    }
    public int coreChargeTint(float partial) {
        return net.minecraft.util.FastColor.ARGB32.lerp(phaseBlend(partial), 0xFFA0EFFF, 0xFFFF2030);
    }
    private void advancePhaseVisuals() {
        previousPhaseVisualTicks = phaseVisualTicks;
        if (!phaseVisualInitialized) {
            // Newly tracked/reloaded second-stage bosses start at their established appearance.
            phaseVisualTicks = previousPhaseVisualTicks = isSecondPhase() ? PHASE_FADE_TICKS : 0;
            phaseVisualInitialized = true;
        } else if (isSecondPhase()) phaseVisualTicks = Math.min(PHASE_FADE_TICKS, phaseVisualTicks + 1);
    }

    public int nearPhase() { return entityData.get(ATTACK); }
    public int nearTicks() { return entityData.get(ATTACK_TICKS); }
    // Legacy aggregate view for single-channel observers. Rendering/audio use explicit channels.
    public int attackPhase() { return nearPhase()!=IDLE ? nearPhase() : ranged.phase()!=IDLE ? ranged.phase() : ultimate.active() ? GeometryHolderUltimate.ACTIVE : IDLE; }
    public int attackTicks() { return nearPhase()!=IDLE ? nearTicks() : ranged.phase()!=IDLE ? ranged.ticks() : ultimate.ticks(); }
    public float prisonExpansion(float partialTick) {
        return nearPhase() == PRISON_ACTIVE ? 1 : nearPhase() == PRISON
                ? Mth.clamp((PRISON_CHARGE_TICKS - nearTicks() + partialTick) / 10F, 0, 1) : 0;
    }
    public Vec3 castingCenter() {
        double yaw = getYRot() * Mth.DEG_TO_RAD;
        return position().add(-Math.sin(yaw) * 13 / 16, 2, Math.cos(yaw) * 13 / 16);
    }
    public Vec3 prisonCenter() {
        if(nearPhase()!=PRISON && nearPhase()!=PRISON_ACTIVE) return castingCenter();
        var offset=entityData.get(PRISON_OFFSET);
        return position().add(offset.x,offset.y,offset.z);
    }
    public AABB prisonBounds() { return AABB.ofSize(prisonCenter(), prisonSize(), prisonSize(), prisonSize()); }
    @Override public AABB getBoundingBoxForCulling() {
        AABB bounds = nearPhase() == PRISON || nearPhase() == PRISON_ACTIVE
                ? getBoundingBox().minmax(prisonBounds()) : getBoundingBox();
        if (ranged == null) return bounds;
        if (ultimate != null) for (int i=0;i<GeometryHolderUltimate.MAX_CUBES;i++) if (ultimate.cubeActive(i))
            bounds = bounds.minmax(ultimate.cubeBounds(i));
        for (int i=0;i<3;i++) if (ranged.bombActive(i))
            bounds = bounds.minmax(AABB.ofSize(ranged.bombPosition(i,1), 1, 1, 1));
        for (int i=0;i<ranged.rayCount();i++) bounds=bounds.minmax(new AABB(castingCenter(),
                castingCenter().add(ranged.rayDirection(i,1).scale(ranged.rayLength(i)))).inflate(Math.max(.5, ranged.rayRadius()*Math.sqrt(2))));
        return bounds;
    }

    private Vec3 home;
    private Vec3 loadedPosition;
    private Vec3 destination;
    private int destinationTicks;

    public GeometryHolderEntity(EntityType<? extends GeometryHolderEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.ARMOR, 20)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return attackPhase()==IDLE ? com.freshfish.mathmaster.init.ModSounds.GEOMETRY_AMBIENT.get() : null;
    }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) { return com.freshfish.mathmaster.init.ModSounds.GEOMETRY_HURT.get(); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.freshfish.mathmaster.init.ModSounds.GEOMETRY_DEATH.get(); }
    @Override public net.minecraft.sounds.SoundSource getSoundSource() { return net.minecraft.sounds.SoundSource.HOSTILE; }
    @Override public int getAmbientSoundInterval() { return 160; }
    @Override protected float getSoundVolume() { return .55F * GeometryHolderAudio.VOLUME_GAIN; }
    @Override public float getVoicePitch() { return isSecondPhase() ? .9F : 1F; }
    @Override protected void registerGoals() {}
    @Override public boolean isPushable() { return false; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override public void tick() {
        if (level().isClientSide()) ranged.clientTick();
        super.tick();
        advancePhaseVisuals();
        if (!level().isClientSide()) updateBossBar();
        if (!level().isClientSide() && isAlive()) {
            if(soundStartedTick!=tickCount && nearTicks()%10==0) GeometryHolderAudio.pulse(this,nearPhase());
            if(rangedSoundStartedTick!=tickCount && ranged.ticks()%10==0) GeometryHolderAudio.pulse(this,ranged.phase());
        }
        if (!level().isClientSide() && !isAlive()) { ultimate.clear(); ranged.clearBombs(); setAttack(IDLE, 0); }
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void updateBossBar() {
        bossBar.setName(getDisplayName());
        bossBar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0, 1));
        bossBar.setColor(isSecondPhase() ? net.minecraft.world.BossEvent.BossBarColor.RED : net.minecraft.world.BossEvent.BossBarColor.BLUE);
        bossBar.setVisible(isAlive() && !isRemoved());
        for (var viewer : barTrackers) updateBossBarViewer(viewer);
    }
    private void updateBossBarViewer(net.minecraft.server.level.ServerPlayer viewer) {
        if (isAlive() && !isRemoved() && viewer.isAlive() && viewer.level() == level()
                && distanceToSqr(viewer) <= LOSE_RANGE * LOSE_RANGE) bossBar.addPlayer(viewer);
        else bossBar.removePlayer(viewer);
    }
    @Override public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        barTrackers.add(player);
        updateBossBar();
    }
    @Override public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        barTrackers.remove(player);
        bossBar.removePlayer(player);
    }
    @Override public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        if (bossBar != null) bossBar.removeAllPlayers();
        if (barTrackers != null) barTrackers.clear();
        super.remove(reason);
    }

    @Override protected void customServerAiStep() {
        if (!entityData.get(SECOND_PHASE) && isSecondPhase()) {
            entityData.set(SECOND_PHASE, true);
            attackCooldown = Math.round(attackCooldown * .8F);
            rangedCooldown = Math.round(rangedCooldown * .8F);
            GeometryHolderAudio.play(this, com.freshfish.mathmaster.init.ModSounds.GEOMETRY_PHASE_TWO.get(), 1F, position());
        }
        if (home != null && loadedPosition != null) home = home.add(position().subtract(loadedPosition));
        loadedPosition = null;
        if (home == null) home = position();
        var target = getTarget();
        if (!(target instanceof Player player) || !eligible(player)
                || distanceToSqr(player) > LOSE_RANGE * LOSE_RANGE) {
            setTarget(null);
            target = null;
        }
        if (target == null && tickCount % 10 == 0) {
            target = level().players().stream().filter(this::eligible)
                    .filter(player -> distanceToSqr(player) <= ACQUIRE_RANGE * ACQUIRE_RANGE && hasLineOfSight(player))
                    .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            setTarget(target);
            if (target != null) GeometryHolderAudio.play(this, com.freshfish.mathmaster.init.ModSounds.GEOMETRY_ALERT.get(), .7F, position());
        }
        ultimate.tickCooldown(target != null);
        if (advanceAttack(target)) return;
        if (target != null) {
            double dx = target.getX() - getX(), dz = target.getZ() - getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 18));
            destination = target.position();
            destinationTicks = 0;
        } else if (destinationTicks <= 0 || destination == null || position().distanceToSqr(destination) < 1) {
            pickDestination(home, 16, true);
        }
        destinationTicks--;
        Vec3 desired = Vec3.ZERO;
        if (destination != null && position().distanceToSqr(destination) > 0.25) {
            double speed = target != null && distanceToSqr(target) > CHASE_RANGE * CHASE_RANGE ? 0.14 : 0.065;
            Vec3 offset = destination.subtract(position());
            // Ease into waypoints instead of abruptly stopping at full flight speed.
            desired = offset.normalize().scale(Math.min(speed, offset.length() * 0.12));
        }
        if (target != null) {
            // Combat height follows the player's feet, independently of horizontal waypoint arrival.
            // The core is two blocks above our feet, so random upward drift makes horizontal waves miss.
            double speed = distanceToSqr(target) > CHASE_RANGE * CHASE_RANGE ? 0.14 : 0.065;
            desired = new Vec3(desired.x, Mth.clamp((target.getY() - getY()) * 0.12, -speed, speed), desired.z);
            if (desired.lengthSqr() > speed * speed) desired = desired.normalize().scale(speed);
        }
        Vec3 velocity = getDeltaMovement().lerp(desired, 0.2);
        if (!level().noCollision(this, getBoundingBox().expandTowards(velocity))) {
            Vec3 attempted = velocity;
            velocity = Vec3.ZERO;
            for (Vec3 slide : new Vec3[]{new Vec3(attempted.x, attempted.y, 0),
                    new Vec3(0, attempted.y, attempted.z), new Vec3(0, attempted.y, 0)}) {
                if (slide.lengthSqr() > 1e-6 && level().noCollision(this, getBoundingBox().expandTowards(slide))) {
                    velocity = slide;
                    break;
                }
            }
            destinationTicks = 0;
        }
        setDeltaMovement(velocity);
        if (target == null && velocity.horizontalDistanceSqr() > 0.001) {
            float yaw = (float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 8));
        }
    }

    private boolean advanceAttack(LivingEntity target) {
        ultimate.advance(target);
        if(target!=null && ultimate.ready()) ultimate.start(target);
        boolean nearReady=attackCooldown==0, farReady=rangedCooldown==0;
        boolean nearBusy=advanceNear(target);
        boolean farBusy=ranged.advance(target);
        if(!nearBusy && nearPhase()==IDLE && attackCooldown>0) attackCooldown--;
        if(!farBusy && ranged.phase()==IDLE && rangedCooldown>0) rangedCooldown--;
        boolean stunned=nearPhase()==RECOVERY && prisonRecovering;
        boolean mayStart=target!=null && !stunned && readyAtCombatHeight(target)
                && hasLineOfSight(target) && tickCount-lastCastStart>=16;
        if(mayStart && nearPhase()==IDLE && nearReady && getBoundingBox().distanceToSqr(target.position())<=16) {
            int selected=nearSkills.pick(random);
            if(!ranged.active()) {
                double dx=target.getX()-getX(),dz=target.getZ()-getZ();
                setYRot((float)(Mth.atan2(dz,dx)*Mth.RAD_TO_DEG)-90);
            }
            setNearAttack(selected,selected==PRISON ? PRISON_CHARGE_TICKS : 16);
            lastCastStart=tickCount;nearBusy=true;
        } else if(mayStart && ranged.phase()==IDLE && farReady) {
            ranged.start(target);lastCastStart=tickCount;farBusy=true;
        }
        boolean casting=(nearPhase()!=IDLE && nearPhase()!=RECOVERY) || stunned || ranged.active() || ultimate.active();
        if(casting) {destination=null;destinationTicks=0;setDeltaMovement(Vec3.ZERO);}
        return casting;
    }

    private boolean readyAtCombatHeight(LivingEntity target) {
        double difference=target.getY()-getY();
        if(Math.abs(difference)<=.1) return true;
        // Prefer matching the player's feet, but terrain must not make alignment a permanent cast lock.
        double speed=distanceToSqr(target)>CHASE_RANGE*CHASE_RANGE ? .14 : .065;
        double vertical=Mth.clamp(difference*.12,-speed,speed);
        return !level().noCollision(this,getBoundingBox().expandTowards(0,vertical,0));
    }

    private boolean advanceNear(LivingEntity target) {
        int phase=nearPhase();
        if(phase==IDLE) return false;
        if(phase!=RECOVERY || prisonRecovering) setDeltaMovement(Vec3.ZERO);
        if(phase==RECOVERY) {
            if(nearTicks()<=1) {setNearAttack(IDLE,0);prisonRecovering=false;}
            else entityData.set(ATTACK_TICKS,nearTicks()-1);
        } else if(phase==PRISON_ACTIVE) {
            int remaining=nearTicks()-1;
            if(remaining>0) {
                entityData.set(ATTACK_TICKS,remaining);damagePrison();
                if(remaining%2==0 && level() instanceof ServerLevel server)
                    cubeParticles(server,prisonCenter(),prisonSize()/2,10,true);
            } else {
                prisonRecovering=true;setNearAttack(RECOVERY,PRISON_STUN_TICKS);attackCooldown=skillCooldownTicks();
            }
        } else if(target==null) {
            setNearAttack(IDLE,0);attackCooldown=skillCooldownTicks();
        } else {
            prisonRecovering=false;
            int remaining=nearTicks()-1;entityData.set(ATTACK_TICKS,remaining);
            if(remaining>0) chargeParticles(phase,remaining);
            else if(phase==PRISON) {
                setNearAttack(PRISON_ACTIVE,PRISON_DAMAGE_TICKS);damagePrison();
                if(level() instanceof ServerLevel server) cubeParticles(server,prisonCenter(),prisonSize()/2,48,true);
            } else {
                releaseAttack(phase);setNearAttack(RECOVERY,10);attackCooldown=skillCooldownTicks();
            }
        }
        return true;
    }

    void finishRanged() {ranged.clearBombs();setRangedAttack(RECOVERY,10);rangedCooldown=skillCooldownTicks();}
    void finishUltimate() {ultimate.clear();}

    // Single-channel setup retained for existing controlled regression fixtures and transient reset.
    void setAttack(int phase,int ticks) {
        prisonRecovering=false;
        if(phase==GeometryHolderUltimate.ACTIVE) {entityData.set(ULTIMATE_TICKS,ticks);return;}
        ultimate.clear();
        if(phase>=GeometryHolderRangedAttacks.BEAM_CHARGE && phase<=GeometryHolderRangedAttacks.CROSS_ACTIVE) {
            setNearAttack(IDLE,0);setRangedAttack(phase,ticks);
        } else {
            setRangedAttack(IDLE,0);ranged.clearBombs();setNearAttack(phase,ticks);
        }
    }
    private void setNearAttack(int phase,int ticks) {
        if(nearPhase()!=phase) {
            if(phase==PRISON || phase==PRISON_ACTIVE && nearPhase()!=PRISON) {
                var offset=castingCenter().subtract(position());
                entityData.set(PRISON_OFFSET,new org.joml.Vector3f((float)offset.x,(float)offset.y,(float)offset.z));
            }
            soundStartedTick=tickCount;GeometryHolderAudio.begin(this,phase);
        }
        entityData.set(ATTACK,phase);entityData.set(ATTACK_TICKS,ticks);
    }
    void setRangedAttack(int phase,int ticks) {
        if(ranged.phase()!=phase) {rangedSoundStartedTick=tickCount;GeometryHolderAudio.begin(this,phase);}
        entityData.set(RANGED_ATTACK,phase);entityData.set(RANGED_TICKS,ticks);
    }

    private void chargeParticles(int phase, int remaining) {
        if (!(level() instanceof ServerLevel server) || remaining % 2 != 0) return;
        if (phase == PRISON) {
            Vec3 center = prisonCenter();
            int elapsed = PRISON_CHARGE_TICKS - remaining;
            double spread = prisonSize() / 2 * prisonExpansion(0);
            cubeParticles(server, center, spread, 2 + elapsed / 3, false);
            if (remaining <= 15) cubeParticles(server, center, prisonSize() / 2, 2 + elapsed / 10, true);
        } else {
            double yaw = getYRot() * Mth.DEG_TO_RAD;
            Vec3 halo = position().add(Math.sin(yaw) * 7 / 16, 46.0 / 16, -Math.cos(yaw) * 7 / 16);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, halo.x, halo.y, halo.z, 8, .7, .7, .1, .01);
        }
    }

    private void cubeParticles(ServerLevel server, Vec3 center, double half, int count, boolean blades) {
        for (int i=0;i<count;i++) server.sendParticles(blades ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.END_ROD,
                center.x + (random.nextDouble()*2-1)*half,
                center.y + (random.nextDouble()*2-1)*half,
                center.z + (random.nextDouble()*2-1)*half, 1, 0, 0, 0, blades ? 0 : .015);
    }

    private boolean attackable(LivingEntity victim) {
        return victim != this && victim.isAlive() && !victim.isSpectator()
                && (!(victim instanceof Player player) || !player.isCreative());
    }

    private void damagePrison() {
        // Keep normal hurt cooldowns; never clear a player's invulnerability timer.
        for (Player player : level().getEntitiesOfClass(Player.class, prisonBounds(), this::eligible))
            player.hurt(damageSources().mobAttack(this), PRISON_DAMAGE);
    }

    private void releaseAttack(int phase) {
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 center = getBoundingBox().getCenter();
        GeometryHolderAudio.play(this, com.freshfish.mathmaster.init.ModSounds.GEOMETRY_HALO_RELEASE.get(), .8F, center);
        double radius = haloRadius();
        AABB area = AABB.ofSize(center, radius * 2, radius * 2, radius * 2);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, area, this::attackable)) {
            if (victim.getBoundingBox().distanceToSqr(center) > radius * radius) continue;
            if (victim.hurt(damageSources().mobAttack(this), HALO_DAMAGE)) {
                double dx = victim.getX() - center.x, dz = victim.getZ() - center.z;
                if (dx*dx + dz*dz < 1e-6) { dx = Math.sin(getYRot()*Mth.DEG_TO_RAD); dz = -Math.cos(getYRot()*Mth.DEG_TO_RAD); }
                victim.knockback(.8, -dx, -dz);
                victim.hurtMarked = true;
            }
        }
        for (int i=0;i<48;i++) {
            double angle = i*Math.PI*2/48;
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x+Math.cos(angle)*radius, center.y, center.z+Math.sin(angle)*radius,
                    1, 0, .15, 0, .08);
        }
    }

    boolean eligible(Player player) {
        return player.level() == level() && player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private void pickDestination(Vec3 center, double radius, boolean varyHeight) {
        destination = null;
        destinationTicks = 60 + random.nextInt(60);
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            Vec3 candidate = center.add(Math.cos(angle)*distance, varyHeight ? random.nextDouble()*3-1.5 : 0, Math.sin(angle)*distance);
            if (varyHeight && Math.abs(candidate.y - home.y) > 4) continue;
            if (!level().hasChunkAt(net.minecraft.core.BlockPos.containing(candidate))) continue;
            if (level().noCollision(this, getBoundingBox().move(candidate.subtract(position())))) {
                destination = candidate;
                return;
            }
        }
    }

    @Override public void travel(Vec3 input) {
        if (isEffectiveAi()) {
            move(MoverType.SELF, getDeltaMovement());
            // Acceleration/deceleration is handled once by customServerAiStep.
            // Extra travel friction used to fight that smoothing every tick.
        }
    }

    @Override protected float tickHeadTurn(float rotation, float step) {
        yBodyRot = getYRot();
        return step;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("GeometryHolderSecondPhase", isSecondPhase());
        if (home != null) {
            CompoundTag anchor = new CompoundTag();
            anchor.putDouble("x", home.x); anchor.putDouble("y", home.y); anchor.putDouble("z", home.z);
            tag.put("GeometryHolderHome", anchor);
        }
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SECOND_PHASE, tag.getBoolean("GeometryHolderSecondPhase") || getHealth() < getMaxHealth() * 0.4F);
        phaseVisualInitialized = false;
        phaseVisualTicks = previousPhaseVisualTicks = 0;
        setAttack(IDLE, 0);
        attackCooldown = 20;
        rangedCooldown = 20; lastCastStart=-1000;prisonRecovering=false;
        nearSkills.reset();
        ranged.reset();
        ultimate.reset();
        home = null;
        if (tag.contains("GeometryHolderHome", 10)) {
            var anchor = tag.getCompound("GeometryHolderHome");
            Vec3 saved = new Vec3(anchor.getDouble("x"), anchor.getDouble("y"), anchor.getDouble("z"));
            if (Double.isFinite(saved.x) && Double.isFinite(saved.y) && Double.isFinite(saved.z)) home = saved;
        }
        // Structure placement can relocate an entity after loading its saved position.
        loadedPosition = position();
        // Migrate the earlier NoAI preview without changing other entities' save data.
        setNoAi(false);
        setNoGravity(true);
        setPersistenceRequired();
        destination = null;
    }
}
