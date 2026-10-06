"""Original 30-second N altar sketch: four MIDI parts and synthetic bowed strings.

Requires NumPy and FFmpeg. No recordings, samples or external musical material.
Outputs development assets only; never writes the mod's sound resources.
"""
from pathlib import Path
import json
import math
import shutil
import struct
import subprocess
import wave

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "dev-assets/n-altar/music/draft-01"
SR = 44100
BPM = 160
BEAT = 60 / BPM
DURATION = 30
RNG = np.random.default_rng(110315)
PARTS = [
    ("Violin I", 40, -.55, .78, [280, 460, 920, 1500, 2450]),
    ("Violin II", 40, .45, .53, [310, 510, 1020, 1650, 2600]),
    ("Viola", 41, -.12, .61, [190, 360, 690, 1160, 2100]),
    ("Cello", 42, .24, .88, [100, 210, 410, 780, 1500]),
]
NOTES = []


def note(part, beat, length, pitch, velocity):
    NOTES.append(dict(part=part, beat=beat, length=length, pitch=pitch, velocity=velocity))


def compose():
    # 20 bars: six-second entrance, twelve-second statement, contrast, climax.
    roots = [38, 39, 38, 45, 38, 34, 43, 45, 38, 34, 43, 45,
             43, 36, 39, 45, 38, 34, 39, 38]
    phrases = [
        [74, 81, 77, 79, 75, 74, 73, 74],
        [77, 74, 82, 81, 79, 77, 76, 73],
        [79, 82, 81, 77, 75, 74, 70, 73],
        [81, 76, 79, 73, 76, 75, 73, 81],
    ]
    for bar, root in enumerate(roots):
        start = bar * 4
        intensity = .70 if bar < 4 else (.86 if bar < 12 else (.78 if bar < 16 else 1.0))
        # Cello attacks alternate root, fifth and octave; avoid an unchanging drone.
        bass = [root, root, root + 7, root, root + 12, root + 7, root, root + 7]
        for step, pitch in enumerate(bass):
            if bar == 19 and step >= 5:
                continue
            note(3, start + step / 2, .34 if step % 2 else .41,
                 pitch, round((99 if step in (0, 4) else 76) * intensity))
        # Repeated viola bowing supplies the pulse, with displaced accents.
        third = 3 if bar not in (3, 7, 11, 15) else 4
        ostinato = [root + 19, root + 12 + third, root + 24, root + 12 + third,
                    root + 19, root + 24, root + 12 + third, root + 19]
        for step, pitch in enumerate(ostinato):
            if bar == 19 and step >= 5:
                continue
            note(2, start + step / 2 + .035, .29, pitch,
                 round((83 if step in (0, 3, 6) else 64) * intensity))
        if bar < 4:
            # Sparse high replies introduce the rhythmic character before the theme.
            for offset, pitch in [(1.5, 81), (2, 77), (3, 75 if bar % 2 else 74)]:
                note(0, start + offset, .35, pitch, 69 + bar * 3)
            note(1, start, 1.45, 69 + (1 if bar == 1 else 0), 55)
            note(1, start + 2.5, .8, 65 if bar != 3 else 64, 62)
        elif bar < 12:
            for step, pitch in enumerate(phrases[(bar - 4) % 4]):
                note(0, start + step / 2, .38 if step in (0, 4) else .30,
                     pitch + (12 if bar in (8, 10) and step in (0, 4) else 0),
                     101 if step in (0, 3, 6) else 87)
            for step, pitch in enumerate([root + 24, root + 19, root + 24 + third, root + 26]):
                note(1, start + step + .25, .58, pitch, 70 if step % 2 else 79)
        elif bar < 16:
            # Wider notes above the continuing short-bow ostinato create contrast.
            for offset, pitch, length in [(0, 82, 1.35), (1.5, 79, .85), (2.5, 77, .95), (3.5, 73, .35)]:
                note(0, start + offset, length, pitch - (2 if bar == 12 else 0), 91)
            for step, pitch in enumerate(phrases[bar % 4]):
                note(1, start + step / 2 + .12, .28, pitch - 12, 76 + (step % 3 == 0) * 9)
        else:
            for step, pitch in enumerate(phrases[bar % 4]):
                if bar == 19 and step >= 5:
                    continue
                for subdivision in range(2):
                    note(0, start + step / 2 + subdivision / 4, .185,
                         pitch + (12 if step in (0, 2, 4) else 0),
                         108 if subdivision == 0 else 85)
                note(1, start + step / 2 + .03, .30, pitch - 12, 88)
    # Final common down-bow and short tail, rather than cutting off an active phrase.
    for part, pitch in enumerate([86, 81, 65, 38]):
        note(part, 78.5, .9, pitch, 113)


