"""Illustrative 27s phase / defeat transition using the approved WAVs, not an in-game recording."""
from pathlib import Path
import subprocess
import wave
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
SR = 48000


def read(phase):
    with wave.open(str(ROOT / f"dev-assets/geometry-holder/music/approved/Geometric_Guardian_{phase}.wav")) as wav:
        assert wav.getframerate() == SR and wav.getnchannels() == 2 and wav.getsampwidth() == 2
        return np.frombuffer(wav.readframes(wav.getnframes()), "<i2").reshape(-1, 2).astype(float) / 32768


def main():
    first, second = read(1), read(2)
    result = np.zeros((27 * SR, 2))
    result[:12 * SR] = first[20 * SR:32 * SR] * .8
    t = np.linspace(0, 1, 2 * SR)
    eased = t * t * (3 - 2 * t)
    result[10 * SR:12 * SR] = .8 * (first[30 * SR:32 * SR] * np.cos(eased[:, None] * np.pi / 2)
                                          + second[:2 * SR] * np.sin(eased[:, None] * np.pi / 2))
    result[12 * SR:21 * SR] = second[2 * SR:11 * SR] * .8
    n = int(1.2 * SR)
    t = np.linspace(0, 1, n)
    eased = t * t * (3 - 2 * t)
    result[20 * SR:20 * SR + n] = .8 * (second[10 * SR:10 * SR + n] * np.cos(eased[:, None] * np.pi / 2)
                                               + second[int(83.2 * SR):int(83.2 * SR) + n] * np.sin(eased[:, None] * np.pi / 2))
    tail = second[int(84.4 * SR):]
    start = int(21.2 * SR)
    result[start:start + len(tail)] = tail * .8
    assert np.max(np.abs(result)) < 1, "Preview clips"
    target = ROOT / "build/geometry-holder-music-transition-preview.wav"
    with wave.open(str(target), "wb") as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(SR)
        wav.writeframes((result * 32767).astype("<i2").tobytes())
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(target),
                    "-c:a", "libmp3lame", "-b:a", "192k", str(target.with_suffix(".mp3"))], check=True)
    print("0-10s first stage / 10-12s phase transition / 12-20s second stage / 20-27s defeat ending")


if __name__ == "__main__":
    main()
