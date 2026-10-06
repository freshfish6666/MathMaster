"""Create Seven's sound set with Blender's bundled Audaspace engine.

Run:
  blender --background --factory-startup --python tools/audio/create_seven_audio.py

Eight's existing sounds are used read-only as the creature-family foundation.
The result is lowered and reinforced, then layered with synthesized diamond
chimes, sharp crystalline attacks, and a restrained ethereal echo.  On Windows,
the script also asks the built-in English voice to produce an occasional
"seven" ambient call.
"""

from __future__ import annotations

import base64
import math
from pathlib import Path
import subprocess

import aud
import numpy as np


ROOT = Path(__file__).resolve().parents[2]
EIGHT = ROOT / "src/main/resources/assets/mathmaster/sounds/entity/eight"
OUTPUT = ROOT / "src/main/resources/assets/mathmaster/sounds/entity/seven"
WORK = ROOT / "build/seven-audio-work"
RATE = 48_000


def mono_file(path: Path) -> np.ndarray:
    sound = aud.Sound.file(str(path)).rechannel(aud.CHANNELS_MONO).resample(RATE)
    return np.asarray(sound.data(), dtype=np.float32)[:, 0]


def resample_pitch(samples: np.ndarray, factor: float) -> np.ndarray:
    """Change pitch and duration together, like slowing a physical recording."""
    positions = np.arange(0.0, len(samples) - 1, factor, dtype=np.float64)
    return np.interp(positions, np.arange(len(samples)), samples).astype(np.float32)


def envelope(length: int, attack: float = 0.012, release: float = 0.16) -> np.ndarray:
    env = np.ones(length, dtype=np.float32)
    attack_samples = min(length, max(1, int(attack * RATE)))
    release_samples = min(length, max(1, int(release * RATE)))
    env[:attack_samples] = np.linspace(0.0, 1.0, attack_samples, dtype=np.float32)
    env[-release_samples:] *= np.linspace(1.0, 0.0, release_samples, dtype=np.float32)
    return env


def add_tone(buffer: np.ndarray, start: float, frequency: float, duration: float,
             gain: float, decay: float, phase: float = 0.0) -> None:
    first = int(start * RATE)
    count = min(len(buffer) - first, int(duration * RATE))
    if count <= 0:
        return
    t = np.arange(count, dtype=np.float32) / RATE
    # A slightly inharmonic pair reads as struck crystal rather than a clean bell.
    wave = np.sin(2.0 * math.pi * frequency * t + phase)
    wave += 0.37 * np.sin(2.0 * math.pi * frequency * 2.713 * t + phase * 0.7)
    wave *= np.exp(-decay * t) * gain
    buffer[first:first + count] += wave.astype(np.float32)


def diamond_layer(length: int, seed: int, intensity: float, early: bool) -> np.ndarray:
    rng = np.random.default_rng(seed)
    layer = np.zeros(length, dtype=np.float32)
    duration = length / RATE
    starts = [0.008, 0.025, 0.055] if early else [duration * 0.18, duration * 0.52]
    frequencies = [1370.0, 2110.0, 3180.0, 4720.0]
    for index, start in enumerate(starts):
        if start >= duration:
            continue
        frequency = frequencies[(seed + index) % len(frequencies)] * rng.uniform(0.94, 1.06)
        add_tone(layer, start, frequency, min(0.48, duration - start),
                 intensity * rng.uniform(0.65, 1.0), rng.uniform(7.0, 12.0), rng.uniform(0, math.tau))
    # A tiny filtered-noise-like crack gives attacks and wounds a diamond edge.
    if early:
        count = min(length, int(0.045 * RATE))
        noise = rng.normal(0.0, 1.0, count).astype(np.float32)
        noise[1:] -= noise[:-1] * 0.92
        layer[:count] += noise * np.linspace(intensity * 0.22, 0.0, count, dtype=np.float32)
    return layer


def body_layer(length: int, seed: int, intensity: float) -> np.ndarray:
    rng = np.random.default_rng(seed)
    t = np.arange(length, dtype=np.float32) / RATE
    wobble = 1.0 + 0.035 * np.sin(2.0 * math.pi * (2.1 + seed % 3) * t)
    phase = 2.0 * math.pi * np.cumsum((72.0 + seed % 17) * wobble) / RATE
    body = np.sin(phase) + 0.42 * np.sin(phase * 2.01 + 0.6)
    return (body * envelope(length, 0.018, 0.24) * intensity).astype(np.float32)


def echo(samples: np.ndarray, delay: float, feedbacks: tuple[float, ...]) -> np.ndarray:
    result = samples.copy()
    step = int(delay * RATE)
    for index, gain in enumerate(feedbacks, start=1):
        offset = step * index
        if offset < len(result):
            result[offset:] += samples[:-offset] * gain
    return result