def render_note(event):
    part = event["part"]
    length = event["length"] * BEAT
    attack = .010 if length < .3 else .025
    release = .065 if length < .3 else .13
    count = int((length + release) * SR)
    t = np.arange(count) / SR
    frequency = 440 * 2 ** ((event["pitch"] - 69) / 12)
    vibrato_depth = .0006 if length < .3 else .0032
    modulation = 1 + vibrato_depth * np.sin(2 * np.pi * (5.1 + part * .23) * t) * np.minimum(t / .12, 1)
    phase = 2 * np.pi * frequency * np.cumsum(modulation) / SR
    signal = np.zeros(count)
    # Harmonics shaped by instrument body resonances; slight detuning adds bow breadth.
    resonances = PARTS[part][4]
    for harmonic in range(1, 29):
        hz = frequency * harmonic
        if hz > 12500:
            break
        body = .28 + sum(weight * math.exp(-.5 * (math.log(hz / center) / .22) ** 2)
                         for center, weight in zip(resonances, [.9, 1.1, .95, .75, .48]))
        amplitude = body / harmonic ** 1.12 * math.exp(-hz / 5800)
        initial = RNG.uniform(-.12, .12)
        signal += amplitude * (.64 * np.sin(harmonic * phase + initial)
                               + .36 * np.sin(harmonic * phase * (1 + .0009) + initial + .17))
    envelope = np.minimum(t / attack, 1) ** .65
    envelope *= .77 + .23 * np.exp(-t / .055)
    envelope *= np.where(t <= length, 1, np.maximum(1 - (t - length) / release, 0) ** 1.8)
    # Subtle filtered bow friction, particularly at each attack.
    noise = RNG.standard_normal(count)
    smooth = np.convolve(noise, np.ones(9) / 9, mode="same")
    friction = smooth - np.convolve(smooth, np.ones(61) / 61, mode="same")
    signal += friction * (.030 + .055 * np.exp(-t / .020))
    signal /= max(np.sqrt(np.mean(signal ** 2)), .01)
    return signal * envelope * PARTS[part][3] * (event["velocity"] / 127) ** 1.55 * .12


