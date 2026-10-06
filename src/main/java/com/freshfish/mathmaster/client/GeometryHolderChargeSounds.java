package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.GeometryHolderAudio;
import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/** One finite charge per observed phase change; uses existing entity synchronization only. */
@EventBusSubscriber(modid = MathMaster.MODID, value = Dist.CLIENT)
public final class GeometryHolderChargeSounds {
    private static ClientLevel watched;
    private record Channel(UUID id, boolean ranged) {}
    private static final HashMap<Channel,Integer> phases = new HashMap<>();
    private static final HashMap<Channel,Charge> charges = new HashMap<>();
    private GeometryHolderChargeSounds() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (watched != mc.level) {
            charges.values().forEach(sound -> mc.getSoundManager().stop(sound));
            charges.clear(); phases.clear(); watched = mc.level;
        }
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        var seen = new HashSet<Channel>();
        for (var entity : mc.level.entitiesForRendering()) if (entity instanceof GeometryHolderEntity boss) {
            for(boolean ranged:new boolean[]{false,true}) {
                var id=new Channel(boss.getUUID(),ranged);seen.add(id);
                int phase=ranged ? boss.rangedAttacks().phase() : boss.nearPhase();
                Integer old=phases.put(id,phase);
                if(old==null || old==phase) continue;
                Charge previous=charges.remove(id);
                if(previous!=null) previous.fadeOut=true;
                SoundEvent sound=GeometryHolderAudio.chargeSound(phase);
                if(sound!=null && boss.isAlive() && !boss.isSilent()) {
                    Charge charge=new Charge(boss,phase,sound,ranged);
                    charges.put(id,charge);mc.getSoundManager().play(charge);
                }
            }
        }
        phases.keySet().removeIf(id -> !seen.contains(id));
        charges.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            entry.getValue().fadeOut = true; return true;
        });
    }

    private static final class Charge extends AbstractTickableSoundInstance {
        private final GeometryHolderEntity boss;
        private final int phase;
        private final boolean ranged;
        private boolean fadeOut;
        private int fadeTicks;
        private Charge(GeometryHolderEntity boss,int phase,SoundEvent sound,boolean ranged) {
            super(sound,SoundSource.HOSTILE,SoundInstance.createUnseededRandom());
            this.boss=boss;this.phase=phase;this.ranged=ranged;volume=GeometryHolderAudio.CHARGE_VOLUME;pitch=1;looping=false;
            locate();
        }
        private void locate() { var pos=boss.castingCenter(); x=pos.x;y=pos.y;z=pos.z; }
        @Override public boolean canPlaySound() { return !boss.isSilent(); }
        @Override public void tick() {
            locate();
            if (boss.isRemoved() || !boss.isAlive() || boss.isSilent() || (ranged ? boss.rangedAttacks().phase() : boss.nearPhase())!=phase
                    || Minecraft.getInstance().level!=boss.level() || Minecraft.getInstance().player==null) fadeOut=true;
            if (fadeOut) {
                volume=GeometryHolderAudio.CHARGE_VOLUME * Math.max(0,1-++fadeTicks/4F);
                if (fadeTicks>=4) stop();
            }
        }
    }
}
