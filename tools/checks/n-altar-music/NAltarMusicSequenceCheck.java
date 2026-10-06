import com.freshfish.mathmaster.ritual.NAltarMusicSequence;
import java.util.ArrayList;
import java.util.List;

/** Deterministic audio-backend substitute; no Minecraft client/audio device required. */
public final class NAltarMusicSequenceCheck {
    private static int checks;
    private static final class Voice implements NAltarMusicSequence.Audio {
        final boolean outro;
        boolean alive = true;
        boolean started = true;
        double age;
        float volume;
        Voice(boolean outro) { this.outro = outro; }
        public boolean playing() { return alive; }
        public boolean started() { return started; }
        public double seconds() { return age; }
        public void advance(double seconds) { if (started) age += seconds; }
        public void volume(float value) { volume = value; }
        public void cancel() { alive = false; }
    }
    private static final class Fixture {
        final List<Voice> voices = new ArrayList<>();
        final NAltarMusicSequence sequence = new NAltarMusicSequence(outro -> {
            Voice voice = new Voice(outro);
            voices.add(voice);
            return voice;
        });
        void tick(boolean active, boolean ended, boolean inRange, double seconds) {
            sequence.tick(active, ended, inRange, seconds);
            check(voices.stream().filter(v -> v.alive).count() <= 2, "Maximum two crossfade voices");
            for (Voice voice : voices) check(voice.volume >= 0 && voice.volume <= 1, "Bounded gain");
        }
        Voice last() { return voices.getLast(); }
        void start() { tick(true, false, true, 1.5); }
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void close(float value, float expected, String message) {
        check(Math.abs(value - expected) < 0.00001, message + ": " + value);
    }
    public static void main(String[] args) {
        Fixture f = new Fixture();
        f.tick(false, false, false, 1);
        check(f.voices.isEmpty(), "No normal-mode music");
        f.tick(true, false, true, .75);
        Voice full = f.last();
        close(full.volume, .5f, "Halfway range fade in");
        f.tick(true, false, true, .75);
        close(full.volume, 1, "Complete fade in");
        for (int i = 0; i < 100; i++) f.tick(true, false, true, .05);
        check(f.voices.size() == 1, "Active/overlapping altars never layer duplicate songs");
        full.alive = false;
        f.tick(true, false, true, .05);
        check(f.voices.size() == 2 && !f.last().outro, "Full song restarts on natural completion");
        close(f.last().volume, 0, "Restart applied on next update");
        f.tick(true, false, true, .05);
        close(f.last().volume, 1, "No artificial loop delay");

        f = new Fixture(); f.start(); full = f.last();
        f.tick(false, false, false, .75);
        close(full.volume, .5f, "Leaving fades out");
        f.tick(true, false, true, .75);
        check(f.last() == full && f.voices.size() == 1, "Re-entry during fade keeps playback progress");
        close(full.volume, 1, "Re-entry fades back up");
        f.tick(false, false, false, 1.5);
        check(!f.sequence.running() && !full.alive, "Leaving cancels after fading, without an outro");
        f.tick(true, false, true, .05);
        check(f.voices.size() == 2, "Return after silence begins a fresh song");

        f = new Fixture(); f.start(); full = f.last();
        f.tick(false, true, true, .05);
        Voice outro = f.last();
        check(outro.outro && full.alive, "Destroyed/reward-completed altar crossfades to outro");
        f.tick(false, true, true, .95);
        close(full.volume, .5f, "Old track halfway fade");
        close(outro.volume, .5f, "Outro halfway fade");
        f.tick(false, false, true, 1);
        check(!full.alive && outro.alive, "Old track released after two seconds");
        close(outro.volume, 1, "Outro at full volume");
        outro.alive = false;
        f.tick(false, true, true, .05);
        check(!f.sequence.running(), "Outro completion stops");
        f.tick(false, true, true, 3);
        check(f.voices.size() == 2, "Ended ritual never loops outro");

        for (double age : new double[]{127.999, 128, 140, 144}) {
            f = new Fixture(); f.start(); full = f.last(); full.age = age;
            f.tick(false, true, true, 0);
            check(f.voices.size() == (age < 128 ? 2 : 1), "Exact outro boundary " + age);
            if (age >= 128) {
                full.alive = false; f.tick(false, true, true, .05);
                check(!f.sequence.running(), "Near-ending track naturally finishes without restart");
            }
        }

        f = new Fixture(); f.start(); full = f.last();
        f.tick(false, true, true, 0); outro = f.last(); outro.started = false;
        f.tick(false, true, true, 1);
        close(full.volume, 1, "Asynchronous decode doesn't fade the old song prematurely");
        close(outro.volume, 0, "Undecoded outro remains silent");
        f.tick(false, true, false, .75);
        close(full.volume, .5f, "Range fade still applies while outro is decoding");
        f.tick(false, true, true, .75);
        outro.started = true; f.tick(false, true, true, 1);
        close(full.volume, .5f, "Crossfade starts only after stream starts");
        f.tick(true, false, true, .05);
        check(!f.last().outro && f.voices.size() == 3, "New ritual replaces an ongoing outro");
        check(!full.alive, "Superseded old crossfade voice stopped");
        f.sequence.reset();
        check(f.voices.stream().noneMatch(v -> v.alive), "Disconnect/dimension/death/mute cleanup stops every voice");

        f = new Fixture(); f.start(); full = f.last();
        f.tick(false, true, false, .75);
        f.tick(false, true, false, .75);
        check(!f.sequence.running() && f.voices.stream().noneMatch(v -> v.alive), "Leaving during outro fades all voices out");

        f = new Fixture(); f.start(); full = f.last();
        f.tick(false, true, true, 0); outro = f.last(); outro.started = false; outro.alive = false;
        f.tick(false, true, true, .05);
        f.tick(false, true, true, .05);
        close(full.volume, 1, "Failed outro decoding restores existing full song");
        f.sequence.reset();
        check(!full.alive, "Fallback sound cleaned up");
        System.out.println("PASS: " + checks + " N altar music assertions");
    }
}
