"""Build multilingual eldritch pseudo-whispers with Blender Audaspace.

First run tools/audio/create_pollution_voice_sources.ps1 to create temporary Windows
TTS sources. The final mix keeps human phonemes but cuts, overlaps, reverses and
cross-pans fragments so no complete sentence remains intelligible.
"""

from pathlib import Path
import math

import aud
import numpy as np


ROOT = Path(__file__).resolve().parents[2]
SOURCES = ROOT / "build/pollution-whisper-sources"
OUTPUT = ROOT / "src/main/resources/assets/mathmaster/sounds/ui/digital_pollution"
RATE = 48_000


def read_mono(path: Path) -> np.ndarray:
    sound = aud.Sound.file(str(path)).rechannel(aud.CHANNELS_MONO).resample(RATE)
    return np.asarray(sound.data(), dtype=np.float32)[:, 0]


def respeed(samples: np.ndarray, factor: float) -> np.ndarray:
    positions = np.arange(0.0, len(samples) - 1, factor, dtype=np.float64)
    return np.interp(positions, np.arange(len(samples)), samples).astype(np.float32)


def fade(samples: np.ndarray, seconds: float = 0.045) -> np.ndarray:
    count = min(len(samples) // 2, max(1, int(seconds * RATE)))
    result = samples.copy()
    result[:count] *= np.linspace(0.0, 1.0, count, dtype=np.float32)
    result[-count:] *= np.linspace(1.0, 0.0, count, dtype=np.float32)
    return result


def speech_envelope(samples: np.ndarray) -> np.ndarray:
    window = max(1, int(0.018 * RATE))
    env = np.convolve(np.abs(samples), np.ones(window, dtype=np.float32) / window, mode="same")
    peak = float(np.max(env))
    return env / peak if peak > 0.0 else env


def breath_layer(samples: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    noise = rng.normal(0.0, 1.0, len(samples)).astype(np.float32)
    spectrum = np.fft.rfft(noise)
    frequencies = np.fft.rfftfreq(len(samples), 1.0 / RATE)
    band = np.exp(-0.5 * ((frequencies - 3_200.0) / 1_900.0) ** 2)
    band *= np.clip((frequencies - 350.0) / 700.0, 0.0, 1.0)
    breath = np.fft.irfft(spectrum * band, n=len(samples)).astype(np.float32)
    peak = float(np.max(np.abs(breath)))
    if peak > 0.0:
        breath /= peak
    return breath * speech_envelope(samples) * 0.15


def shatter(samples: np.ndarray, rng: np.random.Generator, target_length: int) -> np.ndarray:
    result = np.zeros(target_length, dtype=np.float32)
    cursor = int(rng.uniform(0.1, 0.7) * RATE)
    while cursor < target_length:
        duration = int(rng.uniform(0.32, 1.15) * RATE)
        if duration >= len(samples):
            break
        source_start = int(rng.integers(0, len(samples) - duration))
        fragment = samples[source_start:source_start + duration]
        fragment = respeed(fragment, rng.uniform(0.82, 1.10))
        if rng.random() < 0.16:
            fragment = fragment[::-1].copy()
        fragment = fade(fragment) * rng.uniform(0.48, 0.82)
        end = min(target_length, cursor + len(fragment))
        result[cursor:end] += fragment[:end - cursor]
        cursor += int(rng.uniform(0.18, 0.82) * RATE)
    return result


def delayed(source: np.ndarray, seconds: float, gain: float) -> np.ndarray:
    result = np.zeros_like(source)
    offset = int(seconds * RATE)
    if 0 < offset < len(source):
        result[offset:] = source[:-offset] * gain
    return result


def create_variant(index: int, seconds: float) -> None:
    rng = np.random.default_rng(0xE1D17C + index * 1291)
    length = int(seconds * RATE)
    latin = read_mono(SOURCES / f"latin_{index}.wav")
    han = read_mono(SOURCES / f"han_{index}.wav")
    other_index = index % 6 + 1
    other_latin = read_mono(SOURCES / f"latin_{other_index}.wav")
    other_han = read_mono(SOURCES / f"han_{other_index}.wav")

    latin_fragments = shatter(latin, rng, length)
    han_fragments = shatter(han, rng, length)
    stray_latin = shatter(other_latin, rng, length)
    stray_han = shatter(other_han, rng, length)

    left = latin_fragments * 0.72 + han_fragments * 0.46 + stray_han * 0.22
    right = han_fragments * 0.70 + latin_fragments * 0.42 + stray_latin * 0.24
    left += breath_layer(left, rng)
    right += breath_layer(right, rng)

    t = np.arange(length, dtype=np.float32) / RATE
    sub_frequency = 29.0 + index * 1.65
    sub = np.sin(2.0 * math.pi * sub_frequency * t
                 + 0.31 * np.sin(2.0 * math.pi * 0.23 * t + index)) * 0.018
    left += delayed(right, 0.12 + index * 0.013, 0.17) + sub
    right += delayed(left.copy(), 0.18 + index * 0.011, 0.13) - sub * 0.8

    stereo = np.stack((left, right), axis=1)
    stereo = np.tanh(stereo * 1.35)
    stereo *= fade(np.ones(length, dtype=np.float32), 0.45)[:, None]
    peak = float(np.max(np.abs(stereo)))
    if peak > 0.0:
        stereo *= 0.76 / peak

    sound = aud.Sound.buffer(np.ascontiguousarray(stereo, dtype=np.float32), RATE)
    sound.write(str(OUTPUT / f"whisper_{index}.ogg"), RATE, aud.CHANNELS_STEREO,
                aud.FORMAT_S16, aud.CONTAINER_OGG, aud.CODEC_VORBIS,
                192_000, 4096)


def main() -> None:
    missing = [path for prefix in ("latin", "han") for index in range(1, 7)
               if not (path := SOURCES / f"{prefix}_{index}.wav").is_file()]
    if missing:
        raise FileNotFoundError("Run tools/audio/create_pollution_voice_sources.ps1 first")

    OUTPUT.mkdir(parents=True, exist_ok=True)
    durations = (8.4, 9.2, 8.8, 10.1, 9.6, 10.8)
    for index, duration in enumerate(durations, start=1):
        create_variant(index, duration)
    print(f"Generated {len(durations)} multilingual pollution whispers in {OUTPUT}")


if __name__ == "__main__":
    main()
