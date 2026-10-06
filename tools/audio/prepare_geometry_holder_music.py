"""Preserve approved WAVs and derive streamed combat loops / natural defeat endings."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import wave

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT.parent / "MathMaster-resource/Music"
ARCHIVE = ROOT / "dev-assets/geometry-holder/music/approved"
TARGET = ROOT / "src/main/resources/assets/mathmaster/sounds/music/geometry_holder"
OUTRO_STARTS = (80.0, 83.2)


def main():
    ARCHIVE.mkdir(parents=True, exist_ok=True)
    TARGET.mkdir(parents=True, exist_ok=True)
    report = []
    for phase, start in enumerate(OUTRO_STARTS, 1):
        source = SOURCE / f"Geometric_Guardian_{phase}.wav"
        digest = hashlib.sha256(source.read_bytes()).hexdigest()
        preserved = ARCHIVE / source.name
        if preserved.exists() and hashlib.sha256(preserved.read_bytes()).hexdigest() != digest:
            raise RuntimeError("Preserved source differs: " + str(preserved))
        with wave.open(str(source)) as wav:
            duration = wav.getnframes() / wav.getframerate()
            if wav.getnchannels() != 2 or wav.getsampwidth() != 2 or duration <= start + 3:
                raise RuntimeError("Unexpected source format / ending duration")
        shutil.copy2(source, preserved)
        for name, begin, end in [(f"phase_{phase}", 0, start), (f"outro_{phase}", start, duration)]:
            length = end - begin
            filters = (f"atrim=start={begin}:end={end},asetpts=PTS-STARTPTS,"
                       f"afade=t=in:d=0.015,afade=t=out:st={length-.05}:d=0.05")
            subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(source),
                            "-af", filters, "-ar", "48000", "-c:a", "libvorbis", "-q:a", "6",
                            str(TARGET / f"{name}.ogg")], check=True)
        if hashlib.sha256(source.read_bytes()).hexdigest() != digest:
            raise RuntimeError("Input was modified")
        report.append(dict(phase=phase, source=source.name, sha256=digest,
                           duration=duration, combat_duration=start, outro_duration=duration-start))
    (ARCHIVE / "audio-report.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
