package com.freshfish.mathmaster.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Boss-owned spell geometry: no secondary hurt box, projectile registration or custom packets. */
public final class GeometryHolderRangedAttacks {
    public static final int BEAM_CHARGE=5, BEAM_ACTIVE=6, BOMB_CHARGE=7, BOMB_FLIGHT=8, CROSS_CHARGE=9, CROSS_ACTIVE=10;
    public static final int BEAM_CHARGE_TICKS=40, BEAM_TICKS=50, BOMB_TICKS=120, CROSS_TICKS=80;
    public static final double BEAM_RADIUS=.35, BOMB_SPEED=.28;
    public static final float BEAM_DAMAGE=4, CROSS_DAMAGE=6, BOMB_DAMAGE=13;
    private static final EntityDataAccessor<Vector3f> DIRECTION=GeometryHolderEntity.RANGED_DIRECTION;
    private static final EntityDataAccessor<Vector3f> BOMB_OFFSET=GeometryHolderEntity.RANGED_BOMB_OFFSET;
    private static final EntityDataAccessor<Vector3f> LENGTHS=GeometryHolderEntity.RANGED_LENGTHS;
    private static final EntityDataAccessor<Float> FOURTH_LENGTH=GeometryHolderEntity.RANGED_FOURTH_LENGTH;
    private final GeometryHolderEntity boss;
    private final GeometryHolderSkillPool skills = new GeometryHolderSkillPool(BEAM_CHARGE, BOMB_CHARGE, CROSS_CHARGE);
    private final Vec3[] bombVelocity={Vec3.ZERO,Vec3.ZERO,Vec3.ZERO};
    private final Vec3[] previousBomb={Vec3.ZERO,Vec3.ZERO,Vec3.ZERO};
    private final Vec3[] clientBomb={Vec3.ZERO,Vec3.ZERO,Vec3.ZERO};
    private int clientBombMask;
    private static EntityDataAccessor<Vector3f> bombKey(int index) {
        return switch(index) {case 0 -> BOMB_OFFSET;case 1 -> GeometryHolderEntity.RANGED_BOMB_OFFSET_2;case 2 -> GeometryHolderEntity.RANGED_BOMB_OFFSET_3;default -> throw new IllegalArgumentException("Bomb index");};
    }
    public boolean bombActive(int index) {
        return phase()==BOMB_FLIGHT && (boss.getEntityData().get(GeometryHolderEntity.RANGED_BOMB_MASK)&(1<<index))!=0;
    }
    public int bombCount() { return phase()==BOMB_FLIGHT ? Integer.bitCount(boss.getEntityData().get(GeometryHolderEntity.RANGED_BOMB_MASK)) : 0; }
    void clearBombs() { boss.getEntityData().set(GeometryHolderEntity.RANGED_BOMB_MASK,0); }
    private void removeBomb(int index) {
        boss.getEntityData().set(GeometryHolderEntity.RANGED_BOMB_MASK,boss.getEntityData().get(GeometryHolderEntity.RANGED_BOMB_MASK)&~(1<<index));
        if(bombCount()==0) boss.finishRanged();
    }
    public double rayRadius() { return phase()==BEAM_ACTIVE && boss.isSecondPhase() ? .5 : BEAM_RADIUS; }

