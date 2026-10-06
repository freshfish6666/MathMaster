package com.freshfish.mathmaster.entity;

import com.freshfish.mathmaster.init.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Short server-owned cues; no client loops, extra packets or persistent sound state. */
public final class GeometryHolderAudio {
    public static final float VOLUME_GAIN = 2F;
    public static final float CHARGE_VOLUME = .65F * VOLUME_GAIN;
    private GeometryHolderAudio() {}
    public static SoundEvent chargeSound(int phase) {
        return switch(phase) {
            case GeometryHolderEntity.HALO -> ModSounds.GEOMETRY_HALO_CHARGE.get();
            case GeometryHolderEntity.PRISON -> ModSounds.GEOMETRY_PRISON_CHARGE.get();
            case GeometryHolderRangedAttacks.BEAM_CHARGE -> ModSounds.GEOMETRY_BEAM_CHARGE.get();
            case GeometryHolderRangedAttacks.BOMB_CHARGE -> ModSounds.GEOMETRY_BOMB_CHARGE.get();
            case GeometryHolderRangedAttacks.CROSS_CHARGE -> ModSounds.GEOMETRY_CROSS_CHARGE.get();
            default -> null;
        };
    }
    static void play(GeometryHolderEntity boss, SoundEvent sound, float volume, Vec3 position) {
        if (boss.level() instanceof ServerLevel server && sound != null)
            server.playSound(null, position.x, position.y, position.z, sound, SoundSource.HOSTILE, volume * VOLUME_GAIN, boss.getVoicePitch());
    }
    static void begin(GeometryHolderEntity boss, int phase) {
        SoundEvent sound = switch (phase) {
            case GeometryHolderEntity.PRISON_ACTIVE -> ModSounds.GEOMETRY_PRISON_CUT.get();
            case GeometryHolderRangedAttacks.BEAM_ACTIVE -> ModSounds.GEOMETRY_BEAM_FIRE.get();
            case GeometryHolderRangedAttacks.BOMB_FLIGHT -> ModSounds.GEOMETRY_BOMB_THROW.get();
            case GeometryHolderUltimate.ACTIVE -> ModSounds.GEOMETRY_BOMB_THROW.get();
            case GeometryHolderRangedAttacks.CROSS_ACTIVE -> ModSounds.GEOMETRY_CROSS_SWEEP.get();
            default -> null;
        };
        play(boss, sound, .65F, boss.castingCenter());
    }
    static void pulse(GeometryHolderEntity boss,int phase) {
        SoundEvent sound = switch (phase) {
            case GeometryHolderEntity.PRISON_ACTIVE -> ModSounds.GEOMETRY_PRISON_CUT.get();
            case GeometryHolderRangedAttacks.BEAM_ACTIVE -> ModSounds.GEOMETRY_BEAM_SUSTAIN.get();
            case GeometryHolderRangedAttacks.CROSS_ACTIVE -> ModSounds.GEOMETRY_CROSS_SWEEP.get();
            default -> null;
        };
        play(boss, sound, .38F, boss.castingCenter());
    }
}
