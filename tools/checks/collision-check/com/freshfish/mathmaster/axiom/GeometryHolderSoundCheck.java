package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.entity.GeometryHolderRangedAttacks;
import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import java.util.ArrayList;
import java.util.function.Consumer;

public final class GeometryHolderSoundCheck {
    private static int checks;
    public static int run(ServerLevel level) throws Exception {
        checks=0;
        var sounds=new ArrayList<String>();
        Consumer<PlayLevelSoundEvent> listen=event -> {
            if(event.getSound()!=null && event.getSound().value().getLocation().getPath().startsWith("entity.geometry_holder.")) {
                if(event.getSource()!=SoundSource.HOSTILE || event.getOriginalVolume()<=0 || event.getOriginalVolume()>2 || event.getOriginalPitch()<=0)
                    throw new AssertionError("Geometry sound category/volume/pitch");
                sounds.add(event.getSound().value().getLocation().getPath());
            }
        };
        NeoForge.EVENT_BUS.addListener(listen);
        var boss=ModEntities.GEOMETRY_HOLDER.get().create(level);boss.setPos(220,245,220);
        try(var fixture=new SkillCheckPlayer(level,"GeometrySoundCheck")) {
            fixture.player.setPos(230,245,220);
            for(var field:ModSounds.class.getFields()) if(field.getName().startsWith("GEOMETRY_")) {
                var holder=(net.neoforged.neoforge.registries.DeferredHolder<?,?>)field.get(null);
                require(BuiltInRegistries.SOUND_EVENT.getKey((net.minecraft.sounds.SoundEvent)holder.get())!=null,"sound registration "+field.getName());
            }
            var state=GeometryHolderEntity.class.getDeclaredMethod("setAttack",int.class,int.class);state.setAccessible(true);
            int[] phases={1,2,4,5,6,7,8,9,10};
            String[] cues={"halo_charge","prison_charge","prison_cut","beam_charge","beam_fire","bomb_charge","bomb_throw","cross_charge","cross_sweep"};
            for(int i=0;i<phases.length;i++) {
                state.invoke(boss,phases[i],40);String cue="entity.geometry_holder."+cues[i];
                boolean charge=com.freshfish.mathmaster.entity.GeometryHolderAudio.chargeSound(phases[i])!=null;
                require(count(sounds,cue)==(charge ? 0 : 1),"charge stays client-owned; release starts once "+cues[i]);
                if(charge) require(com.freshfish.mathmaster.entity.GeometryHolderAudio.chargeSound(phases[i]).getLocation().getPath().equals(cue),"client charge mapping "+cues[i]);
                int total=sounds.size();state.invoke(boss,phases[i],39);
                require(total==sounds.size(),"remaining-tick update does not restart "+cues[i]);
            }
            boss.setTarget(fixture.player);state.invoke(boss,GeometryHolderEntity.PRISON,31);
            int before=count(sounds,"entity.geometry_holder.prison_charge");tick(boss);
            require(count(sounds,"entity.geometry_holder.prison_charge")==before,"full charge has no repeated server pulse");
            state.invoke(boss,com.freshfish.mathmaster.entity.GeometryHolderRangedAttacks.BEAM_ACTIVE,21);
            before=count(sounds,"entity.geometry_holder.beam_sustain");tick(boss);
            require(count(sounds,"entity.geometry_holder.beam_sustain")==before+1,"short active beam texture keeps ten-tick accents");
            state.invoke(boss,GeometryHolderEntity.RECOVERY,10);before=sounds.size();tick(boss);
            require(sounds.size()==before,"recovery does not continue skill sounds");
            var release=GeometryHolderEntity.class.getDeclaredMethod("releaseAttack",int.class);release.setAccessible(true);release.invoke(boss,GeometryHolderEntity.HALO);
            require(count(sounds,"entity.geometry_holder.halo_release")==1,"halo impact sounds once");
            var ambient=GeometryHolderEntity.class.getDeclaredMethod("getAmbientSound");ambient.setAccessible(true);
            require(ambient.invoke(boss)==null,"ambient suppressed during skill recovery");
            state.invoke(boss,0,0);require(ambient.invoke(boss)==ModSounds.GEOMETRY_AMBIENT.get() && boss.getAmbientSoundInterval()==160,"idle uses custom infrequent ambient");
            boss.setHealth(7);tick(boss);tick(boss);
            require(count(sounds,"entity.geometry_holder.phase_two")==1 && boss.getVoicePitch()==.9F,"second-stage cue fires once with lower voice pitch");
            CompoundTag tag=new CompoundTag();boss.save(tag);var restored=ModEntities.GEOMETRY_HOLDER.get().create(level);restored.load(tag);tick(restored);restored.discard();
            require(count(sounds,"entity.geometry_holder.phase_two")==1,"reload does not replay stage cue");
            boss.invulnerableTime=0;boss.hurt(boss.damageSources().playerAttack(fixture.player),1);
            require(count(sounds,"entity.geometry_holder.hurt")==1,"ordinary accepted hit uses custom hurt sound");
            boss.hurt(boss.damageSources().playerAttack(fixture.player),1);
            require(count(sounds,"entity.geometry_holder.hurt")==1,"invulnerable repeated hit adds no hurt sound");
            boss.invulnerableTime=0;boss.hurt(boss.damageSources().playerAttack(fixture.player),100);
            require(count(sounds,"entity.geometry_holder.death")==1,"actual death uses one custom death sound");
        } finally {boss.discard();NeoForge.EVENT_BUS.unregister(listen);}
        return checks;
    }
    private static void tick(GeometryHolderEntity boss){boss.tickCount++;boss.tick();}
    private static int count(ArrayList<String> sounds,String name){return (int)sounds.stream().filter(name::equals).count();}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError("Geometry sound: "+message);checks++;}
}
