package com.freshfish.mathmaster.ritual;

import com.freshfish.mathmaster.block.entity.NAltarBlockEntity;
import net.minecraft.core.BlockPos;
import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.init.ModBlocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.event.level.ChunkEvent;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Loaded altars only; deaths are confirmed after die() returns so cancelled deaths never count. */
public final class NAltarRitualHandler {
    public static final TagKey<EntityType<?>> TARGETS=TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("mathmaster","n_altar_targets"));
    private static final Map<ServerLevel,Map<BlockPos,NAltarBlockEntity>> ALTARS=new HashMap<>();
    private static final Map<ServerLevel,Map<UUID,PendingDeath>> PENDING=new HashMap<>();

    private static final Map<ServerLevel,Set<LevelChunk>> REPAIR=new HashMap<>();

    public static boolean isTarget(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.Mob && (entity instanceof Enemy || entity.getType().is(TARGETS));
    }
    public static void register(ServerLevel level,NAltarBlockEntity altar) {
        ALTARS.computeIfAbsent(level,key->new HashMap<>()).put(altar.getBlockPos(),altar);
    }
    public static void unregister(ServerLevel level,BlockPos pos) {
        var entries=ALTARS.get(level);
        if(entries!=null) { entries.remove(pos); if(entries.isEmpty()) ALTARS.remove(level); }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if(!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        // The chunk palette is safe to inspect here; level interactions wait until Post tick.
        for(var section:chunk.getSections()) {
            if(section.maybeHas(state -> state.is(ModBlocks.N_ALTAR.get()))) {
                REPAIR.computeIfAbsent(level,key->new HashSet<>()).add(chunk);
                break;
            }
        }
    }

    private static void restoreMissingControllers(ServerLevel level) {
        var chunks=REPAIR.remove(level);
        if(chunks==null) return;
        for(var chunk:chunks) {
            var chunkPos=chunk.getPos();
            if(level.getChunkSource().getChunkNow(chunkPos.x,chunkPos.z)!=chunk) {
                REPAIR.computeIfAbsent(level,key->new HashSet<>()).add(chunk);
                continue;
            }
            var sections=chunk.getSections();
            for(int sectionIndex=0;sectionIndex<sections.length;sectionIndex++) {
                var section=sections[sectionIndex];
                if(!section.maybeHas(state -> state.is(ModBlocks.N_ALTAR.get()))) continue;
                int baseY=chunk.getSectionYFromSectionIndex(sectionIndex)*16;
                for(int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
                    var state=section.getBlockState(x,y,z);
                    if(!state.is(ModBlocks.N_ALTAR.get()) || state.getValue(NAltarBlock.HALF)!=DoubleBlockHalf.LOWER) continue;
                    var pos=new BlockPos(chunkPos.getMinBlockX()+x,baseY+y,chunkPos.getMinBlockZ()+z);
                    if(chunk.getBlockEntity(pos)==null) {
                        var restored=chunk.getBlockEntity(pos,LevelChunk.EntityCreationType.IMMEDIATE);
                        if(restored!=null) restored.setChanged();
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        LivingEntity entity=event.getEntity();
        if(!(entity.level() instanceof ServerLevel level) || !isTarget(entity)) return;
        var altars=ALTARS.get(level);
        if(altars==null) return;
        NAltarBlockEntity nearest=null;
        double best=NAltarBlockEntity.RADIUS*NAltarBlockEntity.RADIUS;
        for(var altar:altars.values()) {
            if(!altar.isBloodSacrifice()) continue;
            double distance=entity.position().distanceToSqr(Vec3.atCenterOf(altar.getBlockPos()));
            if(distance<best || distance==best && (nearest==null || altar.getBlockPos().asLong()<nearest.getBlockPos().asLong())) {
                nearest=altar; best=distance;
            }
        }
        if(nearest!=null) PENDING.computeIfAbsent(level,key->new HashMap<>())
                .putIfAbsent(entity.getUUID(),new PendingDeath(entity,nearest));
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level)) return;
        restoreMissingControllers(level);
        var deaths=PENDING.remove(level);
        if(deaths==null) return;
        for(var death:deaths.values()) {
            Entity.RemovalReason reason=death.entity.getRemovalReason();
            if(!death.entity.isAlive() && (death.entity.getPose()==Pose.DYING || reason==Entity.RemovalReason.KILLED)) {
                death.altar.acceptSacrifice(death.entity.getUUID());
            }
        }
    }
    @SubscribeEvent public void onChunkUnload(ChunkEvent.Unload event) {
        if(event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            var chunks=REPAIR.get(level);
            if(chunks!=null) { chunks.remove(chunk); if(chunks.isEmpty()) REPAIR.remove(level); }
        }
    }
    @SubscribeEvent public void onUnload(LevelEvent.Unload event) {
        if(event.getLevel() instanceof ServerLevel level) { ALTARS.remove(level); PENDING.remove(level); REPAIR.remove(level); }
    }
    @SubscribeEvent public void onStop(ServerStoppedEvent event) { ALTARS.clear(); PENDING.clear(); REPAIR.clear(); }
    private record PendingDeath(LivingEntity entity,NAltarBlockEntity altar) { }
}
