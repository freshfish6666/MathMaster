package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.block.entity.NAltarBlockEntity;
import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.ritual.NAltarRitualHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

/** Real entities, effects, confirmed deaths, persistent progress and resource-backed chest loot. */
public final class NAltarRitualCheck {
    private static int checks;
    public static int run(ServerLevel level) {
        checks=0;
        long oldTime=level.getGameTime();
        var pos=new BlockPos(3,240,3);
        var second=pos.offset(4,0,0);
        var entities=new ArrayList<Mob>();
        var handler=new NAltarRitualHandler();
        try {
            var altar=place(level,pos,true);
            var chunk=level.getChunkAt(pos);
            chunk.removeBlockEntity(pos);
            require(chunk.getBlockEntity(pos)==null,"legacy statue has no controller");
            handler.onChunkLoad(new net.neoforged.neoforge.event.level.ChunkEvent.Load(chunk,false));
            handler.onLevelTick(new LevelTickEvent.Post(()->true,level));
            altar=(NAltarBlockEntity)chunk.getBlockEntity(pos);
            require(altar!=null,"legacy statue controller restored at Post tick");
            altar.onLoad();
            require(MathMasterConfig.nAltarOfferings()==20,"user-confirmed threshold");
            long phase=Math.floorMod(-pos.asLong(),20);
            ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(phase);
            var zombie=EntityType.ZOMBIE.create(level);
            var nine=ModEntities.NINE.get().create(level);
            var cow=EntityType.COW.create(level);
            var outside=EntityType.ZOMBIE.create(level);
            for(var mob:new Mob[]{zombie,nine,cow,outside}) {
                entities.add(mob);mob.setNoAi(true);
                mob.setPos(Vec3.atCenterOf(pos).add(mob==outside?10.01:2,0,0));
                level.addFreshEntity(mob);
            }
            tick(level,altar);
            for(var mob:new Mob[]{zombie,nine}) {
                require(mob.hasEffect(MobEffects.MOVEMENT_SPEED)&&mob.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()==0,"speed I on target "+mob.getType());
                require(mob.hasEffect(MobEffects.DAMAGE_BOOST)&&mob.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier()==0,"strength I on target "+mob.getType());
                double expected=mob==nine?4.5:3;
                require(mob.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()==expected,"base attack unmodified");
                require(mob.getAttributeValue(Attributes.ATTACK_DAMAGE)==expected+3,"vanilla strength modifier applies");
            }
            require(!cow.hasEffect(MobEffects.MOVEMENT_SPEED)&&!outside.hasEffect(MobEffects.DAMAGE_BOOST),"passive and outside excluded");
            nine.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,600,4));
            tick(level,altar);
            require(nine.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier()==4&&nine.getEffect(MobEffects.DAMAGE_BOOST).getDuration()==600,"strong external strength preserved");
            zombie.setPos(Vec3.atCenterOf(pos).add(10,0,0));zombie.removeAllEffects();tick(level,altar);
            require(zombie.hasEffect(MobEffects.DAMAGE_BOOST),"sphere includes exact ten-block boundary");
            require(NAltarRitualHandler.isTarget(ModEntities.SEVEN.get().create(level))
                    && NAltarRitualHandler.isTarget(ModEntities.EIGHT.get().create(level))
                    && NAltarRitualHandler.isTarget(ModEntities.SIX.get().create(level)),"digital creatures included by tag");
            require(!NAltarRitualHandler.isTarget(cow),"passive death excluded");
            cow.kill();handler.onLevelTick(new LevelTickEvent.Post(()->true,level));
            require(altar.offerings()==0,"cow death does not count");
            zombie.kill();
            require(altar.offerings()==0,"death waits for confirmation");
            handler.onLevelTick(new LevelTickEvent.Post(()->true,level));
            require(altar.offerings()==1,"real monster death counted");
            altar.acceptSacrifice(zombie.getUUID());
            require(altar.offerings()==1,"duplicate UUID not counted");
            tick(level,altar);
            require(level.getBlockState(pos).getValue(NAltarBlock.SACRIFICE_GLOW)==15,"pulse starts full brightness");
            for(int i=0;i<40;i++) { ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(phase+i+1);tick(level,altar); }
            require(level.getBlockState(pos).getValue(NAltarBlock.SACRIFICE_GLOW)==0,"pulse fades fully after two seconds");
            var data=altar.saveWithoutMetadata(level.registryAccess());
            var restored=new NAltarBlockEntity(pos,level.getBlockState(pos));restored.loadWithComponents(data,level.registryAccess());
            level.setBlockEntity(restored);restored.onLoad();tick(level,restored);altar=restored;
            require(altar.offerings()==1,"progress and round survive save/load");
            altar.acceptSacrifice(zombie.getUUID());require(altar.offerings()==1,"saved UUID dedup retained");
            var overlap=place(level,second,true);tick(level,overlap);
            var victim=EntityType.ZOMBIE.create(level);entities.add(victim);victim.setPos(Vec3.atCenterOf(second));victim.kill();
            handler.onLevelTick(new LevelTickEvent.Post(()->true,level));
            require(overlap.offerings()==1&&altar.offerings()==1,"overlapping altars choose nearest exactly once");
            var cancelled=EntityType.ZOMBIE.create(level);entities.add(cancelled);cancelled.setPos(Vec3.atCenterOf(pos));
            Consumer<LivingDeathEvent> cancel=event->{if(event.getEntity()==cancelled) event.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(cancel);
            try { cancelled.kill();handler.onLevelTick(new LevelTickEvent.Post(()->true,level));require(altar.offerings()==1,"cancelled death not counted"); }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }
            for(int i=1;i<19;i++) altar.acceptSacrifice(UUID.randomUUID());
            require(altar.offerings()==19&&level.getBlockState(pos.below()).is(ModBlocks.LINGXU_BLOCK.get()),"nineteen sacrifices do not complete ritual");
            altar.acceptSacrifice(UUID.randomUUID());
            require(level.getBlockState(pos.below()).is(Blocks.CHEST),"twentieth sacrifice consumes lingxu into chest");
            require(level.getBlockState(pos).is(ModBlocks.N_ALTAR.get())&&level.getBlockState(pos.above()).is(ModBlocks.N_ALTAR.get()),"chest conversion does not destroy unsupported statue");
            require(!level.getBlockState(pos).getValue(NAltarBlock.BLOOD_SACRIFICE),"reward ends blood mode");
            var chest=(ChestBlockEntity)level.getBlockEntity(pos.below());
            require(NAltarBlockEntity.REWARD_TABLE.equals(chest.getLootTable()),"independent N loot reference");
            long seed=chest.getLootTableSeed();altar.acceptSacrifice(UUID.randomUUID());tick(level,altar);
            require(level.getBlockEntity(pos.below())==chest&&chest.getLootTableSeed()==seed,"completion cannot recreate or reroll chest");
            chest.unpackLootTable(null);
            int items=0, maps=0, ingots=0;
            for(int i=0;i<chest.getContainerSize();i++) {
                var stack=chest.getItem(i);if(stack.isEmpty()) continue;items++;
                require(stack.is(ModItems.LINGXU_NUGGET.get())||stack.is(ModItems.LINGXU_INGOT.get())
                        ||stack.is(ModItems.PRIME_CORE.get())||stack.is(net.minecraft.world.item.Items.ENDER_PEARL)
                        ||stack.is(ModItems.GRADE_2_MATH_STUDY_NOTE.get())||stack.is(ModItems.JUNIOR_HIGH_MATH_STUDY_NOTE.get())
                        ||stack.is(net.minecraft.world.item.Items.MAP)||stack.is(net.minecraft.world.item.Items.FILLED_MAP)
                        ||stack.is(net.minecraft.world.item.Items.GOLDEN_APPLE)||stack.is(net.minecraft.world.item.Items.DIAMOND)
                        ||stack.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE)||stack.is(ModItems.PRIME_NUGGET.get())
                        ||stack.is(ModBlocks.LINGXU_BLOCK.get().asItem())||stack.is(ModItems.LINGXU_SWORD.get())
                        ||stack.is(ModItems.LINGXU_AXE.get())||stack.is(ModItems.LINGXU_CHESTPLATE.get()),"N chest allowed pool");
                if(stack.is(ModItems.LINGXU_INGOT.get())) ingots+=stack.getCount();
                if(stack.is(net.minecraft.world.item.Items.MAP)||stack.is(net.minecraft.world.item.Items.FILLED_MAP)) {
                    maps+=stack.getCount();
                    var name=stack.get(net.minecraft.core.component.DataComponents.ITEM_NAME);
                    require(name!=null&&name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated
                            &&translated.getKey().equals("filled_map.mathmaster.geometry_sanctuary"),"guaranteed sanctuary map name");
                }
                if(stack.is(ModItems.LINGXU_SWORD.get())||stack.is(ModItems.LINGXU_AXE.get())||stack.is(ModItems.LINGXU_CHESTPLATE.get()))
                    require(stack.isEnchanted(),"blood reward equipment is enchanted");
            }
            require(items>0,"actual chest opens with loot");
            require(maps==1&&ingots>=2&&ingots<=3,"actual ritual reward has map and ingot guarantees");
            var opened=chest.saveWithFullMetadata(level.registryAccess()).getList("Items",10).copy();
            chest.unpackLootTable(null);
            require(opened.equals(chest.saveWithFullMetadata(level.registryAccess()).getList("Items",10)),"opened reward does not reroll");
            level.setBlockAndUpdate(second.below(),Blocks.AIR.defaultBlockState());tick(level,overlap);
            require(overlap.offerings()==0&&!level.getBlockState(second).getValue(NAltarBlock.BLOOD_SACRIFICE),"remove base resets round but keeps statue");
            overlap.setRemoved();level.setBlockAndUpdate(second,Blocks.AIR.defaultBlockState());
            overlap.acceptSacrifice(UUID.randomUUID());require(level.getBlockState(second).isAir(),"pending death cannot revive removed altar");
            checks += NAltarTrialSpawnerCheck.run(level);
            return checks;
        } finally {
            for(var mob:entities) mob.discard();
            for(var p:new BlockPos[]{pos,second}) {
                level.setBlockAndUpdate(p.above(),Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(p.below(),Blocks.AIR.defaultBlockState());
            }
            ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(oldTime);
        }
    }
    private static NAltarBlockEntity place(ServerLevel level,BlockPos pos,boolean blood) {
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(),blood?ModBlocks.LINGXU_BLOCK.get().defaultBlockState():Blocks.AIR.defaultBlockState());
        var state=ModBlocks.N_ALTAR.get().defaultBlockState().setValue(NAltarBlock.BLOOD_SACRIFICE,blood);
        level.setBlockAndUpdate(pos,state);
        ModBlocks.N_ALTAR.get().setPlacedBy(level,pos,state,null,net.minecraft.world.item.ItemStack.EMPTY);
        var altar=(NAltarBlockEntity)level.getBlockEntity(pos);altar.onLoad();return altar;
    }
    private static void tick(ServerLevel level,NAltarBlockEntity altar) {
        NAltarBlockEntity.serverTick(level,altar.getBlockPos(),level.getBlockState(altar.getBlockPos()),altar);
    }
    private static void require(boolean ok,String message) {
        if(!ok) throw new AssertionError("N blood ritual: "+message);checks++;
    }
}
