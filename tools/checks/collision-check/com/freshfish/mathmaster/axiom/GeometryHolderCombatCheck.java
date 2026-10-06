package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import java.util.function.Consumer;

/** Real hurt pipeline, real player attribution and controlled movement boundaries. */
public final class GeometryHolderCombatCheck {
    private static int checks;
    public static int run(ServerLevel level) throws Exception {
        checks = 0;
        try (var fixture = new SkillCheckPlayer(level, "GeometryCheck")) {
            var player = fixture.player;
            player.setPos(230, 245, 220);
            for (int x = 12; x <= 15; x++) for (int z = 12; z <= 15; z++) level.getChunk(x,z);
            var boss = ModEntities.GEOMETRY_HOLDER.get().create(level);
            boss.setPos(220, 245, 220);
            boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);
            boss.getAttribute(Attributes.ARMOR).setBaseValue(0); // Isolate the existing damage/event checks; base armor is checked separately.
            reset(boss);
            var movementCooldown=GeometryHolderEntity.class.getDeclaredField("attackCooldown");
            movementCooldown.setAccessible(true); movementCooldown.setInt(boss,10000);
            var farCooldown=GeometryHolderEntity.class.getDeclaredField("rangedCooldown");farCooldown.setAccessible(true);farCooldown.setInt(boss,10000);
            var source = boss.damageSources().playerAttack(player);
            require(boss.hurt(source, 4) && boss.getHealth() == 396, "body only applies ordinary damage");
            require(!boss.isMultipartEntity(), "single body box has no independently moving parts");
            require(boss.getType().updateInterval()==1, "position and rotation track every tick");
            reset(boss);
            int[] posts = {0};
            Consumer<LivingDamageEvent.Post> post = event -> {
                if (event.getEntity()==boss) {
                    posts[0]++;
                    require(event.getSource().getEntity()==player && event.getNewDamage()==4,
                            "ordinary damage retains player source without weak point bonus");
                }
            };
            NeoForge.EVENT_BUS.addListener(post);
            try { require(boss.hurt(source,4) && boss.getHealth()==396, "only ordinary damage is applied"); }
            finally { NeoForge.EVENT_BUS.unregister(post); }
            require(posts[0]==1 && !boss.hurt(source,4), "single damage event and ordinary invulnerability");
            reset(boss);
            Consumer<LivingIncomingDamageEvent> cancel = event -> { if(event.getEntity()==boss) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(cancel);
            try { require(!boss.hurt(source,4) && boss.getHealth()==400, "ordinary damage cancellation preserved"); }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }
            boss.setTarget(null); player.setPos(boss.getX()+32,boss.getY(),boss.getZ()); boss.tickCount=9; tick(boss);
            require(boss.getTarget()==player, "32 block boundary acquires player");
            boss.setPos(220,245,220); boss.setDeltaMovement(Vec3.ZERO);
            player.setPos(boss.getX()+48,boss.getY(),boss.getZ()); tick(boss);
            require(boss.getTarget()==player, "48 block boundary retains target");
            boss.setPos(220,245,220); boss.setDeltaMovement(Vec3.ZERO);
            player.setPos(boss.getX()+48.1,boss.getY(),boss.getZ()); tick(boss);
            require(boss.getTarget()==null, "over 48 blocks loses target");
            boss.setPos(220,245,220); boss.setDeltaMovement(Vec3.ZERO);
            player.setPos(boss.getX()+16.1,boss.getY(),boss.getZ()); boss.setTarget(player); tick(boss);
            var destination = GeometryHolderEntity.class.getDeclaredField("destination"); destination.setAccessible(true);
            require(((Vec3)destination.get(boss)).x==player.getX(), "over 16 blocks pursues player");
            boss.setPos(220,245,220); boss.setDeltaMovement(Vec3.ZERO);
            player.setPos(boss.getX()+16,boss.getY(),boss.getZ()); tick(boss);
            require(((Vec3)destination.get(boss)).x==player.getX(), "16 block boundary still approaches rather than wandering");
            for(double distance:new double[]{10,3}) {
                boss.setPos(220,245,220);boss.setDeltaMovement(Vec3.ZERO);
                player.setPos(220+distance,245,220);boss.setTarget(player);
                double initial=boss.distanceToSqr(player);
                for(int i=0;i<20;i++)tick(boss);
                require(boss.distanceToSqr(player)<initial && boss.getDeltaMovement().x>0,
                        "idle cooldown approaches player at both ranged and melee distance");
            }
            var cast=GeometryHolderEntity.class.getDeclaredMethod("setAttack",int.class,int.class);cast.setAccessible(true);
            boss.setPos(220,245,220);boss.setDeltaMovement(Vec3.ZERO);player.setPos(230,245,220);boss.setTarget(player);
            cast.invoke(boss,GeometryHolderEntity.HALO,16);Vec3 castingPosition=boss.position();
            for(int i=0;i<5;i++)tick(boss);
            require(boss.position().equals(castingPosition),"active cast remains stationary while target is nearby");
            cast.invoke(boss,GeometryHolderEntity.RECOVERY,10);
            for(int i=0;i<5;i++)tick(boss);
            require(boss.distanceToSqr(player)<100,"ordinary recovery approaches without shortening recovery timer");
            require(boss.nearTicks()==5,"movement does not change recovery duration");
            cast.invoke(boss,GeometryHolderEntity.RECOVERY,10);
            var stun=GeometryHolderEntity.class.getDeclaredField("prisonRecovering");stun.setAccessible(true);stun.setBoolean(boss,true);
            boss.setDeltaMovement(Vec3.ZERO);Vec3 stunnedPosition=boss.position();
            for(int i=0;i<5;i++)tick(boss);
            require(boss.position().equals(stunnedPosition),"prison stun still prevents movement");
            cast.invoke(boss,GeometryHolderEntity.IDLE,0);
            player.setPos(boss.getX()+30,boss.getY(),boss.getZ());
            double before=boss.distanceToSqr(player);
            for(int tick=0;tick<80;tick++) tick(boss);
            require(boss.distanceToSqr(player)<before-10, "floating chase actually closes distance");
            require(boss.yBodyRot==boss.getYRot() && boss.yHeadRot==boss.getYRot(), "body and head face player together");
            boss.setPos(220,245,220); boss.setDeltaMovement(Vec3.ZERO); boss.setYRot(90);
            player.setPos(250,245,220); boss.setTarget(player);
            for(int tick=0;tick<10;tick++) tick(boss);
            require(Math.abs(net.minecraft.util.Mth.wrapDegrees(boss.getYRot()+90))<1,
                    "turns to player behind within half a second");
            Vec3 previous = boss.position(); double lastStep = 0;
            for(int tick=0;tick<40;tick++) {
                tick(boss); double step=boss.position().distanceTo(previous);
                require(step>0.01 && step<=0.141 && (lastStep==0 || Math.abs(step-lastStep)<0.025),
                        "continuous chase has no pauses or position jumps");
                previous=boss.position(); lastStep=step;
            }
            boss.setPos(220,249,220); boss.setDeltaMovement(Vec3.ZERO); player.setPos(230,245,220);boss.setTarget(player);
            for(int tick=0;tick<180;tick++) tick(boss);
            require(Math.abs(boss.getY()-player.getY())<.1,"combat descends smoothly from above player");
            require(Math.abs(((Vec3)destination.get(boss)).y-player.getY())<1e-6,"combat waypoints use player height without vertical random walk");
            for(int tick=0;tick<400;tick++) tick(boss);
            require(Math.abs(boss.getY()-player.getY())<.1,"long combat wandering does not accumulate upward drift");
            player.setPos(230,247,220); for(int tick=0;tick<150;tick++) tick(boss);
            require(Math.abs(boss.getY()-player.getY())<.1,"combat follows player onto higher terrain");
            player.setPos(230,245,220); for(int tick=0;tick<150;tick++) tick(boss);
            require(Math.abs(boss.getY()-player.getY())<.1,"combat lowers again when player returns downhill");
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE); tick(boss);
            require(boss.getTarget()==null, "creative player is not pursued");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            CompoundTag saved=new CompoundTag(); boss.save(saved);
            require(saved.contains("GeometryHolderHome",10), "home anchor saved");
            saved.putBoolean("NoAI",true);
            var restored=ModEntities.GEOMETRY_HOLDER.get().create(level); restored.load(saved);
            require(!restored.isNoAi() && restored.isNoGravity(), "old preview save migrates to floating movement");
            CompoundTag roundTrip=new CompoundTag(); restored.save(roundTrip);
            require(saved.getCompound("GeometryHolderHome").equals(roundTrip.getCompound("GeometryHolderHome")),
                    "home anchor survives save reload");
            restored.setPos(restored.getX()+80, restored.getY(), restored.getZ());
            tick(restored); restored.save(roundTrip);
            require(Math.abs(roundTrip.getCompound("GeometryHolderHome").getDouble("x")
                    - saved.getCompound("GeometryHolderHome").getDouble("x") - 80) < 1e-6,
                    "structure placement relocates saved home before movement");
            restored.discard();
            require(!boss.causeFallDamage(20,1,boss.damageSources().fall()), "floating motion causes no fall damage");
            boss.setPos(220,245,220); boss.setDeltaMovement(Vec3.ZERO);
            player.setPos(250,245,220); boss.setTarget(player);
            var wall = new java.util.LinkedHashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for(int y=244;y<=250;y++) for(int z=218;z<=222;z++) {
                var pos=new net.minecraft.core.BlockPos(222,y,z); wall.put(pos,level.getBlockState(pos));
                level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
            try {
                for(int tick=0;tick<60;tick++) tick(boss);
                require(boss.getX()+boss.getBbWidth()/2<=222.0001, "chase cannot pass through blocking wall");
            } finally {wall.forEach(level::setBlockAndUpdate);}
            reset(boss); boss.setHealth(1);
            int[] deaths={0};
            Consumer<LivingDeathEvent> death=event -> {if(event.getEntity()==boss){deaths[0]++;require(event.getSource().getEntity()==player,"ordinary lethal hit has player source");}};
            var drops = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
            Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> loot = event -> {
                if (event.getLevel()==level && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item
                        && item.position().distanceToSqr(boss.position())<4) drops.add(item);
            };
            var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            sword.enchant(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),3);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);
            NeoForge.EVENT_BUS.addListener(death);
            NeoForge.EVENT_BUS.addListener(loot);
            try {
                require(boss.hurt(source,1) && !boss.isAlive() && boss.getKillCredit()==player,
                        "ordinary hit kills with player credit");
                require(!drops.isEmpty() && drops.stream().allMatch(item -> item.getItem().is(
                        com.freshfish.mathmaster.init.ModItems.PRIME_INGOT.get())), "boss drops only prime ingots");
                int count=drops.stream().mapToInt(item -> item.getItem().getCount()).sum();
                require(count>=2 && count<=3,"real Looting III kill drops two to three prime ingots");
                boss.hurt(source,1000);
                require(drops.stream().mapToInt(item -> item.getItem().getCount()).sum()==count,
                        "dead boss cannot duplicate loot");
            } finally {
                NeoForge.EVENT_BUS.unregister(death);
                NeoForge.EVENT_BUS.unregister(loot);
                drops.forEach(net.minecraft.world.entity.item.ItemEntity::discard);
                boss.discard();
            }
            require(deaths[0]==1, "one death event");
        }
        return checks;
    }
    private static void tick(GeometryHolderEntity boss) { boss.tickCount++; boss.tick(); }
    private static void reset(GeometryHolderEntity boss) { boss.invulnerableTime=0; boss.setHealth(400); boss.setAbsorptionAmount(0); }
    private static void require(boolean ok,String message) {if(!ok)throw new AssertionError("Geometry Holder: "+message);checks++;}
}
