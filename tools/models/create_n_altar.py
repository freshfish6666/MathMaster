"""Native cuboid model and pixel textures; does not alter AI concept originals."""
from pathlib import Path
import json
from random import Random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/mathmaster"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main():
    rng = Random(730)
    stone = Image.new("RGBA", (16, 16))
    palette = [(79,110,120), (87,119,128), (96,128,136), (104,137,144)]
    for y in range(16):
        for x in range(16):
            stone.putpixel((x,y), palette[rng.randrange(4)])
    robe = stone.copy()
    d = ImageDraw.Draw(robe)
    for x in (3,8,13): d.line((x,0,x,15),fill="#45616f")
    dark = Image.new("RGBA", (16,16), "#203641")
    glyph = dark.copy()
    d = ImageDraw.Draw(glyph)
    d.rectangle((1,1,14,14), outline="#547a87")
    # Readable double-stroked natural number N at native resolution.
    d.line((4,12,4,3), fill="#a0e5e9", width=2)
    d.line((10,3,10,12), fill="#a0e5e9", width=2)
    d.line((5,3,10,12), fill="#a0e5e9", width=2)
    d.line((12,3,12,12), fill="#69bdcb")
    blood_glyph = dark.copy()
    ImageDraw.Draw(blood_glyph).rectangle((1,1,14,14), outline="#547a87")
    blood_glow = Image.new("RGBA", (16,16), (0,0,0,0))
    for image in (blood_glyph,blood_glow):
        d = ImageDraw.Draw(image)
        d.line((4,12,4,3), fill="#ff5c66", width=2)
        d.line((10,3,10,12), fill="#ff5c66", width=2)
        d.line((5,3,10,12), fill="#ff5c66", width=2)
        d.line((12,3,12,12), fill="#ca2a3c")
    textures = {"stone":stone,"robe":robe,"dark":dark,"n":glyph,
                "n_blood":blood_glyph,"n_blood_glow":blood_glow}
    for name,im in textures.items():
        p = ASSETS / "textures/block" / ("n_altar_"+name+".png")
        p.parent.mkdir(parents=True,exist_ok=True)
        im.save(p)
    parts=[]
    def box(a,b,tex="stone",front=None):
        faces={f:{"uv":[0,0,16,16],"texture":"#"+tex} for f in ("north","south","east","west","up","down")}
        if front: faces["north"]["texture"]="#"+front
        parts.append({"from":a,"to":b,"faces":faces})
    # Lower block: pedestal and deliberately sparse angular tentacle hints.
    box([1,0,1],[15,2,15]); box([2,2,2],[14,4,14])
    box([4,4,4],[12,13,12],"robe")
    box([4,5,3],[12,12,4],"dark","n")
    box([3,13,3],[13,15,13]); box([4,15,4],[12,16,12],"robe")
    for x in (2,12):
        box([x,2,3],[x+2,6,7]); box([x,6,4],[x+2,10,6])
    # Upper block: folded robes, block hands and the held hollow cube.
    box([4,16,5],[12,23,12],"robe")
    box([3,19,4],[5,23,8]); box([11,19,4],[13,23,8])
    box([4,18,2],[6,20,6]); box([10,18,2],[12,20,6])
    # Hollow geometry is actual open air, not a drawn fake cube.
    box([6,19,2],[10,20,6]); box([6,22,2],[10,23,6])
    box([6,20,2],[7,22,6]); box([9,20,2],[10,22,6])
    # Simple hood around a dark, faceless recess.
    box([5,23,5],[11,30,11],"dark")
    box([4,23,4],[6,30,12]); box([10,23,4],[12,30,12])
    box([6,29,4],[10,31,12]); box([6,23,10],[10,29,12])
    # Broken square halo, kept entirely inside the footprint.
    box([2,24,12],[3,30,13]); box([13,24,12],[14,30,13])
    box([2,30,12],[7,32,13]); box([9,30,12],[14,32,13])
    tex={n:"mathmaster:block/n_altar_"+n for n in textures}
    tex["particle"]=tex["stone"]
    for half,low,high in (("lower",0,16),("upper",16,32)):
        elements=[]
        for part in parts:
            if part["from"][1]>=low and part["to"][1]<=high:
                e=json.loads(json.dumps(part))
                e["from"][1]-=low; e["to"][1]-=low
                elements.append(e)
        write(ASSETS / ("models/block/n_altar_"+half+".json"),{"textures":tex,"elements":elements})
        if half == "lower":
            blood_elements=json.loads(json.dumps(elements))
            for element in blood_elements:
                for face in element["faces"].values():
                    if face["texture"] == "#n": face["texture"]="#n_blood"
            # Only the glyph pixels are fullbright; the stone and plaque obey world lighting.
            blood_elements.append({"from":[4,5,2.998],"to":[12,12,2.999],"shade":False,
                "faces":{"north":{"uv":[0,0,16,16],"texture":"#n_blood_glow",
                    "neoforge_data":{"block_light":15,"sky_light":15,"ambient_occlusion":False}}}})
            write(ASSETS / "models/block/n_altar_lower_blood.json",
                  {"textures":tex,"render_type":"minecraft:cutout","elements":blood_elements})
    variants={}
    for direction,y in (("north",0),("east",90),("south",180),("west",270)):
        for half in ("lower","upper"):
            for blood in (False,True):
                model=half+"_blood" if half=="lower" and blood else half
                variants[f"blood_sacrifice={str(blood).lower()},facing={direction},half={half}"]={"model":"mathmaster:block/n_altar_"+model,"y":y}
    write(ASSETS / "blockstates/n_altar.json",{"variants":variants})
    write(ASSETS / "models/item/n_altar.json",{
        "textures":tex,"elements":parts,"gui_light":"front",
        "display":{
            "gui":{"rotation":[30,225,0],"translation":[0,-4,0],"scale":[0.4,0.4,0.4]},
            "ground":{"translation":[0,2,0],"scale":[0.2,0.2,0.2]},
            "fixed":{"translation":[0,-4,0],"scale":[0.4,0.4,0.4]},
            "thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,1,0],"scale":[0.25,0.25,0.25]},
            "firstperson_righthand":{"rotation":[0,45,0],"translation":[0,-2,0],"scale":[0.35,0.35,0.35]}}})
    preview(parts,textures)
    preview(parts,{**textures,"n":blood_glyph},"n-altar-blood-preview.png")
    write(ROOT / "dev-assets/n-altar/n-altar-model-source.json",{"textures":tex,"elements":parts})
    print(f"N altar: {len(parts)} cuboids + blood-only glyph overlay, 6 native 16px textures, two-block model")