    GeometryHolderRangedAttacks(GeometryHolderEntity boss) { this.boss=boss; }
    static void define(SynchedEntityData.Builder builder) {
        builder.define(DIRECTION,new Vector3f(0,0,1)); builder.define(BOMB_OFFSET,new Vector3f());
        builder.define(LENGTHS,new Vector3f()); builder.define(FOURTH_LENGTH,0F);
        builder.define(GeometryHolderEntity.RANGED_BOMB_OFFSET_2,new Vector3f());
        builder.define(GeometryHolderEntity.RANGED_BOMB_OFFSET_3,new Vector3f());
        builder.define(GeometryHolderEntity.RANGED_BOMB_MASK,0);
    }
    void reset() {
        skills.reset();clientBombMask=0;clearBombs();
        java.util.Arrays.fill(bombVelocity,Vec3.ZERO);java.util.Arrays.fill(previousBomb,Vec3.ZERO);java.util.Arrays.fill(clientBomb,Vec3.ZERO);
    }
    public int phase() { return boss.getEntityData().get(GeometryHolderEntity.RANGED_ATTACK); }
    public int ticks() { return boss.getEntityData().get(GeometryHolderEntity.RANGED_TICKS); }
    public boolean active() { return phase()>=BEAM_CHARGE && phase()<=CROSS_ACTIVE; }
    public Vec3 bombPosition(float partial) { return bombPosition(0,partial); }
    public Vec3 bombPosition(int index,float partial) {
        Vector3f offset=boss.getEntityData().get(bombKey(index));
        Vec3 current=boss.position().add(offset.x,offset.y,offset.z);
        return boss.level().isClientSide() ? previousBomb[index].lerp(clientBomb[index],partial) : current;
    }
    void clientTick() {
        int mask=boss.getEntityData().get(GeometryHolderEntity.RANGED_BOMB_MASK);
        for(int i=0;i<3;i++) {
            Vector3f offset=boss.getEntityData().get(bombKey(i));
            previousBomb[i]=clientBomb[i];
            clientBomb[i]=boss.position().add(offset.x,offset.y,offset.z);
            if((clientBombMask&(1<<i))==0 || previousBomb[i].equals(Vec3.ZERO)) previousBomb[i]=clientBomb[i];
        }
        clientBombMask=mask;
    }
    public int rayCount() {
        return switch(phase()) {case BEAM_ACTIVE -> 1; case CROSS_ACTIVE,CROSS_CHARGE -> 4; default -> 0;};
    }
    public Vec3 rayDirection(int index, float partial) {
        if(phase()==BEAM_ACTIVE) {
            Vector3f direction=boss.getEntityData().get(DIRECTION); return new Vec3(direction.x,direction.y,direction.z);
        }
        double yaw=(net.minecraft.util.Mth.rotLerp(partial,boss.yRotO,boss.getYRot())+index*90)*Math.PI/180;
        return new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
    }
    public float rayLength(int index) {
        Vector3f lengths=boss.getEntityData().get(LENGTHS);
        return switch(index) {case 0 -> lengths.x; case 1 -> lengths.y; case 2 -> lengths.z; default -> boss.getEntityData().get(FOURTH_LENGTH);};
    }
    public float chargeGlow(float partial) {
        return phase()==BEAM_CHARGE ? net.minecraft.util.Mth.clamp((40-ticks()+partial)/40F,0,1)
                : phase()==BEAM_ACTIVE ? 1 : 0;
    }
    boolean start(LivingEntity target) {
        int phase=skills.pick(boss.getRandom());
        boss.setRangedAttack(phase,phase==BEAM_CHARGE ? BEAM_CHARGE_TICKS : 20);
        boss.setDeltaMovement(Vec3.ZERO); aim(target);
        if(phase==CROSS_CHARGE) updateRays(false);
        return true;
    }
    boolean advance(LivingEntity target) {
        int phase=phase();
        if(phase==GeometryHolderEntity.RECOVERY) {
            boss.setRangedAttack(ticks()<=1 ? GeometryHolderEntity.IDLE : phase,Math.max(0,ticks()-1));
            return true;
        }
        if(!active()) return false;
        boss.setDeltaMovement(Vec3.ZERO);
        if((phase==BEAM_CHARGE || phase==BOMB_CHARGE || phase==CROSS_CHARGE || phase==BOMB_FLIGHT) && target==null) {
            boss.finishRanged(); return true;
        }
        int remaining=ticks()-1;
        if(phase==BEAM_CHARGE || phase==BOMB_CHARGE || phase==CROSS_CHARGE) {
            if(phase!=CROSS_CHARGE) aim(target);
            boss.setRangedAttack(phase,remaining);
            chargeParticles(phase,remaining);
            if(remaining<=0) {
                if(phase==BEAM_CHARGE) {boss.setRangedAttack(BEAM_ACTIVE,BEAM_TICKS); updateRays(true);}
                else if(phase==CROSS_CHARGE) {boss.setRangedAttack(CROSS_ACTIVE,CROSS_TICKS);updateRays(true);}
                else {
                    boss.setRangedAttack(BOMB_FLIGHT,BOMB_TICKS);
                    int count=boss.isSecondPhase() ? 3 : 1;
                    boss.getEntityData().set(GeometryHolderEntity.RANGED_BOMB_MASK,count==3 ? 7 : 1);
                    Vec3 center=boss.castingCenter();
                    Vec3 forward=target.getBoundingBox().getCenter().subtract(center).normalize();
                    Vec3 side=new Vec3(-forward.z,0,forward.x).normalize();
                    for(int i=0;i<count;i++) {
                        double spread=i==0 ? 0 : i==1 ? -.8 : .8;
                        Vec3 offset=side.scale(spread);
                        Vec3 launch=i==0 ? center : clippedEnd(center,offset.normalize(),offset.length());
                        setBomb(i,launch);
                        bombVelocity[i]=forward.add(side.scale(spread*.55)).normalize().scale(BOMB_SPEED);
                    }
                }
            }
        } else if(phase==BOMB_FLIGHT) {
            if(remaining<=0) {boss.finishRanged();return true;}
            boss.setRangedAttack(phase,remaining); for(int i=0;i<3;i++) if(bombActive(i)) moveBomb(target,i);
        } else {
            if(remaining<=0) {boss.finishRanged();return true;}
            boss.setRangedAttack(phase,remaining);
            if(phase==CROSS_ACTIVE) boss.setYRot(boss.getYRot()+4);
            updateRays(true);
        }
        return true;
    }
    private void aim(LivingEntity target) {
        Vec3 offset=target.getBoundingBox().getCenter().subtract(boss.position());
        boss.setYRot((float)(Math.atan2(offset.z,offset.x)*180/Math.PI)-90);
        Vec3 direction=target.getBoundingBox().getCenter().subtract(boss.castingCenter()).normalize();
        boss.getEntityData().set(DIRECTION,new Vector3f((float)direction.x,(float)direction.y,(float)direction.z));
    }
    private void chargeParticles(int phase,int remaining) {
        if(!(boss.level() instanceof ServerLevel server) || remaining%2!=0) return;
        Vec3 center=boss.castingCenter();
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK,center.x,center.y,center.z,
                phase==BEAM_CHARGE ? 2+(40-remaining)/4 : 6,.3,.3,.3,.02);
    }
    private boolean loadedSegment(Vec3 start, Vec3 end) {
        int minX=BlockPos.containing(start).getX()>>4, maxX=BlockPos.containing(end).getX()>>4;
        int minZ=BlockPos.containing(start).getZ()>>4, maxZ=BlockPos.containing(end).getZ()>>4;
        for(int x=Math.min(minX,maxX);x<=Math.max(minX,maxX);x++)
            for(int z=Math.min(minZ,maxZ);z<=Math.max(minZ,maxZ);z++)
                if(!boss.level().hasChunkAt(new BlockPos(x<<4,0,z<<4))) return false;
        return true;
    }
    private Vec3 clippedEnd(Vec3 start,Vec3 direction,double range) {
        Vec3 end=start.add(direction.scale(range));
        if(!loadedSegment(start,end)) {
            double loadedRange=0;
            for(double step=.5;step<=range;step+=.5) {
                if(!loadedSegment(start,start.add(direction.scale(step)))) break;
                loadedRange=step;
            }
            end=start.add(direction.scale(loadedRange));
        }
        return boss.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,boss)).getLocation();
    }
    private void updateRays(boolean damage) {
        int count=rayCount(); float[] lengths=new float[4];
        Vec3 start=boss.castingCenter();
        for(int i=0;i<count;i++) {
            Vec3 end=clippedEnd(start,rayDirection(i,1),count==1 ? 32 : 24);
            lengths[i]=(float)start.distanceTo(end);
            if(damage) damageLine(start,end);
        }
        boss.getEntityData().set(LENGTHS,new Vector3f(lengths[0],lengths[1],lengths[2]));
        boss.getEntityData().set(FOURTH_LENGTH,lengths[3]);
    }
    private void damageLine(Vec3 start,Vec3 end) {
        double radius=rayRadius();
        for(Player player:boss.level().getEntitiesOfClass(Player.class,new AABB(start,end).inflate(radius),boss::eligible))
            if(player.getBoundingBox().inflate(radius).contains(start)
                    || player.getBoundingBox().inflate(radius).clip(start,end).isPresent())
                player.hurt(boss.damageSources().mobAttack(boss),phase()==BEAM_ACTIVE ? BEAM_DAMAGE : CROSS_DAMAGE);
    }
    private void setBomb(int index,Vec3 position) {
        Vec3 offset=position.subtract(boss.position());
        boss.getEntityData().set(bombKey(index),new Vector3f((float)offset.x,(float)offset.y,(float)offset.z));
    }
    private void moveBomb(LivingEntity target,int index) {
        Vec3 start=bombPosition(index,1);
        Vec3 desired=target.getBoundingBox().getCenter().subtract(start).normalize().scale(BOMB_SPEED);
        bombVelocity[index]=bombVelocity[index].lerp(desired,.08);
        Vec3 end=start.add(bombVelocity[index]);
        if(!loadedSegment(start,end)) {removeBomb(index);return;}
        var wall=boss.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,boss));
        Vec3 contact=wall.getType()==HitResult.Type.MISS ? end : wall.getLocation();
        boolean hit=wall.getType()!=HitResult.Type.MISS;
        double nearest=start.distanceToSqr(contact);
        for(Player player:boss.level().getEntitiesOfClass(Player.class,new AABB(start,end).inflate(.4),boss::eligible)) {
            var bounds=player.getBoundingBox().inflate(.4);
            Vec3 point=bounds.contains(start) ? start : bounds.clip(start,end).orElse(null);
            if(point!=null && start.distanceToSqr(point)<=nearest) {contact=point; nearest=start.distanceToSqr(point);hit=true;}
        }
        setBomb(index,contact);
        if(hit) explode(contact,index);
        else if(boss.level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.END_ROD,end.x,end.y,end.z,2,.1,.1,.1,0);
    }
    private void explode(Vec3 center,int index) {
        explodeAt(center,3,.8F,BOMB_DAMAGE);
        removeBomb(index);
    }
    void explodeAt(Vec3 center,double radius,float volume,float damage) {
        for(LivingEntity victim:boss.level().getEntitiesOfClass(LivingEntity.class,AABB.ofSize(center,radius*2,radius*2,radius*2),entity -> entity!=boss
                && entity.isAlive() && !entity.isSpectator() && (!(entity instanceof Player player)||!player.isCreative()))) {
            if(victim.getBoundingBox().distanceToSqr(center)<=radius*radius && loadedSegment(center,victim.getBoundingBox().getCenter()) && boss.level().clip(new ClipContext(center,victim.getBoundingBox().getCenter(),
                    ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,boss)).getType()==HitResult.Type.MISS)
                victim.hurt(new net.minecraft.world.damagesource.DamageSource(boss.damageSources().explosion(boss,boss).typeHolder(),boss,boss,center),damage);
        }
        if(boss.level() instanceof ServerLevel server) {
            server.sendParticles(radius>1 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION,center.x,center.y,center.z,1,0,0,0,0);
            GeometryHolderAudio.play(boss,com.freshfish.mathmaster.init.ModSounds.GEOMETRY_BOMB_EXPLODE.get(),volume,center);
        }
    }
}
