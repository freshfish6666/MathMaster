"""Approved cinematic direction: 19 game effects, NumPy + FFmpeg.
Old tonal sources are preserved under original-tone-backup. Never reads other mob audio.
"""
from pathlib import Path
import json, subprocess, wave
import numpy as np
import create_geometry_holder_cinematic_draft as design
ROOT=Path(__file__).resolve().parents[2]
RATE=design.RATE
MASTERS=ROOT/"dev-assets/geometry-holder/audio/cinematic"
OUTPUT=ROOT/"src/main/resources/assets/mathmaster/sounds/entity/geometry_holder"


def approved(name,start=0,end=None):
    with wave.open(str(design.OUT/(name+".wav")),"rb") as source:
        data=np.frombuffer(source.readframes(source.getnframes()),dtype="<i2").astype(float).reshape(-1,2).mean(axis=1)/32767
    return data[round(start*RATE):None if end is None else round(end*RATE)]


def render(name):
    if name=="phase_two": return approved("03_phase_two_awakening")
    if name=="halo_release": return approved("01_halo_shockwave",.8)
    if name=="beam_fire": return design.fade(approved("02_charged_beam",2,2.8),.003,.15)
    charges={"halo_charge":.8,"prison_charge":2,"beam_charge":2,"bomb_charge":1,"cross_charge":1}
    if name in charges:
        seconds=charges[name];dry=design.charge(seconds,1.2 if name=="prison_charge" else 1)
        if name=="prison_charge":
            for t in [.12,.48,.94,1.42]: design.insert(dry,design.crystal(.35,.75),t,.065)
        return design.fade(design.room(dry).mean(axis=1)[:round((seconds+.1)*RATE)],.01,.11)
    if name.startswith("ambient"):
        dry=design.fade(design.noise(2,35,320,-.6)*.10,.4,.65)
        return design.room(dry).mean(axis=1)
    if name.startswith("hurt"):
        dry=design.impact(.4,.6)+design.crystal(.4,1.1)*.15
        return design.room(dry).mean(axis=1)*.75
    if name=="death":
        dry=np.zeros(round(2.1*RATE))
        for t,g in [(0,.6),(.24,.35),(.63,.3),(1.12,.2)]: design.insert(dry,design.impact(.8),t,g)
        design.insert(dry,design.crystal(1.9,.48),.1,.23)
        return design.room(dry).mean(axis=1)
    if name=="alert": return design.room(design.charge(.6)+design.crystal(.6,.52)*.15).mean(axis=1)
    if name=="bomb_throw": return design.room(design.charge(.35)+design.impact(.35,.5)).mean(axis=1)
    if name=="bomb_explode": return design.room(design.impact(1.15)+design.crystal(1.15,.55)*.12).mean(axis=1)
    # Sparse, textural active accents remain shorter than the half-second playback interval.
    seconds=.40;t=np.arange(round(seconds*RATE))/RATE
    if name=="prison_cut": dry=design.noise(seconds,700,8500,-.4)*.17*np.exp(-t/.07)+design.crystal(seconds,1.7)*.10
    elif name=="cross_sweep": dry=design.noise(seconds,160,4300,-.5)*.17*np.sin(np.pi*t/seconds)**2
    else: dry=design.noise(seconds,55,1800,-.5)*.17+design.noise(seconds,1800,6500,-.5)*.045
    return design.fade(design.room(dry).mean(axis=1)[:round(.40*RATE)],.015,.09)


def write_wave(path,samples):
    with wave.open(str(path),"wb") as out:
        out.setnchannels(1);out.setsampwidth(2);out.setframerate(RATE)
        out.writeframes(np.rint(samples*32767).astype("<i2").tobytes())


def main():
    MASTERS.mkdir(parents=True,exist_ok=True);OUTPUT.mkdir(parents=True,exist_ok=True)
    names=["ambient_1","ambient_2","hurt_1","hurt_2","death","alert","phase_two","halo_charge","halo_release","prison_charge","prison_cut","beam_charge","beam_fire","beam_sustain","bomb_charge","bomb_throw","bomb_explode","cross_charge","cross_sweep"]
    reel=[];manifest=[];start=0
    for name in names:
        samples=render(name);samples*=min(1,.76/max(abs(samples)))
        wav=MASTERS/(name+".wav");write_wave(wav,samples)
        subprocess.run(["ffmpeg","-hide_banner","-loglevel","error","-y","-i",str(wav),"-c:a","libvorbis","-q:a","5",str(OUTPUT/(name+".ogg"))],check=True)
        manifest.append({"name":name,"start":start,"seconds":len(samples)/RATE,"peak":float(max(abs(samples)))})
        reel.extend([samples,np.zeros(round(.4*RATE))]);start+=(len(samples)/RATE)+.4
    write_wave(MASTERS/"listening_reel.wav",np.concatenate(reel))
    subprocess.run(["ffmpeg","-hide_banner","-loglevel","error","-y","-i",str(MASTERS/"listening_reel.wav"),"-q:a","2",str(MASTERS/"listening_reel.mp3")],check=True)
    (MASTERS/"manifest.json").write_text(json.dumps(manifest,indent=2),encoding="utf-8")
    print("19 cinematic game effects generated; approved masters remain untouched")


if __name__=="__main__": main()