def preview(parts,textures,filename="n-altar-preview.png"):
    """Orthographic textured rendering of the exact model cuboids, not concept art."""
    import numpy as np
    pixels=np.zeros((640,480,3),dtype=np.uint8)
    pixels[:]=[32,39,46]
    depth_buffer=np.full((640,480),-np.inf)
    def project(p):
        x,y,z=p
        return (240+(x+z-16)*12,570-y*14+(x-z)*5)
    faces=[]
    for part in parts:
        x,y,z=part["from"];X,Y,Z=part["to"]
        # North (front), east and top; far surfaces are painted first.
        for name,quad,light in (
            ("north",[(x,Y,z),(X,Y,z),(X,y,z),(x,y,z)],1.0),
            ("east",[(X,Y,Z),(X,Y,z),(X,y,z),(X,y,Z)],0.72),
            ("up",[(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],1.15)):
            depth=sum(p[0]-p[2]-p[1]*0.1 for p in quad)/4
            faces.append((depth,quad,textures[part["faces"][name]["texture"][1:]],light))
    for _,quad,texture,light in faces:
        coords=np.array([project(p) for p in quad])
        depths=np.array([p[0]+p[1]*5/7-p[2] for p in quad])
        uv=np.array([[0,0],[1,0],[1,1],[0,1]])
        tex=np.array(texture)[:,:,:3]
        for indices in ((0,1,2),(0,2,3)):
            a,b,c=coords[list(indices)]
            x0=max(0,int(min(a[0],b[0],c[0])));x1=min(479,int(max(a[0],b[0],c[0]))+1)
            y0=max(0,int(min(a[1],b[1],c[1])));y1=min(639,int(max(a[1],b[1],c[1]))+1)
            xx,yy=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5)
            denominator=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
            if abs(denominator)<1e-6:continue
            wa=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/denominator
            wb=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/denominator
            wc=1-wa-wb
            weights=np.stack([wa,wb,wc],axis=-1)
            depth=weights@depths[list(indices)]
            old=depth_buffer[y0:y1+1,x0:x1+1]
            mask=(wa>=-1e-5)&(wb>=-1e-5)&(wc>=-1e-5)&(depth>=old)
            interpolated=weights@uv[list(indices)]
            u=np.clip((interpolated[:,:,0]*16).astype(int),0,15)
            v=np.clip((interpolated[:,:,1]*16).astype(int),0,15)
            colors=np.clip(tex[v,u]*light,0,255).astype(np.uint8)
            pixels[y0:y1+1,x0:x1+1][mask]=colors[mask]
            old[mask]=depth[mask]
    canvas=Image.fromarray(pixels)
    path=ROOT/"build"/filename
    path.parent.mkdir(parents=True,exist_ok=True)
    canvas.save(path)


if __name__ == "__main__": main()