def render():
    mix = np.zeros((int(DURATION * SR), 2), dtype=np.float64)
    for event in NOTES:
        sound = render_note(event)
        # Human timing is reproducible, and the MIDI itself remains quantized/editable.
        start = max(0, round((event["beat"] * BEAT + RNG.uniform(-.0025, .0025)) * SR))
        stop = min(start + len(sound), len(mix))
        pan = PARTS[event["part"]][2]
        mix[start:stop, 0] += sound[:stop-start] * math.cos((pan + 1) * math.pi / 4)
        mix[start:stop, 1] += sound[:stop-start] * math.sin((pan + 1) * math.pi / 4)
    # Small-room reflections and a low-level diffuse tail, not a washed-out cathedral.
    dry = mix.copy()
    for delay, strength in [(.023, .12), (.037, .10), (.061, .075), (.089, .055), (.131, .035)]:
        offset = round(delay * SR)
        mix[offset:] += dry[:-offset, ::-1] * strength
    size = int(.65 * SR)
    ir = RNG.standard_normal(size) * np.exp(-np.arange(size) / SR / .16)
    ir[:int(.025 * SR)] = 0
    ir = np.convolve(ir, np.ones(13)/13, mode="same")
    ir *= .09 / np.sqrt(np.sum(ir ** 2))
    fft_size = 1 << (len(mix) + size - 1).bit_length()
    kernel = np.fft.rfft(ir, fft_size)
    for channel in range(2):
        mix[:, channel] += np.fft.irfft(np.fft.rfft(dry[:, channel], fft_size) * kernel, fft_size)[:len(mix)]
    fade_in = int(.025 * SR)
    fade_out = int(.48 * SR)
    mix[:fade_in] *= np.linspace(0, 1, fade_in)[:, None]
    mix[-fade_out:] *= np.linspace(1, 0, fade_out)[:, None] ** 1.6
    mix -= mix.mean(axis=0)
    mix = np.tanh(mix * .16 / np.sqrt(np.mean(mix ** 2)))
    mix *= .89 / np.abs(mix).max()
    with wave.open(str(OUT / "n-altar-battle-draft-01.wav"), "wb") as target:
        target.setparams((2, 2, SR, len(mix), "NONE", "not compressed"))
        target.writeframes((mix * 32767).astype("<i2").tobytes())
    return dict(duration_seconds=len(mix)/SR, sample_rate=SR, channels=2,
                peak_dbfs=float(20*np.log10(np.abs(mix).max())),
                rms_dbfs=float(20*np.log10(np.sqrt(np.mean(mix**2)))), notes=len(NOTES))


def variable_length(value):
    result = [value & 127]
    while value >> 7:
        value >>= 7
        result.insert(0, 128 | (value & 127))
    return bytes(result)


def chunk(events):
    output = bytearray()
    previous = 0
    for tick, order, data in sorted(events, key=lambda item: (item[0], item[1])):
        output.extend(variable_length(tick-previous)); output.extend(data); previous=tick
    output.extend(b"\0\xff\x2f\0")
    return b"MTrk" + struct.pack(">I", len(output)) + output


def midi():
    # Format 1, conductor + four independently editable string tracks, PPQ 480.
    tempo = round(60_000_000/BPM)
    tracks = [chunk([(0, 0, b"\xff\x51\x03"+tempo.to_bytes(3,"big")),
                     (0, 1, b"\xff\x58\x04\x04\x02\x18\x08")])]
    for part, (name, program, pan, gain, body) in enumerate(PARTS):
        label = name.encode("ascii")
        events = [(0, 0, b"\xff\x03"+variable_length(len(label))+label),
                  (0, 1, bytes([0xc0+part, program])),
                  (0, 2, bytes([0xb0+part, 10, round((pan+1)*63.5)]))]
        for event in NOTES:
            if event["part"] != part:
                continue
            start = round(event["beat"] * 480)
            end = round((event["beat"] + event["length"]) * 480)
            events += [(start, 4, bytes([0x90+part,event["pitch"],event["velocity"]])),
                       (end, 3, bytes([0x80+part,event["pitch"],0]))]
        tracks.append(chunk(events))
    (OUT / "n-altar-battle-draft-01.mid").write_bytes(b"MThd"+struct.pack(">IHHH",6,1,5,480)+b"".join(tracks))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    compose()
    stats = render()
    midi()
    (OUT / "score-events.json").write_text(json.dumps(dict(bpm=BPM, bars=20, notes=NOTES), indent=2), encoding="utf-8")
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        raise RuntimeError("FFmpeg required for MP3 and OGG previews")
    for extension, encoder in [("mp3", ["-c:a","libmp3lame","-b:a","192k"]),
                               ("ogg", ["-c:a","libvorbis","-q:a","5"])]:
        subprocess.run([ffmpeg,"-y","-hide_banner","-loglevel","error","-i",
                        str(OUT / "n-altar-battle-draft-01.wav"), *encoder,
                        "-metadata","title=N altar - battle sketch 01",
                        str(OUT / f"n-altar-battle-draft-01.{extension}")],check=True)
    (OUT / "audio-report.json").write_text(json.dumps(stats,indent=2),encoding="utf-8")
    print(json.dumps(stats))


if __name__ == "__main__":
    main()