def finish(samples: np.ndarray) -> np.ndarray:
    samples = samples * envelope(len(samples), 0.006, 0.12)
    peak = float(np.max(np.abs(samples))) if len(samples) else 1.0
    if peak > 0.92:
        samples *= 0.92 / peak
    return np.ascontiguousarray(samples[:, None], dtype=np.float32)


def render_from_eight(source_name: str, output_name: str, seed: int,
                      pitch: float, body: float, crystal: float,
                      echo_mix: float, early_crystal: bool) -> None:
    source = resample_pitch(mono_file(EIGHT / source_name), pitch)
    source *= 0.73
    mixed = source + body_layer(len(source), seed, body)
    mixed += diamond_layer(len(source), seed, crystal, early_crystal)
    mixed = echo(mixed, 0.115 + (seed % 3) * 0.018, (echo_mix, echo_mix * 0.42))
    sound = aud.Sound.buffer(finish(mixed), RATE)
    sound.write(str(OUTPUT / output_name), RATE, aud.CHANNELS_MONO,
                aud.FORMAT_S16, aud.CONTAINER_OGG, aud.CODEC_VORBIS,
                160_000, 4096)


def create_spoken_seven() -> Path | None:
    voice_wav = WORK / "seven_voice.wav"
    script = f"""
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech
$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer
$speaker.SelectVoice('Microsoft Zira Desktop')
$speaker.Rate = -2
$speaker.Volume = 100
$speaker.SetOutputToWaveFile('{str(voice_wav).replace("'", "''")}')
$speaker.Speak('seven')
$speaker.Dispose()
"""
    encoded = base64.b64encode(script.encode("utf-16-le")).decode("ascii")
    try:
        subprocess.run(
            ["powershell.exe", "-NoProfile", "-NonInteractive", "-EncodedCommand", encoded],
            check=True,
            timeout=30,
        )
    except (OSError, subprocess.CalledProcessError, subprocess.TimeoutExpired):
        return None
    return voice_wav if voice_wav.exists() else None


def render_spoken_seven() -> bool:
    voice_path = create_spoken_seven()
    if voice_path is None:
        return False
    voice = resample_pitch(mono_file(voice_path), 0.74)
    lead = int(0.075 * RATE)
    tail = int(0.55 * RATE)
    mixed = np.zeros(lead + len(voice) + tail, dtype=np.float32)
    mixed[lead:lead + len(voice)] = voice * 0.78
    mixed += body_layer(len(mixed), 707, 0.16)
    mixed += diamond_layer(len(mixed), 77, 0.13, False)
    mixed = echo(mixed, 0.17, (0.20, 0.085, 0.035))
    sound = aud.Sound.buffer(finish(mixed), RATE)
    sound.write(str(OUTPUT / "ambient_seven.ogg"), RATE, aud.CHANNELS_MONO,
                aud.FORMAT_S16, aud.CONTAINER_OGG, aud.CODEC_VORBIS,
                160_000, 4096)
    return True


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    WORK.mkdir(parents=True, exist_ok=True)
    jobs = [
        # source, output, seed, pitch, low body, crystal, echo, early crystal
        ("ambient_1.ogg", "ambient_1.ogg", 71, 0.79, 0.14, 0.10, 0.12, False),
        ("ambient_2.ogg", "ambient_2.ogg", 72, 0.76, 0.16, 0.11, 0.14, False),
        ("ambient_3.ogg", "ambient_3.ogg", 73, 0.81, 0.13, 0.12, 0.13, False),
        ("alert_1.ogg", "alert_1.ogg", 74, 0.75, 0.20, 0.18, 0.10, True),
        ("alert_2.ogg", "alert_2.ogg", 75, 0.78, 0.19, 0.20, 0.11, True),
        ("attack_1.ogg", "attack_1.ogg", 76, 0.77, 0.24, 0.24, 0.07, True),
        ("attack_2.ogg", "attack_2.ogg", 77, 0.73, 0.26, 0.26, 0.07, True),
        ("hurt_1.ogg", "hurt_1.ogg", 78, 0.79, 0.19, 0.21, 0.09, True),
        ("hurt_2.ogg", "hurt_2.ogg", 79, 0.76, 0.20, 0.19, 0.10, True),
        ("hurt_3.ogg", "hurt_3.ogg", 80, 0.81, 0.18, 0.23, 0.09, True),
        ("death_1.ogg", "death_1.ogg", 81, 0.68, 0.27, 0.15, 0.18, False),
    ]
    for job in jobs:
        render_from_eight(*job)
    spoken = render_spoken_seven()
    print(f"Generated {len(jobs)} Seven sounds in {OUTPUT}")
    print(f"Spoken ambient generated: {spoken}")


if __name__ == "__main__":
    main()
