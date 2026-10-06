"""Create Six's extremely low, violent flesh-creature sounds with Blender Audaspace.

Run: blender --background --factory-startup --python tools/audio/create_six_audio.py
Existing Eight and Nine recordings are read-only source material.
"""

from pathlib import Path
import math

import aud
import numpy as np


ROOT = Path(__file__).resolve().parents[2]
SOUNDS = ROOT / "src/main/resources/assets/mathmaster/sounds/entity"
OUTPUT = SOUNDS / "six"
RATE = 48_000


def read_mono(path: Path) -> np.ndarray:
    sound = aud.Sound.file(str(path)).rechannel(aud.CHANNELS_MONO).resample(RATE)
    return np.asarray(sound.data(), dtype=np.float32)[:, 0]


def lower(samples: np.ndarray, factor: float) -> np.ndarray:
    positions = np.arange(0.0, len(samples) - 1, factor, dtype=np.float64)
    return np.interp(positions, np.arange(len(samples)), samples).astype(np.float32)


def envelope(length: int, attack: float, release: float) -> np.ndarray:
    env = np.ones(length, dtype=np.float32)
    a = min(length, max(1, int(attack * RATE)))
    r = min(length, max(1, int(release * RATE)))
    env[:a] = np.linspace(0.0, 1.0, a, dtype=np.float32)
    env[-r:] *= np.linspace(1.0, 0.0, r, dtype=np.float32)
    return env


def visceral_layers(length: int, seed: int, force: float, attack: bool) -> np.ndarray:
    rng = np.random.default_rng(seed)
    t = np.arange(length, dtype=np.float32) / RATE
    frequency = 38.0 + seed % 13
    phase = 2.0 * math.pi * np.cumsum(
        frequency * (1.0 + 0.09 * np.sin(2.0 * math.pi * 3.1 * t))
    ) / RATE
    rumble = np.sin(phase) + 0.48 * np.sin(phase * 2.02 + 0.7)
    rumble *= envelope(length, 0.025 if not attack else 0.004, 0.3) * force

    noise = rng.normal(0.0, 1.0, length).astype(np.float32)
    # Smoothed breath/gore noise, followed by a short tearing edge on attacks.
    kernel = np.ones(48, dtype=np.float32) / 48.0
    breath = np.convolve(noise, kernel, mode="same") * force * 0.42
    if attack:
        count = min(length, int(0.085 * RATE))
        tear = noise[:count].copy()
        tear[1:] -= tear[:-1] * 0.88
        breath[:count] += tear * np.linspace(force * 0.72, 0.0, count, dtype=np.float32)
    return (rumble + breath).astype(np.float32)


def echo(samples: np.ndarray, delay: float, gains: tuple[float, ...]) -> np.ndarray:
    result = samples.copy()
    step = int(delay * RATE)
    for index, gain in enumerate(gains, start=1):
        offset = step * index
        if offset < len(result):
            result[offset:] += samples[:-offset] * gain
    return result


def render(source: str, output: str, seed: int, pitch: float,
           force: float, aggression: float, attack: bool) -> None:
    base = lower(read_mono(SOUNDS / source), pitch)
    base = np.tanh(base * aggression) / max(1.0, math.tanh(aggression))
    mixed = base * 0.64 + visceral_layers(len(base), seed, force, attack)
    # Tight room reflections keep it massive without giving it Seven's airy character.
    mixed = echo(mixed, 0.073 + (seed % 3) * 0.009, (0.12, 0.045))
    mixed *= envelope(len(mixed), 0.004, 0.16)
    mixed *= 0.90 if attack else 0.74
    peak = float(np.max(np.abs(mixed)))
    if peak > 0.91:
        mixed *= 0.91 / peak
    sound = aud.Sound.buffer(np.ascontiguousarray(mixed[:, None], dtype=np.float32), RATE)
    sound.write(str(OUTPUT / output), RATE, aud.CHANNELS_MONO,
                aud.FORMAT_S16, aud.CONTAINER_OGG, aud.CODEC_VORBIS,
                160_000, 4096)


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    jobs = [
        ("eight/ambient_1.ogg", "ambient_1.ogg", 61, 0.59, 0.24, 2.1, False),
        ("nine/ambient_2.ogg", "ambient_2.ogg", 62, 0.55, 0.27, 2.4, False),
        ("eight/ambient_3.ogg", "ambient_3.ogg", 63, 0.61, 0.23, 2.7, False),
        ("nine/attack_1.ogg", "attack_1.ogg", 64, 0.58, 0.36, 3.0, True),
        ("eight/attack_2.ogg", "attack_2.ogg", 65, 0.54, 0.39, 3.3, True),
        ("eight/hurt_1.ogg", "hurt_1.ogg", 66, 0.60, 0.31, 2.8, True),
        ("nine/hurt_2.ogg", "hurt_2.ogg", 67, 0.56, 0.34, 3.1, True),
        ("eight/hurt_3.ogg", "hurt_3.ogg", 68, 0.62, 0.30, 3.0, True),
        ("nine/death_1.ogg", "death_1.ogg", 69, 0.49, 0.40, 2.7, False),
    ]
    for job in jobs:
        render(*job)
    print(f"Generated {len(jobs)} Six sounds in {OUTPUT}")


if __name__ == "__main__":
    main()
