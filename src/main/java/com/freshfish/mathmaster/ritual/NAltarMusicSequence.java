package com.freshfish.mathmaster.ritual;

/** Client playback decisions, independent of the audio engine so transitions can be checked. */
public final class NAltarMusicSequence {
    public static final double OUTRO_START_SECONDS = 128.0;
    public static final double CROSSFADE_SECONDS = 2.0;
    public static final double RANGE_FADE_SECONDS = 1.5;

    public interface Audio {
        boolean playing();
        boolean started();
        double seconds();
        void advance(double seconds);
        void volume(float volume);
        void cancel();
    }

    public interface Player {
        Audio play(boolean outro);
    }

    private enum Mode { IDLE, RITUAL, LEAVING, OUTRO }
    private final Player player;
    private Mode mode = Mode.IDLE;
    private Audio primary;
    private Audio retiring;
    private float gain;
    private float primaryWeight = 1;
    private float retiringWeight;
    private double crossfade;
    private double retryDelay;

    public NAltarMusicSequence(Player player) {
        this.player = player;
    }

    /** active: a blood altar in range; ended: watched altar truly changed; inRange: outro origin in range. */
    public void tick(boolean active, boolean ended, boolean inRange, double seconds) {
        if (primary != null) primary.advance(seconds);
        if (retiring != null) retiring.advance(seconds);
        retryDelay = Math.max(0, retryDelay - seconds);

        if (active) {
            if (mode == Mode.OUTRO) switchTrack(false);
            if (primary == null && retryDelay == 0) switchTrack(false);
            mode = Mode.RITUAL;
        } else if (mode == Mode.RITUAL || mode == Mode.LEAVING) {
            if (ended && primary != null) {
                if (primary.seconds() < OUTRO_START_SECONDS) switchTrack(true);
                mode = Mode.OUTRO;
            } else {
                mode = Mode.LEAVING;
            }
        }

        float target = active || (mode == Mode.OUTRO && inRange) ? 1 : 0;
        float step = (float) (seconds / RANGE_FADE_SECONDS);
        gain = target > gain ? Math.min(target, gain + step) : Math.max(target, gain - step);
        // Decoding happens asynchronously. Keep the old track until the new stream actually starts.
        if (retiring != null) {
            if (primary.started()) {
                crossfade = Math.min(CROSSFADE_SECONDS, crossfade + seconds);
                primaryWeight = (float) (crossfade / CROSSFADE_SECONDS);
            }
            retiring.volume(gain * retiringWeight * (1 - primaryWeight));
            if (crossfade >= CROSSFADE_SECONDS || !retiring.playing()) {
                retiring.cancel();
                retiring = null;
                primaryWeight = 1;
            }
        }
        if (primary != null) {
            primary.volume(gain * primaryWeight);
            if (!primary.playing()) {
                if (retiring != null && retiring.playing()) {
                    // Failed to allocate/decode the new stream: retain the old sound until retry/end.
                    primary.cancel();
                    primary = retiring;
                    retiring = null;
                    primaryWeight = 1;
                } else {
                    boolean completed = primary.started();
                    if (completed && active) {
                        primary.cancel();
                        primary = null;
                        switchTrack(false);
                    } else {
                        reset();
                        retryDelay = completed ? 0 : 2;
                    }
                }
            }
        }
        if (gain == 0 && !active) reset();
    }

    private void switchTrack(boolean outro) {
        if (retiring != null) retiring.cancel();
        retiring = primary;
        retiringWeight = primaryWeight;
        primary = player.play(outro);
        primaryWeight = retiring == null ? 1 : 0;
        crossfade = 0;
    }

    public boolean running() {
        return primary != null;
    }

    public void reset() {
        if (primary != null) primary.cancel();
        if (retiring != null) retiring.cancel();
        primary = null;
        retiring = null;
        mode = Mode.IDLE;
        gain = 0;
        primaryWeight = 1;
        retryDelay = 0;
    }
}
