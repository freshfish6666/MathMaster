package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.entity.GeometryHolderRangedAttacks;
import com.freshfish.mathmaster.entity.GeometryHolderSkillPool;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;

/** Actual dual-channel timing and cleanup; existing single-skill fixtures isolate their original geometry. */
public final class GeometryHolderConcurrentCheck {
    private static int checks;
    public static int run(ServerLevel level) throws Exception {
        checks=0;
        try(var fixture=new SkillCheckPlayer(level,"GeometryDualCheck")) {
            var player=fixture.player;
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);player.setHealth(400);
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            var invulnerability=player.getClass().getDeclaredField("spawnInvulnerableTime");invulnerability.setAccessible(true);invulnerability.setInt(player,0);
            var boss=ModEntities.GEOMETRY_HOLDER.get().create(level);boss.setPos(220,245,220);level.addFreshEntity(boss);
            try {
                pool(boss,"nearSkills",new GeometryHolderSkillPool(GeometryHolderEntity.PRISON));
                pool(boss.rangedAttacks(),"skills",new GeometryHolderSkillPool(GeometryHolderRangedAttacks.BEAM_CHARGE));
                set(boss,"attackCooldown",0);set(boss,"rangedCooldown",0);set(boss.ultimateAttack(),"cooldown",10000);
                player.setPos(222,245,220);boss.setTarget(player);tick(boss,player);
                var center=boss.prisonCenter();
                require(boss.nearPhase()==GeometryHolderEntity.PRISON && boss.rangedAttacks().phase()==0,"near cast starts first at close range");
                for(int i=0;i<15;i++)tick(boss,player);
                require(boss.rangedAttacks().phase()==0,"other group does not start before 0.8 second");
                tick(boss,player);
                require(boss.nearPhase()==GeometryHolderEntity.PRISON && boss.nearTicks()==24
                        && boss.rangedAttacks().phase()==GeometryHolderRangedAttacks.BEAM_CHARGE && boss.rangedAttacks().ticks()==40,
                        "independent charge starts at exact sixteen-tick spacing");
                for(int i=0;i<24;i++)tick(boss,player);
                require(boss.nearPhase()==GeometryHolderEntity.PRISON_ACTIVE && boss.nearTicks()==30
                        && boss.rangedAttacks().phase()==GeometryHolderRangedAttacks.BEAM_CHARGE && boss.rangedAttacks().ticks()==16,
                        "near release does not replace ranged timer");
                for(int i=0;i<16;i++)tick(boss,player);
                require(boss.nearPhase()==GeometryHolderEntity.PRISON_ACTIVE && boss.rangedAttacks().phase()==GeometryHolderRangedAttacks.BEAM_ACTIVE,
                        "both damage windows overlap");
                for(int i=0;i<14;i++)tick(boss,player);
                require(boss.nearPhase()==GeometryHolderEntity.RECOVERY && boss.nearTicks()==30,"prison keeps thirty-tick stun");
                int beamTicks=boss.rangedAttacks().ticks();
                tick(boss,player);require(boss.rangedAttacks().ticks()==beamTicks-1,"already fired beam continues during stun");
                state(boss,"setRangedAttack",0,0);set(boss,"rangedCooldown",0);
                for(int i=0;i<10;i++)tick(boss,player);
                require(boss.rangedAttacks().phase()==0 && boss.nearPhase()==GeometryHolderEntity.RECOVERY,"stun blocks new ranged cast");

                state(boss,"setAttack",0,0);set(boss,"attackCooldown",0);set(boss,"rangedCooldown",0);set(boss,"lastCastStart",-1000);
                player.setPos(230,245,220);boss.setTarget(player);tick(boss,player);
                require(boss.nearPhase()==0 && boss.rangedAttacks().phase()==GeometryHolderRangedAttacks.BEAM_CHARGE,"far player starts ranged only");
                for(int i=0;i<17;i++)tick(boss,player);
                require(boss.nearPhase()==0,"far range never admits near group despite ready cooldown");
                player.setPos(222,245,220);tick(boss,player);
                require(boss.nearPhase()==GeometryHolderEntity.PRISON && boss.rangedAttacks().phase()==GeometryHolderRangedAttacks.BEAM_CHARGE,
                        "moving close admits near cast alongside ongoing ranged charge");
                center=boss.prisonCenter();
                state(boss,"setRangedAttack",GeometryHolderRangedAttacks.CROSS_ACTIVE,80);
                for(int i=0;i<5;i++)tick(boss,player);
                require(boss.prisonCenter().distanceToSqr(center)<1e-10,"cross rotation cannot move fixed prison volume");
                int nearRemaining=boss.nearTicks(),farRemaining=boss.rangedAttacks().ticks();
                set(boss.ultimateAttack(),"cooldown",0);tick(boss,player);
                require(boss.ultimateAttack().ticks()==80 && boss.nearTicks()==nearRemaining-1 && boss.rangedAttacks().ticks()==farRemaining-1,
                        "ultimate starts midway through both casts without replacing their timers");
                state(boss,"setAttack",com.freshfish.mathmaster.entity.GeometryHolderUltimate.ACTIVE,1);
                tick(boss,player);
                require(!boss.ultimateAttack().active() && boss.nearPhase()==GeometryHolderEntity.PRISON
                        && boss.rangedAttacks().phase()==GeometryHolderRangedAttacks.CROSS_ACTIVE,
                        "ultimate expiry clears only its own channel");
                set(boss,"attackCooldown",40);set(boss,"rangedCooldown",40);boss.setHealth(79);tick(boss,player);
                require(value(boss,"attackCooldown")==32 && value(boss,"rangedCooldown")==32,"phase two scales both future cooldowns once");
                CompoundTag save=new CompoundTag();boss.save(save);
                var loaded=ModEntities.GEOMETRY_HOLDER.get().create(level);loaded.load(save);
                try {require(loaded.nearPhase()==0 && loaded.rangedAttacks().phase()==0 && loaded.rangedAttacks().bombCount()==0,
                        "reload clears both transient channels");} finally {loaded.discard();}

                state(boss,"setAttack",0,0);set(boss,"attackCooldown",0);set(boss,"rangedCooldown",0);set(boss,"lastCastStart",-1000);
                set(boss.ultimateAttack(),"cooldown",0);boss.setTarget(player);tick(boss,player);
                require(boss.ultimateAttack().active() && boss.nearPhase()==GeometryHolderEntity.PRISON && boss.rangedAttacks().phase()==0,
                        "ultimate starts independently alongside near cast");
                for(int i=0;i<17;i++)tick(boss,player);
                require(boss.ultimateAttack().active() && boss.nearPhase()==GeometryHolderEntity.PRISON && boss.rangedAttacks().phase()==GeometryHolderRangedAttacks.BEAM_CHARGE,
                        "all three groups overlap without replacing each other");
                boss.setHealth(0);tick(boss,player);
                require(boss.nearPhase()==0 && boss.rangedAttacks().phase()==0 && boss.ultimateAttack().cubeCount()==0,"death clears both groups and ultimate");
            } finally {boss.discard();}
            blockedHeight(level,player);
        }
        return checks;
    }
    private static void blockedHeight(ServerLevel level,net.minecraft.server.level.ServerPlayer player) throws Exception {
        for(boolean second:new boolean[]{false,true}) for(int terrain=0;terrain<3;terrain++) for(boolean near:new boolean[]{false,true}) {
            boolean ceiling=terrain==2;
            double y=terrain==0 ? 246 : terrain==1 ? 245.5 : 245.375;
            int blockY=ceiling ? 249 : 245;
            var blocks=new java.util.HashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>();
            var boss=ModEntities.GEOMETRY_HOLDER.get().create(level);
            boss.setPos(230.5,y,220.5);if(second)boss.setHealth(79);level.addFreshEntity(boss);
            try {
                for(int x=229;x<=231;x++)for(int z=219;z<=221;z++) {
                    var pos=new net.minecraft.core.BlockPos(x,blockY,z);blocks.put(pos,level.getBlockState(pos));
                    level.setBlockAndUpdate(pos,(terrain==1 ? net.minecraft.world.level.block.Blocks.STONE_SLAB
                            : net.minecraft.world.level.block.Blocks.STONE).defaultBlockState());
                }
                player.setPos(near ? 234 : 240.5,y+(ceiling ? 1 : -1),220.5);player.setHealth(400);player.invulnerableTime=0;
                boss.setTarget(player);
                pool(boss,"nearSkills",new GeometryHolderSkillPool(GeometryHolderEntity.PRISON));
                pool(boss.rangedAttacks(),"skills",new GeometryHolderSkillPool(GeometryHolderRangedAttacks.BEAM_CHARGE));
                set(boss,"attackCooldown",0);set(boss,"rangedCooldown",0);set(boss.ultimateAttack(),"cooldown",10000);
                require(boss.hasLineOfSight(player) && Math.abs(player.getY()-boss.getY())>.9,
                        "terrain fixture has visible player at different feet height");
                tick(boss,player);
                require(near ? boss.nearPhase()==GeometryHolderEntity.PRISON : boss.rangedAttacks().phase()==GeometryHolderRangedAttacks.BEAM_CHARGE,
                        "solid floor/slab/ceiling cannot prevent near or far opening in either stage");
                for(int i=0;i<16;i++)tick(boss,player);
                require(boss.rangedAttacks().phase()==GeometryHolderRangedAttacks.BEAM_CHARGE && (!near || boss.nearPhase()==GeometryHolderEntity.PRISON),
                        "blocked alignment retains independent groups and stagger");
                for(int i=0;i<(near ? 40 : 24);i++)tick(boss,player);
                require(player.getHealth()<400 && boss.getY()==y,
                        "uneven terrain casts actually damage player without moving through blocks");
            } finally {boss.discard();blocks.forEach(level::setBlockAndUpdate);}
        }
    }
    private static void pool(Object object,String name,Object pool) throws Exception {var f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,pool);}
    private static void set(Object object,String name,int value) throws Exception {var f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.setInt(object,value);}
    private static int value(Object object,String name) throws Exception {var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.getInt(object);}
    private static void state(GeometryHolderEntity boss,String name,int phase,int ticks) throws Exception {var m=GeometryHolderEntity.class.getDeclaredMethod(name,int.class,int.class);m.setAccessible(true);m.invoke(boss,phase,ticks);}
    private static void tick(GeometryHolderEntity boss,net.minecraft.server.level.ServerPlayer player) {if(player.invulnerableTime>0)player.invulnerableTime--;boss.tickCount++;boss.tick();}
    private static void require(boolean ok,String message) {if(!ok)throw new AssertionError("Geometry concurrent: "+message);checks++;}
}
