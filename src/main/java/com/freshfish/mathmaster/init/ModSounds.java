package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MathMaster.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SIX_AMBIENT = register("entity.six.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIX_ATTACK = register("entity.six.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIX_HURT = register("entity.six.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIX_DEATH = register("entity.six.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> SEVEN_AMBIENT = register("entity.seven.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEVEN_ALERT = register("entity.seven.alert");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEVEN_ATTACK = register("entity.seven.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEVEN_HURT = register("entity.seven.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEVEN_DEATH = register("entity.seven.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> NINE_AMBIENT = register("entity.nine.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> NINE_ALERT = register("entity.nine.alert");
    public static final DeferredHolder<SoundEvent, SoundEvent> NINE_ATTACK = register("entity.nine.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> NINE_HURT = register("entity.nine.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> NINE_DEATH = register("entity.nine.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> EIGHT_AMBIENT = register("entity.eight.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> EIGHT_ALERT = register("entity.eight.alert");
    public static final DeferredHolder<SoundEvent, SoundEvent> EIGHT_ATTACK = register("entity.eight.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> EIGHT_HURT = register("entity.eight.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> EIGHT_DEATH = register("entity.eight.death");

    public static final DeferredHolder<SoundEvent, SoundEvent> DIGITAL_POLLUTION_WHISPER =
            register("ui.digital_pollution.whisper");

    public static final DeferredHolder<SoundEvent, SoundEvent> N_ALTAR_BLOOD_SACRIFICE = register("music.n_altar.blood_sacrifice");
    public static final DeferredHolder<SoundEvent, SoundEvent> N_ALTAR_OUTRO = register("music.n_altar.outro");

    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_AMBIENT = register("entity.geometry_holder.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_HURT = register("entity.geometry_holder.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_DEATH = register("entity.geometry_holder.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_ALERT = register("entity.geometry_holder.alert");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_PHASE_TWO = register("entity.geometry_holder.phase_two");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_HALO_CHARGE = register("entity.geometry_holder.halo_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_HALO_RELEASE = register("entity.geometry_holder.halo_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_PRISON_CHARGE = register("entity.geometry_holder.prison_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_PRISON_CUT = register("entity.geometry_holder.prison_cut");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_BEAM_CHARGE = register("entity.geometry_holder.beam_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_BEAM_FIRE = register("entity.geometry_holder.beam_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_BEAM_SUSTAIN = register("entity.geometry_holder.beam_sustain");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_BOMB_THROW = register("entity.geometry_holder.bomb_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_BOMB_EXPLODE = register("entity.geometry_holder.bomb_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_CROSS_CHARGE = register("entity.geometry_holder.cross_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_CROSS_SWEEP = register("entity.geometry_holder.cross_sweep");

    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_BOMB_CHARGE = register("entity.geometry_holder.bomb_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_MUSIC_ONE = register("music.geometry_holder.phase_one");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_MUSIC_TWO = register("music.geometry_holder.phase_two");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_OUTRO_ONE = register("music.geometry_holder.outro_one");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEOMETRY_OUTRO_TWO = register("music.geometry_holder.outro_two");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, path);
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
