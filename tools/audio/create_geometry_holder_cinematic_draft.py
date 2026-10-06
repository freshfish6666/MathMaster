"""Three cinematic sound-design studies; writes previews only, never game assets.

Requires NumPy and FFmpeg. Original noise/resonance layers; no other mob recordings.
"""
from pathlib import Path
import json
import subprocess
import wave
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "dev-assets/geometry-holder/audio/cinematic-draft"
RATE = 48000
RNG = np.random.default_rng(13002026)


def noise(seconds, low, high, tilt=0):
    n = round(seconds * RATE)
    f = np.fft.rfftfreq(n, 1 / RATE)
    spectrum = np.fft.rfft(RNG.normal(size=n))
    shape = np.minimum(1, (f / max(low, 1)) ** 4) / (1 + (f / high) ** 8)
    shape *= np.maximum(f, 20) ** tilt
    shape[f < 20] = 0
    signal = np.fft.irfft(spectrum * shape, n)
    return signal / max(np.std(signal), 1e-8)


def fade(samples, attack=.012, release=.1):
    n = len(samples)
    a, r = min(n, round(attack * RATE)), min(n, round(release * RATE))
    envelope = np.ones(n)
    if a:
        envelope[:a] = np.sin(np.linspace(0, np.pi / 2, a)) ** 2
    if r:
        envelope[-r:] = np.sin(np.linspace(np.pi / 2, 0, r)) ** 2
    return samples * envelope


def crystal(seconds, brightness=1):
    n = round(seconds * RATE)
    t = np.arange(n) / RATE
    signal = np.zeros(n)
    # Inharmonic struck material: transient resonances rather than a repeating synth chord.
    for frequency, decay in zip([183, 397, 719, 1147, 1861, 2963], [.65, .52, .41, .31, .24, .15]):
        phase = 2 * np.pi * frequency * brightness * t + .08 * np.sin(t * 31)
        signal += np.sin(phase) * np.exp(-t / decay) / (frequency / 183) ** .65
    return fade(signal, .002, .12)


def insert(target, layer, start, gain=1):
    i = round(start * RATE)
    n = min(len(layer), len(target) - i)
    if n > 0:
        target[i:i+n] += layer[:n] * gain


def impact(seconds, strength=1):
    n = round(seconds * RATE)
    t = np.arange(n) / RATE
    low = noise(seconds, 25, 130, -.5) * np.exp(-t / .32)
    body = noise(seconds, 130, 1800, -.35) * np.exp(-t / .12)
    debris = np.zeros(n)
    for start in [.018, .055, .092, .18, .28, .42]:
        shard = fade(noise(.16, 750, 7000, -.3), .001, .12)
        insert(debris, shard, start, .12 * np.exp(-start * 4))
    return fade((low * .75 + body * .28 + debris) * strength, .003, .15)


def charge(seconds, darkness=1):
    n = round(seconds * RATE)
    t = np.arange(n) / RATE
    u = t / seconds
    rumble = noise(seconds, 30, 150, -.45)
    stone = noise(seconds, 180, 1800, -.65)
    air = noise(seconds, 900, 6400, -.35)
    # A single physical swell, with irregular turbulence and a rising high-frequency edge.
    envelope = u ** 1.65
    layered = .33 * rumble * darkness * (.4 + .6 * u) + (.18 * stone + .18 * air * u) * envelope
    return fade(layered, .15, .02)


def fft_convolve(a, b):
    n = len(a) + len(b) - 1
    size = 1 << (n - 1).bit_length()
    return np.fft.irfft(np.fft.rfft(a, size) * np.fft.rfft(b, size), size)[:n]


def room(dry):
    channels = []
    for side in range(2):
        duration = 1.8
        t = np.arange(round(duration * RATE)) / RATE
        ir = noise(duration, 220, 5200, -.45) * np.exp(-t / .38) * .0018
        ir[:round(.029 * RATE)] = 0
        for delay, gain in [(.036, .10), (.079, .075), (.131, .05), (.213, .024)]:
            ir[round((delay + side * .006) * RATE)] += gain
        wet = fft_convolve(dry, ir)
        result = np.zeros(len(wet))
        result[:len(dry)] = dry
        result += wet
        channels.append(fade(result, .008, .25))
    return np.stack(channels, axis=1)


def write(name, dry):
    stereo = room(np.tanh(dry * 1.15) / 1.15)
    peak = float(np.max(np.abs(stereo)))
    stereo *= .82 / max(peak, .82)
    pcm = np.rint(stereo * 32767).astype("<i2")
    path = OUT / (name + ".wav")
    with wave.open(str(path), "wb") as handle:
        handle.setnchannels(2)
        handle.setsampwidth(2)
        handle.setframerate(RATE)
        handle.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(path),
                    "-c:a", "libmp3lame", "-q:a", "2", str(path.with_suffix(".mp3"))], check=True)
    return {"name": name, "seconds": len(stereo) / RATE,
            "peak": float(np.max(np.abs(stereo))), "rms": float(np.sqrt(np.mean(stereo ** 2)))}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    manifest = []
    halo = np.zeros(round(2.0 * RATE))
    insert(halo, charge(.8), 0, .7)
    insert(halo, impact(1.2), .8, .95)
    insert(halo, crystal(1.0, .72), .8, .20)
    manifest.append(write("01_halo_shockwave", halo))
    beam = np.zeros(round(5.2 * RATE))
    insert(beam, charge(2.0), 0, .8)
    insert(beam, impact(.8, .8), 2.0, .75)
    t = np.arange(round(2.5 * RATE)) / RATE
    energy = noise(2.5, 70, 950, -.35) * .2 + noise(2.5, 1400, 7500, -.45) * .075
    energy *= .8 + .2 * np.sin(t * 9 + np.sin(t * 3))
    insert(beam, fade(energy, .04, .12), 2.0)
    manifest.append(write("02_charged_beam", beam))
    phase = np.zeros(round(3.1 * RATE))
    insert(phase, charge(1.3, 1.5), 0, .85)
    insert(phase, impact(1.8), 1.15, .8)
    insert(phase, crystal(1.6, .38), 1.2, .22)
    insert(phase, fade(noise(1.9, 32, 270, -.6), .2, .8), 1.1, .12)
    manifest.append(write("03_phase_two_awakening", phase))
    (OUT / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(json.dumps(manifest))


if __name__ == "__main__":
    main()
