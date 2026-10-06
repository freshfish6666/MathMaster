package com.freshfish.mathmaster.ritual;

import java.util.ArrayList;
import java.util.List;

/** Audio-engine-independent boss music: combat loops, phase crossfade and confirmed defeat ending. */
public final class GeometryHolderMusicSequence {
    public interface Audio {
        boolean playing();
        boolean started();
        double seconds();
        void advance(double seconds);
        void volume(float value);
        void cancel();
    }
    @FunctionalInterface public interface Player { Audio play(int track); }
    public static final double PHASE_FADE_SECONDS = 2, LOOP_FADE_SECONDS = .75, DEFEAT_FADE_SECONDS = 1.2;
    public static double duration(int track) { return switch(track) {case 1 -> 80; case 2 -> 83.2; case 3 -> 8.68; case 4 -> 6; default -> 0;}; }
    private static final class Voice {
        final Audio audio;
        final int track;
        float weight, initial;
        double age;
        Voice(Audio audio, int track) { this.audio=audio; this.track=track; }
    }
    private final Player player;
    private final List<Voice> retiring = new ArrayList<>();
    private Voice primary;
    private float gain;
    private double blend, fadeSeconds, retry;
    private int requested;
    private boolean ending;
    private double endingAge;

    public GeometryHolderMusicSequence(Player player) { this.player=player; }

    /** stage=1/2 while an alive boss is in range; defeated requires the real death packet, not unloading. */
    public void tick(int stage, boolean defeated, boolean inRange, double seconds) {
        double elapsed=Double.isFinite(seconds) ? Math.max(0,seconds) : 0;
        double dt=Math.min(.25,elapsed);
        // Playback position follows real audio time even when a client frame stalls.
        advance(primary,elapsed);
        for(var voice:retiring) advance(voice,elapsed);
        retry=Math.max(0,retry-dt);
        if(stage>0) {
            ending=false;
            endingAge=0;
            if(requested!=stage) { requested=stage; retry=0; }
            if(primary==null && retry==0 || primary!=null && primary.track!=stage && retry==0)
                switchTrack(stage,PHASE_FADE_SECONDS);
        } else if(defeated && !ending && primary!=null) {
            ending=true;
            endingAge=0;
            requested=(requested==2 ? 4 : 3);
            retry=0;
            switchTrack(requested,DEFEAT_FADE_SECONDS);
        }
        if(ending && retry==0 && (primary==null || primary.track!=requested))
            switchTrack(requested,DEFEAT_FADE_SECONDS);
        if(ending) {
            endingAge+=dt;
            // A broken ending resource must not indefinitely suppress ordinary music.
            if(endingAge>duration(requested)+5) {reset();return;}
        }
        float target=stage>0 || ending && inRange ? 1 : 0;
        float step=(float)(dt/1.5);
        gain=target>gain ? Math.min(target,gain+step) : Math.max(target,gain-step);

        if(primary!=null && (primary.audio.started() && !primary.audio.playing() || !primary.audio.started() && primary.age>=5)) {
            boolean completed=primary.audio.started();
            primary.audio.cancel(); primary=null;
            if(ending && completed) { reset(); return; }
            // Stream allocation failure retains the previous audible track until a bounded retry.
            if(!retiring.isEmpty()) {
                primary=retiring.remove(retiring.size()-1);
                primary.weight=primary.initial;
                blend=fadeSeconds;
            }
            retry=completed ? 0 : 2;
        }
        if(primary!=null) {
            if(primary.audio.started()) blend=Math.min(fadeSeconds,blend+dt);
            double t=fadeSeconds==0 ? 1 : blend/fadeSeconds;
            double eased=t*t*(3-2*t);
            primary.weight=(float)Math.sin(eased*Math.PI/2);
            primary.audio.volume(.8F*gain*primary.weight);
            for(var voice:retiring) {
                voice.weight=voice.initial*(float)Math.cos(eased*Math.PI/2);
                voice.audio.volume(.8F*gain*voice.weight);
            }
            if(t>=1) {
                retiring.forEach(v -> v.audio.cancel()); retiring.clear();
            }
            if(stage>0 && !ending && retry==0 && primary.audio.started()
                    && primary.track==stage && primary.audio.seconds()>=duration(stage)-LOOP_FADE_SECONDS)
                switchTrack(stage,LOOP_FADE_SECONDS);
        }
        retiring.removeIf(v -> { if(v.audio.started() && !v.audio.playing() || !v.audio.started() && v.age>=5) {v.audio.cancel();return true;} return false; });
        if(gain==0 && stage==0) reset();
    }
    private static void advance(Voice voice,double dt) { if(voice!=null) { voice.age+=dt;voice.audio.advance(dt); } }
    private void switchTrack(int track,double fade) {
        // Preserve both audible weights when death interrupts a phase transition.
        for(var voice:retiring) voice.initial=voice.weight;
        if(primary!=null) {primary.initial=primary.weight;retiring.add(primary);}
        primary=new Voice(player.play(track),track);
        blend=0; fadeSeconds=fade;
    }
    public boolean running() { return primary!=null || !retiring.isEmpty(); }
    public void reset() {
        if(primary!=null) primary.audio.cancel();
        retiring.forEach(v -> v.audio.cancel());retiring.clear();primary=null;
        gain=0; blend=0; retry=0;requested=0;ending=false;endingAge=0;
    }
}
