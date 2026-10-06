package com.freshfish.mathmaster.block.entity;

import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.init.ModBlockEntities;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.ritual.NAltarRitualHandler;
import com.freshfish.mathmaster.ritual.NAltarBloodDifficulty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** The lower half owns one persistent blood-sacrifice round. */
public final class NAltarBlockEntity extends BlockEntity {
    public static final double RADIUS=10.0;
    public static final ResourceKey<LootTable> REWARD_TABLE=ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("mathmaster","n/blood_sacrifice"));
    private static final DustParticleOptions PURPLE=new DustParticleOptions(new Vector3f(.42F,.15F,1F),1.8F);
    private int offerings;
    private boolean active;
    private int pulseTicks;
    private boolean indexed;
    private final Set<UUID> counted=new HashSet<>();

    public NAltarBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.N_ALTAR.get(),pos,state); }

    public int offerings() { return offerings; }

    public boolean isBloodSacrifice() {
        return level instanceof ServerLevel && !isRemoved() && getBlockState().is(ModBlocks.N_ALTAR.get())
                && level.getBlockState(worldPosition.below()).is(ModBlocks.LINGXU_BLOCK.get());
    }

    private void synchronizeMode() {
        boolean blood=isBloodSacrifice();
        if(level instanceof ServerLevel server && getBlockState().getValue(NAltarBlock.BLOOD_SACRIFICE)!=blood) {
            server.setBlockAndUpdate(worldPosition,getBlockState().setValue(NAltarBlock.BLOOD_SACRIFICE,blood));
        }
        // Structure placement can update the lower half after the upper half's final shape pass.
        if(level instanceof ServerLevel server) {
            BlockState upper=server.getBlockState(worldPosition.above());
            if(upper.is(ModBlocks.N_ALTAR.get())
                    && upper.getValue(NAltarBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER
                    && upper.getValue(NAltarBlock.BLOOD_SACRIFICE)!=blood) {
                server.setBlockAndUpdate(worldPosition.above(),upper.setValue(NAltarBlock.BLOOD_SACRIFICE,blood));
            }
        }
        if(blood!=active) {
            active=blood;
            offerings=0;
            counted.clear();
            setChanged();
        }
        if(level instanceof ServerLevel server) {
            if(blood && !indexed) { NAltarRitualHandler.register(server,this); indexed=true; }
            else if(!blood && indexed) { NAltarRitualHandler.unregister(server,worldPosition); indexed=false; }
        }
    }

    public static void serverTick(Level level,BlockPos pos,BlockState state,NAltarBlockEntity altar) {
        if(!(level instanceof ServerLevel server)) return;
        altar.synchronizeMode();
        if(altar.active) {
            if(Math.floorMod(server.getGameTime()+pos.asLong(),20)==0) altar.refreshBuffs(server);
            if(altar.offerings>=MathMasterConfig.nAltarOfferings()) altar.finish(server);
        }
        altar.tickPulse(server);
    }

    private void refreshBuffs(ServerLevel server) {
        Vec3 center=Vec3.atCenterOf(worldPosition);
        var effects=NAltarBloodDifficulty.effectsFor(NAltarBloodDifficulty.averageIq(server,center));
        for(Mob mob:server.getEntitiesOfClass(Mob.class,new AABB(center,center).inflate(RADIUS),
                entity -> entity.isAlive() && NAltarRitualHandler.isTarget(entity)
                        && entity.position().distanceToSqr(center)<=RADIUS*RADIUS)) {
            // Vanilla merging retains stronger and longer external effects; no direct attribute writes.
            for(var effect:effects) mob.addEffect(new MobEffectInstance(effect,25,0));
        }
    }

    public void acceptSacrifice(UUID id) {
        if(isRemoved() || !(level instanceof ServerLevel) || !level.hasChunkAt(worldPosition)
                || level.getBlockEntity(worldPosition)!=this) return;
        synchronizeMode();
        if(!active || !(level instanceof ServerLevel server) || !counted.add(id)) return;
        offerings=Math.min(offerings+1,MathMasterConfig.nAltarOfferings());
        pulseTicks=40;
        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,worldPosition.getX()+.5,worldPosition.getY()+1,
                worldPosition.getZ()+.5,18,.4,.65,.4,.02);
        setChanged();
        if(offerings>=MathMasterConfig.nAltarOfferings()) finish(server);
    }

    private void finish(ServerLevel server) {
        BlockPos base=worldPosition.below();
        if(!server.getBlockState(base).is(ModBlocks.LINGXU_BLOCK.get())) return;
        if(!server.setBlockAndUpdate(base,Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING,getBlockState().getValue(NAltarBlock.FACING)))) return;
        if(server.getBlockEntity(base) instanceof ChestBlockEntity chest) {
            var namedChest=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST);
            namedChest.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    Component.translatable("container.mathmaster.n_altar_reward"));
            chest.applyComponentsFromItemStack(namedChest);
            chest.setLootTable(REWARD_TABLE,server.random.nextLong());
            chest.setChanged();
        }
        synchronizeMode();
    }

    private void tickPulse(ServerLevel server) {
        if(pulseTicks>0 && pulseTicks%5==0) {
            server.sendParticles(PURPLE,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,
                    Math.max(1,pulseTicks/3),.55,.9,.55,.015);
        }
        int glow=pulseTicks>0 ? (15*pulseTicks+39)/40 : 0;
        BlockState current=getBlockState();
        if(current.getValue(NAltarBlock.SACRIFICE_GLOW)!=glow) {
            server.setBlockAndUpdate(worldPosition,current.setValue(NAltarBlock.SACRIFICE_GLOW,glow));
        }
        if(pulseTicks>0) pulseTicks--;
    }

    @Override public void onLoad() {
        super.onLoad();
        if(level instanceof ServerLevel server && isBloodSacrifice()) {
            NAltarRitualHandler.register(server,this); indexed=true;
        }
    }
    @Override public void setRemoved() {
        if(level instanceof ServerLevel server) NAltarRitualHandler.unregister(server,worldPosition);
        indexed=false;
        super.setRemoved();
    }
    @Override public void onChunkUnloaded() {
        if(level instanceof ServerLevel server) NAltarRitualHandler.unregister(server,worldPosition);
        indexed=false;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        tag.putInt("Offerings",offerings);
        tag.putBoolean("BloodRoundActive",active);
        ListTag ids=new ListTag();
        for(UUID id:counted) ids.add(StringTag.valueOf(id.toString()));
        tag.put("CountedSacrifices",ids);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        offerings=Math.clamp(tag.getInt("Offerings"),0,4096);
        active=tag.getBoolean("BloodRoundActive");
        counted.clear();
        for(var entry:tag.getList("CountedSacrifices",8)) {
            if(counted.size()>=4096) break;
            try { counted.add(UUID.fromString(entry.getAsString())); }
            catch(IllegalArgumentException ignored) { }
        }
        pulseTicks=0;
    }
}
