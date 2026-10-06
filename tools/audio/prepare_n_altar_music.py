"""Convert the approved song and its outro; never modify the input WAV."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import wave

ROOT = Path(__file__).resolve().parents[2]
OUTRO_START = 128.0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    args = parser.parse_args()
    source = args.source.resolve()
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    with wave.open(str(source)) as wav:
        duration = wav.getnframes() / wav.getframerate()
    if abs(duration - 144.08) > 0.01:
        raise SystemExit("Expected approved 144.08s song; update timing constants before replacing it.")
    archive = ROOT / "dev-assets/n-altar/music/approved"
    archive.mkdir(parents=True, exist_ok=True)
    preserved = archive / source.name
    if preserved.exists() and hashlib.sha256(preserved.read_bytes()).hexdigest() != digest:
        raise SystemExit("Existing preserved source differs; refusing to overwrite it.")
    if source != preserved.resolve():
        shutil.copy2(source, preserved)
    target = ROOT / "src/main/resources/assets/mathmaster/sounds/music/n_altar"
    target.mkdir(parents=True, exist_ok=True)
    for name, start in [("blood_sacrifice", 0), ("blood_sacrifice_outro", OUTRO_START)]:
        subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(source),
                        "-af", f"atrim=start={start},asetpts=PTS-STARTPTS", "-ar", "48000",
                        "-c:a", "libvorbis", "-q:a", "5", str(target / f"{name}.ogg")], check=True)
    if hashlib.sha256(source.read_bytes()).hexdigest() != digest:
        raise RuntimeError("Source changed during conversion")
    report = {"source": source.name, "source_sha256": digest, "duration_seconds": duration,
              "outro_start_seconds": OUTRO_START, "outro_duration_seconds": duration - OUTRO_START,
              "crossfade_seconds": 2, "range_fade_seconds": 1.5,
              "encoding": "Vorbis quality 5, 48000 Hz stereo, streamed in Minecraft"}
    (archive / "audio-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
