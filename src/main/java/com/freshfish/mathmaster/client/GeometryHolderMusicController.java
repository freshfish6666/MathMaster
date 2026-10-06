package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.init.ModSounds;
import com.freshfish.mathmaster.ritual.GeometryHolderMusicSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;

@EventBusSubscriber(modid=MathMaster.MODID,value=Dist.CLIENT)
public final class GeometryHolderMusicController {
    private static final GeometryHolderMusicSequence sequence=new GeometryHolderMusicSequence(track -> {
        var sound=new BossMusic(track);
        Minecraft.getInstance().getSoundManager().play(sound);
        return sound;
    });
    private static ClientLevel watchedLevel;
    private static GeometryHolderEntity watchedBoss;
    private static long lastNanos;
    private GeometryHolderMusicController() {}
    public static boolean ownsMusic() { return sequence.running(); }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        long now=System.nanoTime();
        double dt=lastNanos==0 ? 0 : (now-lastNanos)/1_000_000_000.0;
        lastNanos=now;
        if(mc.level==null || mc.level!=watchedLevel || mc.player==null || !mc.player.isAlive()
                || mc.options.getSoundSourceVolume(SoundSource.MUSIC)==0 || mc.options.getSoundSourceVolume(SoundSource.MASTER)==0) {
            sequence.reset();watchedBoss=null;watchedLevel=mc.level;return;
        }
        if(mc.isPaused()) return;
        boolean inRange=watchedBoss!=null && watchedBoss.distanceToSqr(mc.player)<=48*48;
        boolean defeated=watchedBoss!=null && watchedBoss.clientDeathConfirmed() && inRange;
        if(watchedBoss==null || watchedBoss.isRemoved() || !watchedBoss.isAlive() || !inRange) {
            GeometryHolderEntity nearest=null;
            double distance=48*48;
            for(var entity:mc.level.entitiesForRendering()) if(entity instanceof GeometryHolderEntity boss && boss.isAlive() && !boss.isRemoved()) {
                double candidate=boss.distanceToSqr(mc.player);
                if(candidate<=distance) {nearest=boss;distance=candidate;}
            }
            if(nearest!=null) {watchedBoss=nearest;inRange=true;defeated=false;}
        }
        boolean active=watchedBoss!=null && !watchedBoss.isRemoved() && watchedBoss.isAlive() && inRange;
        boolean wasPlaying=sequence.running();
        sequence.tick(active ? (watchedBoss.isSecondPhase() ? 2 : 1) : 0,defeated,inRange,dt);
        if(!wasPlaying && sequence.running()) mc.getMusicManager().stopPlaying();
        if(!sequence.running() && !active) watchedBoss=null;
    }
    @SubscribeEvent public static void select(SelectMusicEvent event) { if(sequence.running()) event.setMusic(null); }
    @SubscribeEvent public static void started(PlayStreamingSourceEvent event) {
        if(event.getSound() instanceof BossMusic sound) sound.started=true;
    }
    private static final class BossMusic extends AbstractTickableSoundInstance implements GeometryHolderMusicSequence.Audio {
        private volatile boolean started;
        private double elapsed;
        BossMusic(int track) {
            super(switch(track) {case 1 -> ModSounds.GEOMETRY_MUSIC_ONE.get();case 2 -> ModSounds.GEOMETRY_MUSIC_TWO.get();
                case 3 -> ModSounds.GEOMETRY_OUTRO_ONE.get();case 4 -> ModSounds.GEOMETRY_OUTRO_TWO.get();default -> throw new IllegalArgumentException();},
                SoundSource.MUSIC,RandomSource.create());
            relative=true;attenuation=Attenuation.NONE;volume=0;looping=false;
        }
        @Override public boolean canStartSilent() {return true;}
        @Override public void tick() {}
        @Override public boolean playing() {return !isStopped() && Minecraft.getInstance().getSoundManager().isActive(this);}
        @Override public boolean started() {return started;}
        @Override public double seconds() {return elapsed;}
        @Override public void advance(double seconds) {if(started) elapsed+=seconds;}
        @Override public void volume(float value) {volume=value;}
        @Override public void cancel() {stop();Minecraft.getInstance().getSoundManager().stop(this);}
    }
}
