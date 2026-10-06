package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.entity.GeometryConstructEntity;
import com.freshfish.mathmaster.init.ModCreativeTabs;
import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import java.util.ArrayList;

/** Actual egg, laser timing/geometry, ordinary damage, movement and death loot. */
public final class GeometryConstructCheck {
    private static int checks;
    public static int run(ServerLevel level) throws Exception {
        checks = 0;
        var previousDifficulty = level.getDifficulty();
        BlockPos floor = new BlockPos(310, 240, 310);
        var mobs = new ArrayList<GeometryConstructEntity>();
        var drops = new ArrayList<ItemEntity>();
        java.util.function.Consumer<EntityJoinLevelEvent> observer = event -> {
            if (event.getLevel() != level) return;
            if (event.getEntity() instanceof GeometryConstructEntity mob) mobs.add(mob);
            if (event.getEntity() instanceof ItemEntity item && item.getItem().is(ModItems.GEOMETRY_CORE.get())) drops.add(item);
        };
        NeoForge.EVENT_BUS.addListener(observer);
        try (var fixture = new SkillCheckPlayer(level, "ConstructCheck")) {
            var player = fixture.player;
            for (int x=-3;x<=22;x++) for (int z=-3;z<=3;z++) for(int y=0;y<6;y++)
                level.setBlockAndUpdate(floor.offset(x,y,z), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            var invulnerability = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            invulnerability.setAccessible(true); invulnerability.setInt(player, 0);
            player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            player.setPos(318.5, 241, 310.5);
            var stack = new ItemStack(ModItems.GEOMETRY_CONSTRUCT_SPAWN_EGG.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var egg = (SpawnEggItem) stack.getItem();
            require(egg.getType(stack) == ModEntities.GEOMETRY_CONSTRUCT.get(), "egg type");
            require(egg.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0), Direction.UP, floor, false))).consumesAction(), "actual spawn egg");
            require(mobs.size() == 1 && stack.getCount() == 1, "one entity and one egg consumed");
            var mob = mobs.getFirst();
            require(mob.getHealth() == 24 && mob.getArmorValue() == 4 && mob.getBbWidth() == 1.375F && mob.isNoGravity(), "attributes/size");
            var tab = ModCreativeTabs.MATHMASTER_TAB.get();
            tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), false, level.registryAccess()));
            require(tab.getSearchTabDisplayItems().stream().anyMatch(s -> s.is(ModItems.GEOMETRY_CONSTRUCT_SPAWN_EGG.get())), "creative search egg");
            ready(mob, player);
            tick(mob);
            require(mob.attackPhase() == GeometryConstructEntity.CHARGING && mob.attackTicks() == 20, "one second warning");
            Vec3 lockedPosition = mob.position();
            for (int i=0;i<19;i++) tick(mob);
            require(player.getHealth() == 20 && mob.attackTicks() == 1 && mob.position().equals(lockedPosition), "warning harmless, cast stationary");
            tick(mob);
            require(mob.attackPhase() == GeometryConstructEntity.FIRING && mob.attackTicks() == 20 && player.getHealth() == 16, "first four-point hit at release");
            Vec3 lockedDirection = mob.beamDirection();
            player.setPos(318.5, 241, 313.5);
            for(int i=0;i<10;i++) tick(mob);
            require(mob.beamDirection().equals(lockedDirection) && player.getHealth() == 16, "sideways dodge and locked direction");
            player.setPos(318.5,241,310.5); player.invulnerableTime=0; tick(mob);
            require(player.getHealth() == 12, "re-entering active ray takes ordinary damage");
            for(int i=0;i<8;i++) tick(mob);
            require(mob.attackPhase() == GeometryConstructEntity.FIRING && mob.attackTicks()==1, "active duration includes last tick");
            tick(mob);
            require(mob.attackPhase()==GeometryConstructEntity.IDLE && mob.beamLength()==0, "ray ends after one second");
            require(mob.getBoundingBoxForCulling().getXsize()<2, "finished beam leaves no large culling box");
            player.setHealth(20);player.invulnerableTime=0;
            ready(mob,player);tick(mob);
            for(int i=0;i<20;i++) tick(mob);
            for(int i=0;i<19;i++) {if(player.invulnerableTime>0)player.invulnerableTime--;tick(mob);}
            require(player.getHealth()==12, "vanilla immunity allows two hits in twenty-tick beam");
            Vec3 rayEnd=mob.beamStart().add(mob.beamDirection().scale(mob.beamLength()));
            require(mob.getBoundingBoxForCulling().contains(rayEnd), "beam included in culling volume");

            // Walls built after firing starts must immediately shorten both damage and visual length.
            ready(mob,player);tick(mob);for(int i=0;i<20;i++)tick(mob);
            require(mob.attackPhase()==GeometryConstructEntity.FIRING && mob.attackTicks()==20,"active ray established before wall test");
            player.setHealth(20);player.invulnerableTime=0;
            for(int y=1;y<=3;y++)level.setBlockAndUpdate(floor.offset(4,y,0),Blocks.STONE.defaultBlockState());
            tick(mob);
            require(player.getHealth()==20 && mob.beamLength()>0 && mob.beamLength()<4
                    && mob.attackPhase()==GeometryConstructEntity.FIRING, "wall truncates an already active beam");
            for(int y=1;y<=3;y++)level.setBlockAndUpdate(floor.offset(4,y,0),Blocks.AIR.defaultBlockState());
            // Nonzero pitch must survive vanilla look control.
            player.setPos(318.5,244,310.5);ready(mob,player);tick(mob);
            require(mob.getXRot()<-10 && mob.beamDirection().y>.2, "vertical aiming and visible model pitch");
            var saved=mob.saveWithoutId(new CompoundTag());
            var restored=ModEntities.GEOMETRY_CONSTRUCT.get().create(level);restored.load(saved);
            require(restored.attackPhase()==0 && restored.beamLength()==0 && restored.isNoGravity()
                    && restored.getHealth()==24 && restored.getArmorValue()==4, "save reload clears cast, preserves mob");
            restored.discard();

            // Ordinary events and difficulty/armor mitigation, not direct health subtraction.
            var damage = GeometryConstructEntity.class.getDeclaredMethod("updateBeam",boolean.class);damage.setAccessible(true);
            player.setPos(318.5,241,310.5);ready(mob,player);tick(mob);
            for(int i=0;i<20;i++)tick(mob);
            var armor=player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            for(var difficulty:new Difficulty[]{Difficulty.EASY,Difficulty.NORMAL,Difficulty.HARD}) {
                level.getServer().setDifficulty(difficulty,true);
                float raw=difficulty==Difficulty.EASY?3:difficulty==Difficulty.HARD?6:4;
                for(int protection:new int[]{0,20}) {
                    armor.setBaseValue(protection);player.setHealth(20);player.invulnerableTime=0;
                    damage.invoke(mob,true);
                    float expected=raw*(1-Math.max(protection*.2F,protection-raw/2)/25);
                    require(Math.abs(player.getHealth()-(20-expected))<.0001,"difficulty/armor "+difficulty+"/"+protection);
                }
            }
            armor.setBaseValue(0);level.getServer().setDifficulty(Difficulty.NORMAL,true);
            java.util.function.Consumer<LivingIncomingDamageEvent> cancelDamage=event -> {
                if(event.getSource().getEntity()==mob)event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(cancelDamage);
            try {player.setHealth(20);player.invulnerableTime=0;damage.invoke(mob,true);
                require(player.getHealth()==20,"damage cancellation respected");}
            finally {NeoForge.EVENT_BUS.unregister(cancelDamage);}

            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);tick(mob);
            require(mob.getTarget()==null && mob.attackPhase()==0,"creative player cancels targeting and cast");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

            player.setPos(325.5,241,310.5);ready(mob,player);
            var cooldown=GeometryConstructEntity.class.getDeclaredField("cooldown");cooldown.setAccessible(true);cooldown.setInt(mob,1000);
            Vec3 before=mob.position();for(int i=0;i<40;i++)tick(mob);
            require(mob.getX()>before.x+2 && mob.attackPhase()==0,"smooth ranged pursuit while idle");
            require(Math.abs(mob.getY()-241)<.5,"combat height does not drift upward");
            for(int y=1;y<5;y++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(floor.offset(5,y,z),Blocks.STONE.defaultBlockState());
            mob.setPos(314.1,241,310.5);mob.setDeltaMovement(Vec3.ZERO);
            for(int i=0;i<60;i++)tick(mob);
            require(level.noCollision(mob,mob.getBoundingBox().deflate(.001)) && mob.getX()<314.4,"body respects solid wall");
            for(int y=1;y<5;y++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(floor.offset(5,y,z),Blocks.AIR.defaultBlockState());
            player.setPos(350,241,310);tick(mob);
            require(mob.getTarget()==null && mob.attackPhase()==0,"out of twenty-four blocks loses target");

            // Real death path: guaranteed one core, one settlement, cancellation respected.
            mob.setPos(310.5,241,310.5);player.setPos(312.5,241,310.5);
            var lootingSword=new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            lootingSword.enchant(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),3);
            player.setItemInHand(InteractionHand.MAIN_HAND,lootingSword);
            java.util.function.Consumer<LivingDeathEvent> cancelDeath=event -> {if(event.getEntity()==mob)event.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(cancelDeath);
            try {mob.hurt(level.damageSources().playerAttack(player),100);
                require(drops.isEmpty(),"cancelled death gives no loot");}
            finally {NeoForge.EVENT_BUS.unregister(cancelDeath);}
            mob.setHealth(24);mob.invulnerableTime=0;
            mob.hurt(level.damageSources().playerAttack(player),100);
            require(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"actual Looting III kill always drops exactly one core");
            mob.hurt(level.damageSources().playerAttack(player),100);
            require(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"dead mob cannot duplicate drop");
        } finally {
            NeoForge.EVENT_BUS.unregister(observer);
            mobs.forEach(GeometryConstructEntity::discard);drops.forEach(ItemEntity::discard);
            level.getServer().setDifficulty(previousDifficulty,true);
            for(int y=1;y<5;y++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(floor.offset(5,y,z),Blocks.AIR.defaultBlockState());
        }
        return checks;
    }
    private static void ready(GeometryConstructEntity mob,net.minecraft.server.level.ServerPlayer player) throws Exception {
        var phase=GeometryConstructEntity.class.getDeclaredMethod("phase",int.class,int.class);phase.setAccessible(true);phase.invoke(mob,0,0);
        var cooldown=GeometryConstructEntity.class.getDeclaredField("cooldown");cooldown.setAccessible(true);cooldown.setInt(mob,0);
        mob.setTarget(player);mob.setDeltaMovement(Vec3.ZERO);
    }
    private static void tick(GeometryConstructEntity mob){mob.tickCount++;mob.tick();}
    private static void require(boolean value,String message){if(!value)throw new AssertionError("Geometry construct: "+message);checks++;}
}
