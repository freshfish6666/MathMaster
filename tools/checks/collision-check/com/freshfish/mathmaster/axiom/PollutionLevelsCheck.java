package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.block.DigitallyCorruptedBlock;
import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.effect.DigitalPollutionMobEffect;
import com.freshfish.mathmaster.event.DigitallyCorruptedBlockEffectHandler;
import com.freshfish.mathmaster.init.*;
import com.freshfish.mathmaster.pollution.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.List;

public final class PollutionLevelsCheck {
    private static int checks;
    @SuppressWarnings("unchecked")
    public static int run(ServerLevel overworld) throws Exception {
        checks = 0;
        int[] intervals = {20,18,16,15,14,10}, amounts = {1,2,3,4,5,15};
        for (int level=1;level<=6;level++) {
            require(DigitalPollutionMobEffect.pollutionIntervalTicks(160,level)==intervals[level-1]
                    && DigitalPollutionMobEffect.pollutionAmount(level)==amounts[level-1],"level profile "+level);
        }
        require(DigitalPollutionMobEffect.pollutionIntervalTicks(200,256)==10
                && DigitalPollutionMobEffect.pollutionAmount(256)==15,"upper tier cap");
        require(DigitalPollutionMobEffect.pollutionIntervalTicks(40,2)==180
                && DigitalPollutionMobEffect.pollutionIntervalTicks(80,3)==80
                && DigitalPollutionMobEffect.pollutionIntervalTicks(120,5)==28,"IQ scaling");
        require(DimensionPollutionLevels.isValidEntry("example:dimension=12")
                && !DimensionPollutionLevels.isValidEntry("bad id=2")
                && !DimensionPollutionLevels.isValidEntry("example:dimension=-1")
                && !DimensionPollutionLevels.isValidEntry("example:dimension=999999999999"),"config validation");
        var field = MathMasterConfig.class.getDeclaredField("DIMENSION_POLLUTION_LEVELS");
        field.setAccessible(true);
        var config = (ModConfigSpec.ConfigValue<List<? extends String>>)field.get(null);
        var original = config.get();
        try {
            config.set(List.of("minecraft:overworld=1","minecraft:the_nether=2","minecraft:the_end=3"));
            require(DimensionPollutionLevels.get(overworld)==1,"overworld level");
            require(DimensionPollutionLevels.get(ResourceLocation.parse("minecraft:the_nether"))==2,"nether level");
            require(DimensionPollutionLevels.get(ResourceLocation.parse("minecraft:the_end"))==3,"end level");
            require(DimensionPollutionLevels.get(ResourceLocation.parse("example:unlisted"))==0,"unlisted default");
            config.set(List.of());
            require(DimensionPollutionLevels.get(overworld)==0,"empty config/cache invalidation");
            config.set(List.of("minecraft:overworld=1","minecraft:the_nether=2","minecraft:the_end=3"));
            for (var key : List.of(Level.OVERWORLD,Level.NETHER,Level.END)) {
                ServerLevel world = overworld.getServer().getLevel(key);
                var cow = EntityType.COW.create(world);
                world.setBlockAndUpdate(new BlockPos(30,200,30),ModBlocks.DIGITALLY_CORRUPTED_BLOCK.get().defaultBlockState());
                cow.setPos(30.5,201,30.5);
                var handler = new DigitallyCorruptedBlockEffectHandler();
                handler.onEntityTick(new EntityTickEvent.Post(cow));
                require(cow.getEffect(ModMobEffects.DIGITAL_POLLUTION).getAmplifier()+1==DimensionPollutionLevels.get(world),"dimension block "+key);
                var data = cow.getData(ModAttachments.DIGITAL_POLLUTION); data.reset();
                for (int i=0;i<260;i++) {
                    cow.getEffect(ModMobEffects.DIGITAL_POLLUTION).tick(cow,()->{});
                    handler.onEntityTick(new EntityTickEvent.Post(cow));
                }
                require(data.getValue()>0,"refresh/progress "+key);
                cow.addEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION,1200,0));
                require(PollutionExposure.externalEffect(cow.getEffect(ModMobEffects.DIGITAL_POLLUTION))!=null,"added potion "+key);
                for (int i=0;i<20;i++) {
                    cow.getEffect(ModMobEffects.DIGITAL_POLLUTION).tick(cow,()->{});
                    handler.onEntityTick(new EntityTickEvent.Post(cow));
                }
                MobEffectInstance saved = MobEffectInstance.load((CompoundTag)cow.getEffect(ModMobEffects.DIGITAL_POLLUTION).save());
                require(PollutionExposure.externalEffect(saved).getDuration()==1180,"hidden duration/save "+key);
                cow.forceAddEffect(saved,null); cow.setPos(32.5,201,30.5);
                handler.onEntityTick(new EntityTickEvent.Post(cow));
                var external = cow.getEffect(ModMobEffects.DIGITAL_POLLUTION);
                require(external!=null && external.getAmplifier()==0 && external.getDuration()==1180
                        && !DigitallyCorruptedBlock.isGrantedEffect(external),"leave restores potion "+key);
                cow.discard();
            }
            try (var fixture = new SkillCheckPlayer(overworld,"PollutionLevels")) {
                var player = fixture.player;
                player.getData(ModAttachments.INTELLIGENCE).setIq(160); player.setPos(34.5,241,34.5);
                var effect = ModMobEffects.DIGITAL_POLLUTION.get();
                var data = player.getData(ModAttachments.DIGITAL_POLLUTION);
                for (int level=1;level<=7;level++) {
                    data.reset(); player.removeEffect(ModMobEffects.DIGITAL_POLLUTION);
                    player.addEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION,1200,level-1));
                    for (int tick=0;tick<DigitalPollutionMobEffect.pollutionIntervalTicks(160,level)+1;tick++) effect.applyEffectTick(player,level-1);
                    require(data.getValue()==DigitalPollutionMobEffect.pollutionAmount(level),"real increment "+level);
                }
                for (int level : new int[]{5,6}) {
                    data.reset(); player.removeEffect(ModMobEffects.DIGITAL_POLLUTION);
                    player.addEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION,1200,level-1));
                    int ticks=0;
                    while(data.getValue()<90 && ticks<300) { effect.applyEffectTick(player,level-1); ticks++; }
                    require(level==5 ? ticks>=245 && ticks<=255 : ticks>=60 && ticks<=61,"near-lethal timing "+level);
                }
                player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.LINGXU_HELMET.get()));
                player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ModItems.LINGXU_CHESTPLATE.get()));
                player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModItems.LINGXU_LEGGINGS.get()));
                player.setItemSlot(EquipmentSlot.FEET,new ItemStack(ModItems.LINGXU_BOOTS.get()));
                require(PollutionExposure.protection(player)==1,"full suit");
                overworld.setBlockAndUpdate(new BlockPos(34,240,34),ModBlocks.DIGITALLY_CORRUPTED_BLOCK.get().defaultBlockState());
                data.reset(); player.removeEffect(ModMobEffects.DIGITAL_POLLUTION);
                var handler = new DigitallyCorruptedBlockEffectHandler(); handler.onEntityTick(new EntityTickEvent.Post(player));
                require(!player.hasEffect(ModMobEffects.DIGITAL_POLLUTION),"overworld protection zero");
                config.set(List.of("minecraft:overworld=3")); handler.onEntityTick(new EntityTickEvent.Post(player));
                require(player.getEffect(ModMobEffects.DIGITAL_POLLUTION).getAmplifier()==1,"environment protection once");
                require(PollutionExposure.effectiveLevel(player,player.getEffect(ModMobEffects.DIGITAL_POLLUTION))==2,"no double reduction");
                player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY); handler.onEntityTick(new EntityTickEvent.Post(player));
                require(player.getEffect(ModMobEffects.DIGITAL_POLLUTION).getAmplifier()==2,"removal immediate");
                player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.LINGXU_HELMET.get()));
                handler.onEntityTick(new EntityTickEvent.Post(player));
                require(player.getEffect(ModMobEffects.DIGITAL_POLLUTION).getAmplifier()==1,"wearing immediate");
                data.reset(); DigitalPollutionManager.add(player,7);
                require(data.getValue()==7,"direct pollution unchanged");
                config.set(List.of()); handler.onEntityTick(new EntityTickEvent.Post(player));
                require(!player.hasEffect(ModMobEffects.DIGITAL_POLLUTION),"dimension zero cleanup");
                player.addEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION,1200,0));
                data.reset(); for(int i=0;i<40;i++) effect.applyEffectTick(player,0);
                require(data.getValue()==0,"external level one protected");
                player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
                for(int i=0;i<21;i++) effect.applyEffectTick(player,0);
                require(data.getValue()==1,"external source dimension independent");
                data.reset(); data.tickPollutionEffect(2);
                player.addEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION,1200,4));
                require(data.tickPollutionEffect(2),"level change preserves progress");
            }
            for(int tier:new int[]{5,6}) {
                try(var lethal = new SkillCheckPlayer(overworld,"PollutionLethal"+tier)) {
                    var player=lethal.player;
                    player.getData(ModAttachments.INTELLIGENCE).setIq(160);
                    player.getData(ModAttachments.DIGITAL_POLLUTION).reset();
                    player.addEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION,1200,tier-1));
                    int ticks=0;
                    while(player.isAlive() && ticks<310) {
                        ModMobEffects.DIGITAL_POLLUTION.get().applyEffectTick(player,tier-1); ticks++;
                    }
                    require(!player.isAlive() && (tier==5 ? ticks>=280 && ticks<=281 : ticks>=70 && ticks<=71)
                            && player.getData(ModAttachments.DIGITAL_POLLUTION).getValue()==0,"actual lethal timing "+tier);
                }
            }
        } finally { config.set(original); }
        return checks;
    }
    private static void require(boolean condition,String message) {
        if(!condition) throw new AssertionError(message); checks++;
    }
}
