package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import java.util.ArrayList;
import java.util.function.Consumer;

/** Timed attacks against actual world entities; no client or user save involved. */
public final class GeometryHolderAttackCheck {
    private static int checks;
    public static int run(ServerLevel level) throws Exception {
        checks = 0;
        var entities = new ArrayList<LivingEntity>();
        try (var fixture = new SkillCheckPlayer(level, "GeometryAttackCheck")) {
            var player = fixture.player;
            for (int x=12;x<=15;x++) for(int z=12;z<=15;z++) level.getChunk(x,z);
            var boss = ModEntities.GEOMETRY_HOLDER.get().create(level);
            boss.setPos(220,245,220); level.addFreshEntity(boss); entities.add(boss);
            player.setPos(224.875,245,220); boss.setTarget(player); ready(boss);
            tick(boss);
            require(boss.attackPhase()==GeometryHolderEntity.HALO && boss.attackTicks()==16,
                    "four-block surface boundary starts halo with sixteen ticks");
            var inside = EntityType.ZOMBIE.create(level); inside.setNoAi(true);
            inside.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
            inside.setPos(221,245,220); level.addFreshEntity(inside); entities.add(inside);
            var outside = EntityType.ZOMBIE.create(level); outside.setNoAi(true);
            outside.setPos(225,245,220); level.addFreshEntity(outside); entities.add(outside);
            float hp=inside.getHealth(); Vec3 locked=boss.position(); float yaw=boss.getYRot();
            player.setPos(227,245,220);
            for(int i=0;i<15;i++) tick(boss);
            require(inside.getHealth()==hp && boss.attackPhase()==GeometryHolderEntity.HALO,
                    "halo does not hit before 0.8 second or switch when target moves away");
            require(boss.position().equals(locked) && boss.getYRot()==yaw, "casting locks position and facing");
            tick(boss);
            require(inside.getHealth()==hp-12 && outside.getHealth()==20, "halo deals twelve only within radius three");
            require(inside.getDeltaMovement().horizontalDistanceSqr()>0 && boss.attackPhase()==GeometryHolderEntity.RECOVERY,
                    "halo pushes outward and enters recovery");
            require(boss.getHealth()==200, "area attack excludes the caster");
            for(int i=0;i<12;i++) tick(boss);
            require(inside.getHealth()==hp-12, "halo damage settles once");
            player.setPos(222,245,220); ready(boss,GeometryHolderEntity.PRISON); tick(boss);
            require(boss.attackPhase()==GeometryHolderEntity.PRISON && boss.attackTicks()==40,
                    "controlled near cast selects to two-second prison");
            var box=boss.prisonBounds();
            require(box.getXsize()==7 && box.getYsize()==7 && box.getZsize()==7, "prison is exactly seven cubed");
            Vec3 center=box.getCenter(); inside.setPos(center.x,center.y,center.z);
            outside.setPos(box.maxX+1,center.y,center.z); inside.invulnerableTime=0;
            hp=inside.getHealth(); locked=boss.position(); yaw=boss.getYRot();
            for(int i=0;i<10;i++) tick(boss);
            require(boss.prisonExpansion(0)==1, "cube reaches seven-block scale after initial expansion");
            for(int i=10;i<39;i++) tick(boss);
            require(inside.getHealth()==hp && boss.attackTicks()==1, "prison telegraph does no premature damage");
            require(boss.position().equals(locked) && boss.getYRot()==yaw && boss.prisonBounds().equals(box),
                    "prison volume stays fixed and escapable throughout charge");
            var spawnProtection = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnProtection.setAccessible(true); spawnProtection.setInt(player,0);
            player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            player.setPos(center.x+2,center.y,center.z); player.setHealth(20); player.invulnerableTime=0;
            tick(boss);
            require(boss.attackPhase()==GeometryHolderEntity.PRISON_ACTIVE && boss.attackTicks()==30 && player.getHealth()==14,
                    "after two-second charge player takes first hit and thirty-tick damage window starts");
            require(inside.getHealth()==hp && outside.getHealth()==20, "continuous prison affects players only");
            require(boss.prisonExpansion(0)==1 && boss.getBoundingBoxForCulling().contains(box.getMinPosition()),
                    "expanded visual and culling stay throughout active damage");
            for(int i=0;i<29;i++) {
                if(player.invulnerableTime>0) player.invulnerableTime--;
                tick(boss);
            }
            require(player.getHealth()==2 && boss.attackPhase()==GeometryHolderEntity.PRISON_ACTIVE && boss.attackTicks()==1,
                    "normal invulnerability permits exactly three six-point hits in thirty ticks");
            player.invulnerableTime--; tick(boss);
            require(boss.attackPhase()==GeometryHolderEntity.RECOVERY && boss.attackTicks()==30,
                    "damage window ends before one-and-half-second stun");
            for(int i=0;i<29;i++) tick(boss);
            require(boss.attackPhase()==GeometryHolderEntity.RECOVERY && boss.position().equals(locked) && player.getHealth()==2,
                    "stun locks boss for thirty ticks without further damage");
            tick(boss); require(boss.attackPhase()==GeometryHolderEntity.IDLE, "stun ends without another damage pulse");
            require(!boss.isMultipartEntity(), "persistent damage adds no core collider");

            boss.setPos(220,245,220); boss.setDeltaMovement(Vec3.ZERO);
            player.setPos(224.876,245,220); boss.setTarget(player); ready(boss);
            var skip=GeometryHolderEntity.class.getDeclaredField("attackCooldown"); skip.setAccessible(true); skip.setInt(boss,1); tick(boss);
            require(boss.attackPhase()==GeometryHolderEntity.IDLE, "beyond four blocks does not start a near skill");
            boss.setPos(220,245,220); player.setPos(222,245,220); ready(boss,GeometryHolderEntity.PRISON); tick(boss);
            player.setPos(280,245,220); tick(boss);
            require(boss.attackPhase()==GeometryHolderEntity.IDLE, "lost target cancels charging safely");

            boss.setPos(220,245,220); player.setPos(222,245,220); boss.setTarget(player); ready(boss,GeometryHolderEntity.PRISON); tick(boss);
            CompoundTag tag=new CompoundTag(); boss.save(tag);
            var restored=ModEntities.GEOMETRY_HOLDER.get().create(level); restored.load(tag); entities.add(restored);
            require(restored.attackPhase()==GeometryHolderEntity.IDLE && restored.attackTicks()==0,
                    "reload clears transient cast rather than firing stale attacks");

            int[] attempts={0};
            Consumer<LivingIncomingDamageEvent> cancel=event -> {
                if(event.getSource().getEntity()==boss) {
                    if(event.getEntity()==player) attempts[0]++;
                    event.setCanceled(true);
                }
            };
            // A real unarmored player outside the body tests cancellation independently of collision pushes.
            player.setPos(boss.castingCenter().x+2,boss.castingCenter().y,boss.castingCenter().z);
            player.invulnerableTime=0; player.setHealth(20); player.setDeltaMovement(Vec3.ZERO);
            NeoForge.EVENT_BUS.addListener(cancel);
            try {
                for(int i=0;i<40;i++) tick(boss);
                require(boss.attackPhase()==GeometryHolderEntity.PRISON_ACTIVE && attempts[0]==1,
                        "active window begins with one player damage attempt");
                player.setPos(boss.prisonBounds().maxX+2,boss.castingCenter().y,boss.castingCenter().z);
                for(int i=0;i<10;i++) tick(boss);
                require(attempts[0]==1, "leaving the cube immediately stops damage attempts");
                player.setPos(boss.castingCenter().x+2,boss.castingCenter().y,boss.castingCenter().z);
                for(int i=0;i<10;i++) tick(boss);
                require(attempts[0]==11, "reentering resumes per-tick checks without bypassing cancellation");
                for(int i=0;i<10;i++) tick(boss);
            }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }
            require(player.getHealth()==20 && player.getDeltaMovement().lengthSqr()==0,
                    "cancelled sustained damage does not hurt or push");

            // Phase threshold, save compatibility, actual enlarged halo hits and prison damage geometry.
            var phased=ModEntities.GEOMETRY_HOLDER.get().create(level); phased.setPos(220,245,220);
            phased.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
            phased.setHealth(40); entities.add(phased);
            require(!phased.isSecondPhase() && phased.haloRadius()==3 && phased.prisonSize()==7,
                    "exact forty percent retains accepted first-stage ranges");
            require(phased.bodyTint()==0xFFFFFFFF && phased.glowTint()==0xFFFFFFFF,"first-stage colors unchanged");
            tick(phased);
            require(phased.phaseBlend(1)==0 && phased.bodyTint(1)==0xFFFFFFFF,"healthy boss starts with unmodified first-stage visuals");
            phased.setHealth(39);
            require(phased.isSecondPhase() && phased.haloRadius()==4 && phased.prisonSize()==8,
                    "below forty percent immediately expands second-stage attacks");
            require(phased.bodyTint()!=0xFFFFFFFF && phased.glowTint()!=0xFFFFFFFF,"second-stage body and glow use distinct red-black tints");
            tick(phased);
            require(phased.phaseBlend(0)==0 && phased.phaseBlend(1)>0 && phased.phaseBlend(1)<.01,
                    "phase change starts gently rather than jumping to final color");
            require(phased.phaseBlend(.5F)>phased.phaseBlend(0) && phased.phaseBlend(.5F)<phased.phaseBlend(1),
                    "phase visuals interpolate between ticks");
            for(int i=1;i<20;i++) tick(phased);
            require(Math.abs(phased.phaseBlend(1)-.5F)<1e-6 && phased.bodyTint(1)!=phased.bodyTint()
                    && phased.glowTint(1)!=phased.glowTint(),"one second reaches half blend for both body and glow");
            float prior=phased.phaseBlend(1);boolean monotonic=true;
            for(int i=20;i<40;i++) {tick(phased);monotonic &= phased.phaseBlend(1)>=prior;prior=phased.phaseBlend(1);}
            require(monotonic && phased.phaseBlend(1)==1 && phased.bodyTint(1)==phased.bodyTint()
                    && phased.glowTint(1)==phased.glowTint() && phased.coreChargeTint(1)==0xFFFF2030,
                    "two-second fade finishes monotonically at all final colors");
            tick(phased); require(phased.phaseBlend(0)==1 && phased.phaseBlend(1)==1,"finished transition stays stable");
            phased.setHealth(100);
            require(phased.isSecondPhase(),"entered second stage does not revert on healing");
            CompoundTag phaseTag=new CompoundTag(); phased.save(phaseTag);
            var reloaded=ModEntities.GEOMETRY_HOLDER.get().create(level);reloaded.load(phaseTag);entities.add(reloaded);
            require(reloaded.isSecondPhase(),"second stage survives save reload even above threshold");
            require(reloaded.phaseBlend(0)==1 && reloaded.glowTint(.5F)==reloaded.glowTint(),"reloaded second-stage boss does not flash or replay fade");
            phaseTag.remove("GeometryHolderSecondPhase");reloaded.load(phaseTag);
            require(!reloaded.isSecondPhase(),"old high-health save without stage key stays first stage");
            phaseTag.putFloat("Health",39);reloaded.load(phaseTag);
            require(reloaded.isSecondPhase(),"old low-health save enters second stage");
            phased.setPos(220,245,220); phased.setDeltaMovement(Vec3.ZERO);
            var phasedBox=phased.prisonBounds();
            require(phasedBox.getXsize()==8 && phasedBox.getYsize()==8 && phasedBox.getZsize()==8,
                    "second-stage prison has exactly eight-block edges");
            var setPhase=GeometryHolderEntity.class.getDeclaredMethod("setAttack",int.class,int.class);setPhase.setAccessible(true);
            setPhase.invoke(phased,GeometryHolderEntity.PRISON_ACTIVE,30);
            phasedBox=phased.prisonBounds();
            require(phased.getBoundingBoxForCulling().contains(phasedBox.getMinPosition()),"eight-block visual expands render culling");
            player.setPos(phasedBox.maxX-.05,245,phasedBox.getCenter().z);player.setHealth(20);player.invulnerableTime=0;
            var prisonDamage=GeometryHolderEntity.class.getDeclaredMethod("damagePrison");prisonDamage.setAccessible(true);prisonDamage.invoke(phased);
            require(player.getHealth()==14,"added prison shell damages players with six-point hit");
            inside.setPos(223.9,245,220);outside.setPos(224.6,245,220);inside.setHealth(20);outside.setHealth(20);inside.invulnerableTime=outside.invulnerableTime=0;
            player.setPos(230,245,220);
            var halo=GeometryHolderEntity.class.getDeclaredMethod("releaseAttack",int.class);halo.setAccessible(true);halo.invoke(phased,GeometryHolderEntity.HALO);
            require(inside.getHealth()==8 && outside.getHealth()==20,"four-block halo hits enlarged shell but excludes outside");
            require(inside.getDeltaMovement().horizontalDistanceSqr()>0,"second-stage halo retains knockback");

            // Actual player hurt verifies difficulty scaling and armor remain vanilla in both phases.
            var originalDifficulty=level.getDifficulty();
            var armor=player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            double originalArmor=armor.getBaseValue();
            try {
                for(var caster:new GeometryHolderEntity[]{boss,phased}) {
                    player.setPos(caster.castingCenter().x+2,caster.castingCenter().y,caster.castingCenter().z);
                    for(var difficulty:new net.minecraft.world.Difficulty[]{net.minecraft.world.Difficulty.EASY,
                            net.minecraft.world.Difficulty.NORMAL,net.minecraft.world.Difficulty.HARD}) {
                        level.getServer().setDifficulty(difficulty,true);
                        float scaled=difficulty==net.minecraft.world.Difficulty.EASY ? 4 : difficulty==net.minecraft.world.Difficulty.HARD ? 9 : 6;
                        for(int protection:new int[]{0,20}) {
                            armor.setBaseValue(protection);player.setHealth(20);player.invulnerableTime=0;
                            prisonDamage.invoke(caster);
                            float expected=scaled*(1-Math.max(protection*.2F,protection-scaled/2)/25);
                            require(Math.abs(player.getHealth()-(20-expected))<.0001,
                                    "prison keeps difficulty and armor mitigation in phase "+caster.isSecondPhase()+" / "+difficulty+" / "+protection);
                        }
                    }
                }
            } finally {armor.setBaseValue(originalArmor);level.getServer().setDifficulty(originalDifficulty,true);}

        } finally { entities.forEach(LivingEntity::discard); }
        return checks;
    }
    private static void ready(GeometryHolderEntity boss) throws Exception { ready(boss,GeometryHolderEntity.HALO); }
    private static void ready(GeometryHolderEntity boss,int phase) throws Exception {
        var pool=GeometryHolderEntity.class.getDeclaredField("nearSkills");pool.setAccessible(true);pool.set(boss,new com.freshfish.mathmaster.entity.GeometryHolderSkillPool(phase));
        var field=GeometryHolderEntity.class.getDeclaredField("attackCooldown");
        field.setAccessible(true); field.setInt(boss,0);
        var stagger=GeometryHolderEntity.class.getDeclaredField("lastCastStart");stagger.setAccessible(true);stagger.setInt(boss,-1000);
        var far=GeometryHolderEntity.class.getDeclaredField("rangedCooldown");far.setAccessible(true);far.setInt(boss,10000);
    }
    private static void tick(GeometryHolderEntity boss) {boss.tickCount++;boss.tick();}
    private static void require(boolean value,String message) {if(!value)throw new AssertionError("Geometry attack: "+message);checks++;}
}
