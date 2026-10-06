package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.entity.GeometryHolderRangedAttacks;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import java.util.function.Consumer;

public final class GeometryHolderRangedCheck {
    private static int checks;
    public static int run(ServerLevel level) throws Exception {
        checks=0;
        try(var fixture=new SkillCheckPlayer(level,"GeometryRangedCheck")) {
            var player=fixture.player;
            for(int x=12;x<=16;x++) for(int z=12;z<=16;z++) level.getChunk(x,z);
            var protection=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime"); protection.setAccessible(true); protection.setInt(player,0);
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            player.setHealth(40);
            var boss=ModEntities.GEOMETRY_HOLDER.get().create(level);
            boss.setPos(220,245,220); level.addFreshEntity(boss);
            try {
                var firstPool=GeometryHolderRangedAttacks.class.getDeclaredField("skills");firstPool.setAccessible(true);firstPool.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BEAM_CHARGE));
                player.setPos(230,245,220); boss.setTarget(player); ready(boss); tick(boss,player);
                require(boss.attackPhase()==GeometryHolderRangedAttacks.BEAM_CHARGE && boss.attackTicks()==40,"far group starts two-second beam charge");
                Vec3 locked=boss.position();
                for(int i=0;i<39;i++) tick(boss,player);
                require(player.getHealth()==40 && boss.attackTicks()==1,"beam cannot damage during charge");
                tick(boss,player);
                require(boss.attackPhase()==GeometryHolderRangedAttacks.BEAM_ACTIVE && boss.attackTicks()==50 && player.getHealth()==36,"beam fires at forty ticks");
                var direction=boss.rangedAttacks().rayDirection(0,1);
                for(int i=0;i<49;i++) tick(boss,player);
                require(player.getHealth()==20 && boss.position().equals(locked),"beam uses normal invulnerability for five hits over 2.5 seconds and locks position");
                tick(boss,player); require(boss.attackPhase()==GeometryHolderEntity.RECOVERY,"beam damage duration ends cleanly");
                idle(boss); player.setPos(230,245,220); player.setHealth(20); player.invulnerableTime=0;
                var next=GeometryHolderRangedAttacks.class.getDeclaredField("skills"); next.setAccessible(true); next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BEAM_CHARGE));
                boss.setTarget(player); ready(boss); tick(boss,player);
                for(int i=0;i<40;i++) tick(boss,player);
                var before=boss.rangedAttacks().rayDirection(0,1);
                player.setPos(230,245,225); tick(boss,player);
                require(boss.rangedAttacks().rayDirection(0,1).equals(before),"fired laser does not chase aim");
                float hp=player.getHealth(); for(int i=0;i<15;i++) tick(boss,player);
                require(player.getHealth()==hp,"sidestepping laser avoids further hits");

                idle(boss); next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BEAM_CHARGE)); player.setPos(230,245,220); player.setHealth(20); player.invulnerableTime=0;
                boss.setTarget(player); ready(boss); tick(boss,player);
                var wall=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
                for(int y=245;y<=248;y++) for(int z=219;z<=221;z++) {
                    var pos=new BlockPos(225,y,z);wall.put(pos,level.getBlockState(pos));
                    level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                }
                try {
                    for(int i=0;i<40;i++) tick(boss,player);
                    require(boss.rangedAttacks().rayLength(0)<5 && player.getHealth()==20,"solid wall clips laser and prevents damage behind it");
                } finally {wall.forEach(level::setBlockAndUpdate);}

                idle(boss); next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BOMB_CHARGE)); player.setPos(232,245,220); boss.setTarget(player); ready(boss); tick(boss,player);
                require(boss.attackPhase()==GeometryHolderRangedAttacks.BOMB_CHARGE && boss.attackTicks()==20,"bomb has one-second throw telegraph");
                for(int i=0;i<20;i++) tick(boss,player);
                require(boss.attackPhase()==GeometryHolderRangedAttacks.BOMB_FLIGHT && boss.attackTicks()==120 && boss.rangedAttacks().bombCount()==1 && !boss.isMultipartEntity(),"thrown focus adds no independently attacking body part");
                Vec3 bomb=boss.rangedAttacks().bombPosition(1); double oldDistance=bomb.distanceToSqr(player.getBoundingBox().getCenter());
                tick(boss,player);
                require(boss.rangedAttacks().bombPosition(1).distanceTo(bomb)<=.281,"bomb has bounded flight speed");
                for(int i=0;i<10;i++) tick(boss,player);
                require(boss.rangedAttacks().bombPosition(1).distanceToSqr(player.getBoundingBox().getCenter())<oldDistance,"bomb follows target");
                bomb=boss.rangedAttacks().bombPosition(1); player.setPos(bomb.x,bomb.y-.9,bomb.z); player.setHealth(20);player.invulnerableTime=0;
                Consumer<LivingIncomingDamageEvent> inspect=event -> {
                    if(event.getEntity()==player && event.getSource().getEntity()==boss)
                        require(event.getAmount()==13 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION),"contact is thirteen-point boss-attributed explosion damage");
                };
                NeoForge.EVENT_BUS.addListener(inspect);
                try {tick(boss,player);} finally {NeoForge.EVENT_BUS.unregister(inspect);}
                require(player.getHealth()==7 && boss.attackPhase()==GeometryHolderEntity.RECOVERY,"bomb contact detonates once");
                hp=player.getHealth(); for(int i=0;i<5;i++) tick(boss,player);
                require(player.getHealth()==hp,"detonated bomb cannot deal repeat hits");

                idle(boss); next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BOMB_CHARGE)); player.setPos(232,245,220); player.setHealth(20);
                boss.setTarget(player); ready(boss); tick(boss,player); for(int i=0;i<20;i++) tick(boss,player);
                var bombWall=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
                for(int y=245;y<=248;y++) for(int z=219;z<=221;z++) {
                    var pos=new BlockPos(224,y,z);bombWall.put(pos,level.getBlockState(pos));
                    level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                }
                try {
                    for(int i=0;i<25 && boss.attackPhase()==GeometryHolderRangedAttacks.BOMB_FLIGHT;i++) tick(boss,player);
                    require(boss.attackPhase()==GeometryHolderEntity.RECOVERY && player.getHealth()==20,"bomb detonates on wall without harming distant target");
                    require(bombWall.keySet().stream().allMatch(pos -> level.getBlockState(pos).is(Blocks.STONE)),"explosion does not destroy blocks");
                } finally {bombWall.forEach(level::setBlockAndUpdate);}
                idle(boss); next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BOMB_CHARGE)); player.setPos(232,245,220);boss.setTarget(player);ready(boss);tick(boss,player);
                for(int i=0;i<20;i++) tick(boss,player);
                var state=GeometryHolderEntity.class.getDeclaredMethod("setAttack",int.class,int.class);state.setAccessible(true);
                state.invoke(boss,GeometryHolderRangedAttacks.BOMB_FLIGHT,1);tick(boss,player);
                require(boss.attackPhase()==GeometryHolderEntity.RECOVERY && player.getHealth()==20,"bomb expires safely at its lifetime boundary");

                idle(boss); next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.CROSS_CHARGE)); player.setPos(230,245,220); player.setHealth(100); boss.setTarget(player); ready(boss); tick(boss,player);
                require(boss.attackPhase()==GeometryHolderRangedAttacks.CROSS_CHARGE && boss.rangedAttacks().rayCount()==4,"cross warns in four directions");
                for(int i=0;i<19;i++) tick(boss,player);
                require(player.getHealth()==100,"cross warning is harmless");
                tick(boss,player); float yaw=boss.getYRot();
                require(boss.attackPhase()==GeometryHolderRangedAttacks.CROSS_ACTIVE && boss.attackTicks()==80,"cross begins eighty-tick sweep");
                var a=boss.rangedAttacks().rayDirection(0,1); var b=boss.rangedAttacks().rayDirection(1,1);
                require(Math.abs(a.dot(b))<1e-6 && Math.abs(a.y)<1e-6 && boss.rangedAttacks().rayDirection(2,1).dot(a)<-.999,"cross has orthogonal opposite horizontal rays");
                tick(boss,player); require(boss.getYRot()==yaw+4,"cross rotates boss each tick");
                for(int i=0;i<79;i++) tick(boss,player);
                require(boss.attackPhase()==GeometryHolderEntity.RECOVERY && boss.rangedAttacks().rayCount()==0,"cross clears all rays after sweep");

                idle(boss); next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BOMB_CHARGE)); player.setPos(230,245,220); player.setHealth(20); boss.setTarget(player); ready(boss); tick(boss,player);
                for(int i=0;i<20;i++) tick(boss,player);
                CompoundTag activeTag=new CompoundTag();boss.save(activeTag);
                var activeReload=ModEntities.GEOMETRY_HOLDER.get().create(level);activeReload.load(activeTag);
                require(activeReload.attackPhase()==GeometryHolderEntity.IDLE,"active flying cube is not recreated after reload");activeReload.discard();
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE); tick(boss,player);
                require(boss.attackPhase()==GeometryHolderEntity.RECOVERY,"invalid target clears bomb without explosion");
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                idle(boss); boss.setPos(220,248,220); player.setPos(230,245,220); player.setHealth(20);player.invulnerableTime=0;
                next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.CROSS_CHARGE)); boss.setTarget(player);ready(boss);tick(boss,player);
                require(boss.attackPhase()==GeometryHolderEntity.IDLE && boss.getDeltaMovement().y<0,"elevated boss descends before casting rather than locking an unreachable wave");
                for(int i=0;i<160 && boss.attackPhase()==GeometryHolderEntity.IDLE;i++) tick(boss,player);
                require(boss.attackPhase()==GeometryHolderRangedAttacks.CROSS_CHARGE && Math.abs(boss.getY()-player.getY())<=.1,"cross waits for smooth alignment to player feet");
                for(int i=0;i<19;i++) tick(boss,player);
                require(player.getHealth()==20,"height alignment keeps cross warning harmless");
                tick(boss,player);
                require(player.getHealth()==14,"aligned horizontal cross intersects standing player");
                CompoundTag tag=new CompoundTag(); boss.save(tag); var restored=ModEntities.GEOMETRY_HOLDER.get().create(level);restored.load(tag);
                require(restored.attackPhase()==GeometryHolderEntity.IDLE && restored.rangedAttacks().rayCount()==0,"reload does not resurrect transient ranged attacks");restored.discard();
                idle(boss);boss.setPos(220,245,220);boss.setHealth(7);player.setPos(230,245,220);player.setHealth(20);player.invulnerableTime=0;
                next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BEAM_CHARGE));boss.setTarget(player);ready(boss);tick(boss,player);
                for(int i=0;i<40;i++) tick(boss,player);
                require(boss.isSecondPhase() && boss.rangedAttacks().rayRadius()==.5,"second-stage laser widens from .35 to .5 radius");
                player.setPos(230,245,220.72);player.setHealth(20);player.invulnerableTime=0;tick(boss,player);
                require(player.getHealth()==16,"second-stage enlarged laser shell deals normal four-point damage");
                state.invoke(boss,GeometryHolderRangedAttacks.CROSS_ACTIVE,80);
                require(boss.rangedAttacks().rayRadius()==.35 && boss.attackTicks()==80,"second-stage cross keeps original width and four-second duration");
                idle(boss);player.setPos(232,245,220);player.setHealth(40);player.invulnerableTime=0;
                next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BOMB_CHARGE));boss.setTarget(player);ready(boss);tick(boss,player);for(int i=0;i<20;i++)tick(boss,player);
                require(boss.rangedAttacks().bombCount()==3 && boss.attackTicks()==120,"second stage throws three cubes with six-second lifetime");
                require(boss.rangedAttacks().bombPosition(0,1).distanceTo(boss.rangedAttacks().bombPosition(1,1))>.7
                        && boss.rangedAttacks().bombPosition(1,1).distanceTo(boss.rangedAttacks().bombPosition(2,1))>1.5,"three cubes start with visible lateral separation");
                Vec3[] old={boss.rangedAttacks().bombPosition(0,1),boss.rangedAttacks().bombPosition(1,1),boss.rangedAttacks().bombPosition(2,1)};
                tick(boss,player);boolean bounded=true;
                for(int i=0;i<3;i++)bounded &= boss.rangedAttacks().bombPosition(i,1).distanceTo(old[i])<=.281
                        && boss.rangedAttacks().bombPosition(i,1).distanceToSqr(player.getBoundingBox().getCenter())<old[i].distanceToSqr(player.getBoundingBox().getCenter());
                require(bounded,"all three cubes independently track without exceeding accepted speed");
                require(boss.getBoundingBoxForCulling().contains(boss.rangedAttacks().bombPosition(1,1))
                        && boss.getBoundingBoxForCulling().contains(boss.rangedAttacks().bombPosition(2,1)),"render bounds include both extra cubes");
                var placeBomb=GeometryHolderRangedAttacks.class.getDeclaredMethod("setBomb",int.class,Vec3.class);placeBomb.setAccessible(true);
                placeBomb.invoke(boss.rangedAttacks(),0,player.getBoundingBox().getCenter());
                placeBomb.invoke(boss.rangedAttacks(),1,new Vec3(225,247,230));placeBomb.invoke(boss.rangedAttacks(),2,new Vec3(225,247,210));
                tick(boss,player);
                require(boss.rangedAttacks().bombCount()==2 && !boss.rangedAttacks().bombActive(0) && player.getHealth()==27,
                        "first detonation removes only its own cube and retains two tracking bombs");
                placeBomb.invoke(boss.rangedAttacks(),1,player.getBoundingBox().getCenter());tick(boss,player);
                require(boss.rangedAttacks().bombCount()==1 && player.getHealth()==27,"second explosion retains normal player invulnerability and last cube");
                player.invulnerableTime=0;placeBomb.invoke(boss.rangedAttacks(),2,player.getBoundingBox().getCenter());tick(boss,player);
                require(boss.rangedAttacks().bombCount()==0 && boss.attackPhase()==GeometryHolderEntity.RECOVERY && player.getHealth()==14,
                        "last cube explodes once then enters recovery");
                idle(boss);next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BOMB_CHARGE));boss.setTarget(player);ready(boss);tick(boss,player);for(int i=0;i<20;i++)tick(boss,player);
                CompoundTag tripleTag=new CompoundTag();boss.save(tripleTag);var tripleReload=ModEntities.GEOMETRY_HOLDER.get().create(level);tripleReload.load(tripleTag);
                require(tripleReload.isSecondPhase() && tripleReload.rangedAttacks().bombCount()==0,"reload preserves phase but restores no flying cubes");tripleReload.discard();
                state.invoke(boss,GeometryHolderRangedAttacks.BOMB_FLIGHT,1);tick(boss,player);
                require(boss.attackPhase()==GeometryHolderEntity.RECOVERY && boss.rangedAttacks().bombCount()==0,"six-second boundary clears entire triple volley without detonation");
                idle(boss);next.set(boss.rangedAttacks(),new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(GeometryHolderRangedAttacks.BOMB_CHARGE));boss.setTarget(player);ready(boss);tick(boss,player);for(int i=0;i<20;i++)tick(boss,player);
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);tick(boss,player);
                require(boss.rangedAttacks().bombCount()==0 && boss.attackPhase()==GeometryHolderEntity.RECOVERY,"invalid target cancels all three cubes");
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            } finally {boss.discard();}
        }
        return checks;
    }
    private static void ready(GeometryHolderEntity boss) throws Exception {
        var f=GeometryHolderEntity.class.getDeclaredField("attackCooldown");f.setAccessible(true);f.setInt(boss,10000);
        var stagger=GeometryHolderEntity.class.getDeclaredField("lastCastStart");stagger.setAccessible(true);stagger.setInt(boss,-1000);
        var far=GeometryHolderEntity.class.getDeclaredField("rangedCooldown");far.setAccessible(true);far.setInt(boss,0);
        // Keep this fixture focused on remote spells; the ultimate has its own scheduler checks.
        var wait=com.freshfish.mathmaster.entity.GeometryHolderUltimate.class.getDeclaredField("cooldown");
        wait.setAccessible(true);wait.setInt(boss.ultimateAttack(),400);
    }
    private static void idle(GeometryHolderEntity boss) throws Exception {var m=GeometryHolderEntity.class.getDeclaredMethod("setAttack",int.class,int.class);m.setAccessible(true);m.invoke(boss,0,0);boss.setDeltaMovement(Vec3.ZERO);boss.setPos(220,245,220);}
    private static void tick(GeometryHolderEntity boss,ServerPlayer player) {if(player.invulnerableTime>0)player.invulnerableTime--;boss.tickCount++;boss.tick();}
    private static void require(boolean value,String message) {if(!value)throw new AssertionError("Geometry ranged: "+message);checks++;}
}
