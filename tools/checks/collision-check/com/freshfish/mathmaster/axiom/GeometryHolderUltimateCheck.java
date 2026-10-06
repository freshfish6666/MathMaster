package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.entity.GeometryHolderUltimate;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import java.util.function.Consumer;

public final class GeometryHolderUltimateCheck {
    private static int checks;
    public static int run(ServerLevel level) throws Exception {
        checks=0;
        for(int x=12;x<=15;x++) for(int z=12;z<=15;z++) level.getChunk(x,z);
        try(var fixture=new SkillCheckPlayer(level,"GeometryUltimateCheck")) {
            var player=fixture.player;
            var protection=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            protection.setAccessible(true);protection.setInt(player,0);
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            var boss=ModEntities.GEOMETRY_HOLDER.get().create(level);
            boss.setPos(220,245,220); level.addFreshEntity(boss);
            int[] sounds={0}, hits={0};
            Consumer<PlayLevelSoundEvent> listen=e -> {
                if(e.getSound()!=null && e.getSound().value().getLocation().getPath().equals("entity.geometry_holder.bomb_explode")) sounds[0]++;
            };
            Consumer<LivingIncomingDamageEvent> damage=e -> {
                if(e.getEntity()==player && e.getSource().getEntity()==boss) {
                    require(e.getAmount()==10 && e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION),"ten-point explosion retains boss ownership");
                    hits[0]++;
                }
            };
            NeoForge.EVENT_BUS.addListener(listen);NeoForge.EVENT_BUS.addListener(damage);
            try {
                player.setPos(240,245,220);boss.setTarget(player);
                ready(boss);tick(boss,player);
                require(boss.ultimateAttack().active() && boss.ultimateAttack().ticks()==80 && boss.ultimateAttack().cubeCount()==12,
                        "far target starts twelve cubes immediately for four seconds");
                var locked=boss.position();
                for(int i=0;i<12;i++) {
                    var center=boss.ultimateAttack().cubePosition(i);
                    var bounds=boss.ultimateAttack().cubeBounds(i);
                    require(center.subtract(locked).horizontalDistance()<=16 && Math.abs(bounds.getXsize()-.5)<1e-6
                            && Math.abs(bounds.getYsize()-.5)<1e-6 && Math.abs(bounds.getZsize()-.5)<1e-6,
                            "cube "+i+" matches half-block contact volume and sixteen-block range");
                    require(boss.getBoundingBoxForCulling().contains(center),"culling includes cube "+i);
                }
                for(int i=0;i<79;i++)tick(boss,player);
                require(boss.ultimateAttack().ticks()==1 && sounds[0]==0 && boss.ultimateAttack().cubeCount()==12
                        && boss.position().equals(locked),"stationary cubes wait until exact four-second boundary");
                tick(boss,player);
                require(sounds[0]==12 && boss.ultimateAttack().cubeCount()==0 && !boss.ultimateAttack().active(),
                        "each cube expires with one explosion and independent cleanup");
                for(int i=0;i<3;i++)tick(boss,player);
                require(sounds[0]==12,"expired cubes do not repeat explosions");
                require(cooldown(boss)>9000,"ultimate does not overwrite near cooldown");

                idle(boss);boss.setHealth(7);player.setPos(222,245,220);boss.setTarget(player);ready(boss);tick(boss,player);
                require(boss.ultimateAttack().cubeCount()==20 && boss.ultimateAttack().active(),
                        "near target also casts distance-independent twenty-cube second-stage ultimate");
                require(boss.getBoundingBoxForCulling().contains(boss.ultimateAttack().cubePosition(19)),
                        "render bounds include appended twentieth cube");
                int before=sounds[0];Vec3 contact=boss.ultimateAttack().cubePosition(0);
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
                player.setPos(contact.x,contact.y-.9,contact.z);player.setHealth(40);player.invulnerableTime=0;
                tick(boss,player);
                require(player.getHealth()==30 && !boss.ultimateAttack().cubeActive(0) && boss.ultimateAttack().cubeCount()==19
                        && sounds[0]==before+1 && hits[0]==1,"player contact explodes only that cube once");
                tick(boss,player);require(player.getHealth()==30 && sounds[0]==before+1,"removed contact cube cannot hit again");
                Vec3 last=boss.ultimateAttack().cubePosition(19);
                player.setPos(last.x,last.y-.9,last.z);player.invulnerableTime=0;tick(boss,player);
                require(player.getHealth()==20 && !boss.ultimateAttack().cubeActive(19) && boss.ultimateAttack().cubeCount()==18
                        && sounds[0]==before+2,"twentieth cube independently contacts and removes its high mask bit");
                before++;
                CompoundTag tag=new CompoundTag();boss.save(tag);
                var restored=ModEntities.GEOMETRY_HOLDER.get().create(level);restored.load(tag);
                require(restored.isSecondPhase() && restored.attackPhase()==GeometryHolderEntity.IDLE && restored.ultimateAttack().cubeCount()==0,
                        "save reload retains stage without restoring transient cubes");restored.discard();
                player.setGameMode(GameType.CREATIVE);tick(boss,player);
                require(boss.ultimateAttack().cubeCount()==0 && sounds[0]==before+1,"invalid target cancels remaining cubes without damage");
                require(cooldown(boss)>7000,"stage change scales isolated near cooldown once");
                player.setGameMode(GameType.SURVIVAL);

                // Isolate one stationary cube to verify true spherical damage bounds and wall shielding.
                idle(boss);player.setPos(230,245,220);boss.setTarget(player);ready(boss);tick(boss,player);
                setSingle(boss,new Vec3(230,245.9,220));player.setHealth(20);player.invulnerableTime=0;tick(boss,player);
                require(player.getHealth()==10,"single cube contact deals ten damage");
                idle(boss);boss.setTarget(player);ready(boss);tick(boss,player);
                setSingle(boss,new Vec3(232,245.9,220));state(boss,GeometryHolderUltimate.ACTIVE,1);
                player.setHealth(20);player.invulnerableTime=0;tick(boss,player);
                require(player.getHealth()==20,"player beyond one-block sphere takes no timed explosion damage");
                idle(boss);boss.setTarget(player);ready(boss);tick(boss,player);
                setSingle(boss,new Vec3(231,245.9,220));state(boss,GeometryHolderUltimate.ACTIVE,1);
                player.setHealth(20);player.invulnerableTime=0;tick(boss,player);
                require(player.getHealth()==10,"player within one-block sphere takes timed explosion damage without contact");
                idle(boss);boss.setTarget(player);ready(boss);tick(boss,player);
                setSingle(boss,new Vec3(232,245.9,220));player.setPos(230.75,245,220);
                var wall=new BlockPos(231,245,220);var old=level.getBlockState(wall);level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
                try {
                    state(boss,GeometryHolderUltimate.ACTIVE,1);player.setHealth(20);player.invulnerableTime=0;tick(boss,player);
                    require(player.getHealth()==20 && level.getBlockState(wall).is(Blocks.STONE),"solid wall blocks blast and is preserved");
                } finally {level.setBlockAndUpdate(wall,old);}

                idle(boss);player.setPos(230,245,220);boss.setTarget(player);ready(boss);tick(boss,player);
                before=sounds[0];boss.setHealth(0);tick(boss,player);
                require(boss.ultimateAttack().cubeCount()==0 && sounds[0]==before,"boss death clears all cubes without detonating");

                // Exercise real scheduling instead of forcing a spell start.
                var scheduler=ModEntities.GEOMETRY_HOLDER.get().create(level);scheduler.setPos(220,245,220);
                try {
                    scheduler.setTarget(player);
                    var gap=GeometryHolderEntity.class.getDeclaredField("attackCooldown");gap.setAccessible(true);gap.setInt(scheduler,2);
                    var far=GeometryHolderEntity.class.getDeclaredField("rangedCooldown");far.setAccessible(true);far.setInt(scheduler,0);
                    var wait=GeometryHolderUltimate.class.getDeclaredField("cooldown");wait.setAccessible(true);wait.setInt(scheduler.ultimateAttack(),1);
                    tick(scheduler,player);
                    require(scheduler.ultimateAttack().active(),"ultimate ignores ordinary cooldown and overlaps ranged casting");
                    tick(scheduler,player);tick(scheduler,player);
                    require(scheduler.ultimateAttack().active() && scheduler.rangedAttacks().active(),"ultimate and remote group retain independent active states");
                    require(wait.getInt(scheduler.ultimateAttack())==400,"first-stage ultimate cooldown resets to twenty seconds");
                    scheduler.setHealth(7);tick(scheduler,player);
                    require(wait.getInt(scheduler.ultimateAttack())==320 && scheduler.ultimateAttack().cubeCount()==12,
                            "phase change scales future wait but does not add cubes midway through existing cast");
                } finally {scheduler.discard();}

                // Fixed-seed casts check area coverage, rather than assuming linear radius is uniform.
                var distribution=ModEntities.GEOMETRY_HOLDER.get().create(level);
                distribution.setPos(220,245,220);distribution.setHealth(7);distribution.getRandom().setSeed(20261004L);
                player.setPos(240,245,220);
                var start=GeometryHolderUltimate.class.getDeclaredMethod("start",net.minecraft.world.entity.LivingEntity.class);
                start.setAccessible(true);
                int samples=0,inner=0;double radialSum=0,maxRadius=16-.5/Math.sqrt(2);
                boolean allInside=true, allFull=true;
                try {
                    for(int cast=0;cast<64;cast++) {
                        start.invoke(distribution.ultimateAttack(),player);
                        allFull &= distribution.ultimateAttack().cubeCount()==20;
                        for(int i=0;i<distribution.ultimateAttack().cubeCount();i++) {
                            var center=distribution.ultimateAttack().cubePosition(i);
                            double squared=center.subtract(distribution.position()).horizontalDistanceSqr();
                            radialSum+=squared/(maxRadius*maxRadius);samples++;
                            if(squared<=maxRadius*maxRadius/4)inner++;
                            var bounds=distribution.ultimateAttack().cubeBounds(i);
                            for(double x:new double[]{bounds.minX,bounds.maxX}) for(double z:new double[]{bounds.minZ,bounds.maxZ})
                                allInside &= (x-220)*(x-220)+(z-220)*(z-220)<=256+1e-5;
                        }
                    }
                    require(allFull && samples==1280,"open arena repeatedly fills all twenty positions");
                    require(allInside,"all cube corners remain within sixteen-block disk");
                    require((double)inner/samples>.18 && (double)inner/samples<.32,
                            "inner half-radius receives approximately one quarter of area samples");
                    require(radialSum/samples>.44 && radialSum/samples<.56,
                            "mean squared radius follows uniform area distribution rather than outer ring bias");
                } finally {distribution.discard();}
            } finally {
                boss.discard();NeoForge.EVENT_BUS.unregister(listen);NeoForge.EVENT_BUS.unregister(damage);
            }
        }
        return checks;
    }
    private static void setSingle(GeometryHolderEntity boss,Vec3 center) throws Exception {
        var place=GeometryHolderUltimate.class.getDeclaredMethod("setCube",int.class,Vec3.class);place.setAccessible(true);place.invoke(boss.ultimateAttack(),0,center);
        var mask=GeometryHolderEntity.class.getDeclaredField("ULTIMATE_MASK");mask.setAccessible(true);
        @SuppressWarnings("unchecked") var key=(net.minecraft.network.syncher.EntityDataAccessor<Integer>)mask.get(null);
        boss.getEntityData().set(key,1);
    }
    private static void ready(GeometryHolderEntity boss) throws Exception {
        var gap=GeometryHolderEntity.class.getDeclaredField("attackCooldown");gap.setAccessible(true);gap.setInt(boss,10000);
        var far=GeometryHolderEntity.class.getDeclaredField("rangedCooldown");far.setAccessible(true);far.setInt(boss,10000);
        var wait=GeometryHolderUltimate.class.getDeclaredField("cooldown");wait.setAccessible(true);wait.setInt(boss.ultimateAttack(),0);
    }
    private static int cooldown(GeometryHolderEntity boss) throws Exception {var f=GeometryHolderEntity.class.getDeclaredField("attackCooldown");f.setAccessible(true);return f.getInt(boss);}
    private static void state(GeometryHolderEntity boss,int phase,int ticks) throws Exception {var m=GeometryHolderEntity.class.getDeclaredMethod("setAttack",int.class,int.class);m.setAccessible(true);m.invoke(boss,phase,ticks);}
    private static void idle(GeometryHolderEntity boss) throws Exception {state(boss,0,0);boss.setDeltaMovement(Vec3.ZERO);boss.setPos(220,245,220);}
    private static void tick(GeometryHolderEntity boss,ServerPlayer player) {if(player.invulnerableTime>0)player.invulnerableTime--;boss.tickCount++;boss.tick();}
    private static void require(boolean value,String message) {if(!value)throw new AssertionError("Geometry ultimate: "+message);checks++;}
}
