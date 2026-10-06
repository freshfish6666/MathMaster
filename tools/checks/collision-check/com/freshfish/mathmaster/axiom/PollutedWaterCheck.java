package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.block.DigitallyCorruptedBlock;
import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.event.DigitallyCorruptedBlockEffectHandler;
import com.freshfish.mathmaster.init.*;
import com.freshfish.mathmaster.pollution.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.List;

public final class PollutedWaterCheck {
    private static int checks;
    private static final BlockPos POS = new BlockPos(80,230,80);
    @SuppressWarnings("unchecked")
    public static int run(ServerLevel overworld) throws Exception {
        checks = 0;
        var field = MathMasterConfig.class.getDeclaredField("DIMENSION_POLLUTION_LEVELS");
        field.setAccessible(true);
        var config = (ModConfigSpec.ConfigValue<List<? extends String>>)field.get(null);
        var original = config.get();
        var source = ModFluids.DIGITALLY_POLLUTED_WATER.get();
        var flowing = ModFluids.FLOWING_DIGITALLY_POLLUTED_WATER.get();
        var type = ModFluids.DIGITALLY_POLLUTED_WATER_TYPE.get();
        var handler = new DigitallyCorruptedBlockEffectHandler();
        try {
            config.set(List.of("minecraft:overworld=1","minecraft:the_nether=2","minecraft:the_end=3"));
            for (var key : List.of(Level.OVERWORLD,Level.NETHER,Level.END)) {
                ServerLevel world = overworld.getServer().getLevel(key);
                prepare(world);
                var bucket = (BucketItem)ModItems.DIGITALLY_POLLUTED_WATER_BUCKET.get();
                require(bucket.emptyContents(null,world,POS,null),"place bucket "+key);
                require(world.getFluidState(POS).isSource() && source.isSame(world.getFluidState(POS).getType()),"source "+key);
                require(source.getTickDelay(world)==30 && !source.canConvertToSource(world.getFluidState(POS),world,POS),"fixed lava speed/finite source "+key);
                var cow = EntityType.COW.create(world);
                cow.setPos(80.5,230,80.5);
                handler.onEntityTick(new EntityTickEvent.Post(cow));
                require(cow.hasEffect(ModMobEffects.DIGITAL_POLLUTION)
                        && cow.getEffect(ModMobEffects.DIGITAL_POLLUTION).getAmplifier()+1==DimensionPollutionLevels.get(world),"dimension contact "+key);
                require(type.canSwim(cow) && type.canDrownIn(cow),"swim/drown "+key);
                require(!cow.isOnFire() && cow.getHealth()==cow.getMaxHealth(),"no contact damage "+key);
                var picked = ModBlocks.DIGITALLY_POLLUTED_WATER.get().pickupBlock(null,world,POS,world.getBlockState(POS));
                require(picked.is(ModItems.DIGITALLY_POLLUTED_WATER_BUCKET.get()) && world.getFluidState(POS).isEmpty(),"source collection "+key);
                handler.onEntityTick(new EntityTickEvent.Post(cow));
                require(!cow.hasEffect(ModMobEffects.DIGITAL_POLLUTION),"leave removes environment "+key);
                world.setBlockAndUpdate(POS,flowing.getFlowing(1,false).createLegacyBlock());
                cow.setPos(80.5,230.13,80.5);
                require(!PollutionExposure.touchesPollutedWater(cow),"above shallow flow "+key);
                cow.setPos(81.2,230.02,80.5);
                require(PollutionExposure.touchesPollutedWater(cow),"body edge contact "+key);
                handler.onEntityTick(new EntityTickEvent.Post(cow));
                require(cow.hasEffect(ModMobEffects.DIGITAL_POLLUTION),"flow exposure "+key);
                var notPicked = ModBlocks.DIGITALLY_POLLUTED_WATER.get().pickupBlock(null,world,POS,world.getBlockState(POS));
                require(notPicked.isEmpty() && !world.getFluidState(POS).isEmpty(),"cannot collect flowing "+key);
                world.setBlockAndUpdate(POS,source.defaultFluidState().createLegacyBlock());
                source.tick(world,POS,world.getFluidState(POS));
                require(world.getFluidState(POS.east()).getAmount()==6,"horizontal drop two "+key);
                flowing.tick(world,POS.east(),world.getFluidState(POS.east()));
                require(world.getFluidState(POS.east(2)).getAmount()==4,"second horizontal drop "+key);
                world.setBlockAndUpdate(POS.east(),flowing.getFlowing(6,false).createLegacyBlock());
                world.setBlockAndUpdate(POS.east(2),source.defaultFluidState().createLegacyBlock());
                flowing.tick(world,POS.east(),world.getFluidState(POS.east()));
                require(!world.getFluidState(POS.east()).isSource(),"two neighbors no infinite source "+key);
                // Strong environment keeps the external potion ticking underneath, then restores it.
                cow.removeEffect(ModMobEffects.DIGITAL_POLLUTION);
                cow.forceAddEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION,1200,0),null);
                cow.setPos(80.5,230,80.5);
                for (int i=0;i<20;i++) {
                    handler.onEntityTick(new EntityTickEvent.Post(cow));
                    cow.getEffect(ModMobEffects.DIGITAL_POLLUTION).tick(cow,()->{});
                }
                cow.setPos(84.5,234,80.5);
                handler.onEntityTick(new EntityTickEvent.Post(cow));
                require(cow.getEffect(ModMobEffects.DIGITAL_POLLUTION).getDuration()==1180
                        && !DigitallyCorruptedBlock.isGrantedEffect(cow.getEffect(ModMobEffects.DIGITAL_POLLUTION)),"potion duration restored "+key);
                world.setBlockAndUpdate(POS.below(),Blocks.AIR.defaultBlockState());
                source.tick(world,POS,world.getFluidState(POS));
                require(source.isSame(world.getFluidState(POS.below()).getType())
                        && world.getFluidState(POS.below()).getValue(net.minecraft.world.level.material.FlowingFluid.FALLING),"flows downward "+key);
                cow.discard();
                prepare(world);
            }
            try (var fixture = new SkillCheckPlayer(overworld,"PollutedWater")) {
                var player = fixture.player;
                prepare(overworld);
                overworld.setBlockAndUpdate(POS,source.defaultFluidState().createLegacyBlock());
                overworld.setBlockAndUpdate(POS.below(),ModBlocks.DIGITALLY_CORRUPTED_BLOCK.get().defaultBlockState());
                player.setPos(80.5,230,80.5);
                handler.onEntityTick(new EntityTickEvent.Post(player));
                require(PollutionExposure.effectiveLevel(player,player.getEffect(ModMobEffects.DIGITAL_POLLUTION))==1,"overlapping environment max");
                player.getData(ModAttachments.INTELLIGENCE).setIq(160);
                var pollution = player.getData(ModAttachments.DIGITAL_POLLUTION);
                pollution.reset();
                for(int i=0;i<21;i++) {
                    player.getEffect(ModMobEffects.DIGITAL_POLLUTION).tick(player,()->{});
                    handler.onEntityTick(new EntityTickEvent.Post(player));
                }
                require(pollution.getValue()==1,"water/block overlap settles only once");
                player.setPos(84.5,234,80.5);
                handler.onEntityTick(new EntityTickEvent.Post(player));
                require(!player.hasEffect(ModMobEffects.DIGITAL_POLLUTION) && pollution.getValue()==1,"exit preserves accumulated pollution");
                player.setPos(80.5,230,80.5);
                handler.onEntityTick(new EntityTickEvent.Post(player));
                player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.LINGXU_HELMET.get()));
                player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ModItems.LINGXU_CHESTPLATE.get()));
                player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModItems.LINGXU_LEGGINGS.get()));
                player.setItemSlot(EquipmentSlot.FEET,new ItemStack(ModItems.LINGXU_BOOTS.get()));
                handler.onEntityTick(new EntityTickEvent.Post(player));
                require(!player.hasEffect(ModMobEffects.DIGITAL_POLLUTION),"full armor level one immunity");
                player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);
                handler.onEntityTick(new EntityTickEvent.Post(player));
                require(player.hasEffect(ModMobEffects.DIGITAL_POLLUTION),"partial armor no reduction");
                player.forceAddEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION,600,4),null);
                require(PollutionExposure.effectiveLevel(player,player.getEffect(ModMobEffects.DIGITAL_POLLUTION))==5,"external stronger max");
                config.set(List.of());
                player.removeEffect(ModMobEffects.DIGITAL_POLLUTION);
                handler.onEntityTick(new EntityTickEvent.Post(player));
                require(!player.hasEffect(ModMobEffects.DIGITAL_POLLUTION),"undefined dimension zero");
                config.set(List.of("minecraft:overworld=1"));
                var stand = EntityType.ARMOR_STAND.create(overworld);
                stand.setPos(80.5,230,80.5);
                handler.onEntityTick(new EntityTickEvent.Post(stand));
                require(!stand.hasEffect(ModMobEffects.DIGITAL_POLLUTION),"non intellect excluded");
                stand.discard();
            }
            // Real LivingEntity.travel, with the engine's custom fluid height detection.
            prepare(overworld);
            for (int x=79;x<=83;x++) for (int z=79;z<=83;z++) for (int y=230;y<=234;y++)
                overworld.setBlockAndUpdate(new BlockPos(x,y,z),source.defaultFluidState().createLegacyBlock());
            var swimmer = EntityType.COW.create(overworld);
            swimmer.setPos(80.5,231,80.5);
            swimmer.setNoGravity(true);
            swimmer.updateFluidHeightAndDoFluidPushing();
            double startX=swimmer.getX();
            for(int i=0;i<15;i++) swimmer.travel(new Vec3(1,0,0));
            double slowDistance=swimmer.getX()-startX;
            require(slowDistance>0,"can move horizontally");
            for (int x=79;x<=83;x++) for (int z=79;z<=83;z++) for (int y=230;y<=234;y++)
                overworld.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState());
            swimmer.setPos(80.5,231,80.5); swimmer.setDeltaMovement(Vec3.ZERO);
            swimmer.updateFluidHeightAndDoFluidPushing();
            for(int i=0;i<15;i++) swimmer.travel(new Vec3(1,0,0));
            require(swimmer.getX()-80.5>slowDistance,"slower than ordinary water");
            for (int x=79;x<=83;x++) for (int z=79;z<=83;z++) for (int y=230;y<=234;y++)
                overworld.setBlockAndUpdate(new BlockPos(x,y,z),source.defaultFluidState().createLegacyBlock());
            swimmer.setPos(80.5,231,80.5); swimmer.setDeltaMovement(Vec3.ZERO); swimmer.setNoGravity(false);
            for(int i=0;i<80 && swimmer.getY()<235;i++) {
                swimmer.updateFluidHeightAndDoFluidPushing();
                swimmer.jumpInFluid(type);
                swimmer.travel(Vec3.ZERO);
            }
            require(swimmer.getY()>235,"can swim to surface");
            swimmer.discard();
            require(ModItems.DIGITALLY_POLLUTED_WATER_BUCKET.get().getDefaultInstance().getMaxStackSize()==1,"bucket stacks one");
            require(ModItems.DIGITALLY_POLLUTED_WATER_BUCKET.get().getCraftingRemainingItem()==Items.BUCKET,"bucket remainder");
        } finally {
            config.set(original);
            for (var key:List.of(Level.OVERWORLD,Level.NETHER,Level.END)) prepare(overworld.getServer().getLevel(key));
        }
        return checks;
    }
    private static void prepare(ServerLevel world) {
        for(int x=78;x<=85;x++) for(int z=78;z<=85;z++) {
            world.setBlockAndUpdate(new BlockPos(x,229,z),Blocks.STONE.defaultBlockState());
            for(int y=230;y<=236;y++) world.setBlockAndUpdate(new BlockPos(x,y,z),(x==78 || x==85 || z==78 || z==85 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
    }
    private static void require(boolean value,String message) {
        if(!value) throw new AssertionError("Polluted water: "+message);
        checks++;
    }
}
