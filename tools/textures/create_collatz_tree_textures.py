"""Editable 16px red Collatz tree sprites, matching approved concept C.
This authors native pixel textures; it does not crop or transform the AI concept.
"""
from pathlib import Path
import math, random
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/mathmaster/textures/block"
BARK = ["#49171e", "#65212a", "#822a33", "#9b343c", "#b6474c", "#c65c5d"]
LEAF = ["#70142d", "#941838", "#b32146", "#cf3054", "#e34869", "#f36d87"]

def bark():
    im = Image.new("RGBA", (16, 16))
    rng = random.Random(13031)
    bands = [1,2,3,2,4,3,2,1,2,3,4,3,2,3,2,1]
    for y in range(16):
        for x in range(16):
            offset = -1 if (x * 3 + y) % 11 == 0 else (1 if rng.random() < .12 else 0)
            im.putpixel((x,y), ImageColor(BARK[max(0,min(5,bands[x]+offset))]))
    d = ImageDraw.Draw(im)
    for x,y,length in [(1,0,5),(6,4,7),(11,9,5),(14,2,6)]:
        d.line((x,y,x,y+length), fill=BARK[0])
        d.line((x+1,y,x+1,y+length-1), fill=BARK[4])
    d.line([(4,9),(3,10),(3,12),(4,13),(5,12),(5,10),(4,9)],fill=BARK[1])
    return im

def ImageColor(hex_color):
    return tuple(int(hex_color[i:i+2],16) for i in (1,3,5))+(255,)

def end_grain():
    im=Image.new("RGBA",(16,16),BARK[1]);d=ImageDraw.Draw(im)
    for inset,color in [(1,BARK[3]),(2,"#cc6968"),(3,"#a84449"),(4,"#d77b75"),
                         (5,"#b85355"),(6,"#de8780"),(7,"#be5d5c")]:
        d.rectangle((inset,inset,15-inset,15-inset),outline=color)
    for pos in [(3,2),(12,3),(13,11),(4,12),(6,5)]:im.putpixel(pos,ImageColor("#b85355"))
    d.line((0,7,3,7),fill=BARK[1]);d.line((11,13,11,15),fill=BARK[1])
    return im

def roots():
    im=bark().copy();d=ImageDraw.Draw(im)
    for points in [[(0,2),(4,6),(8,7),(12,11),(15,12)],[(0,12),(4,10),(7,5),(11,3),(15,0)],
                   [(0,15),(4,13),(9,13),(12,8),(15,7)]]:
        d.line(points,fill=BARK[0],width=3)
        d.line(points,fill=BARK[4],width=1)
    return im

def node():
    im=bark().copy();d=ImageDraw.Draw(im)
    d.rectangle((1,1,14,14),fill=BARK[0]);d.rectangle((2,2,13,13),fill="#cc6a68")
    d.rectangle((3,3,12,12),fill="#a9474d");d.rectangle((4,4,11,11),fill="#e58d83")
    d.rectangle((5,5,10,10),fill="#bd5c5e");d.rectangle((6,6,9,9),fill="#f5aea0")
    d.rectangle((7,7,8,8),fill="#da827a")
    # A tiny break in the squared rings suggests a wood knot rather than machinery.
    d.line((11,2,11,4),fill="#a9474d");d.point((5,10),fill="#e58d83")
    return im

def leaves():
    im=Image.new("RGBA",(16,16));rng=random.Random(13032)
    centers=[(rng.randrange(16),rng.randrange(16)) for _ in range(18)]
    for y in range(16):
        for x in range(16):
            dist=min(min(abs(x-cx),16-abs(x-cx))**2+min(abs(y-cy),16-abs(y-cy))**2 for cx,cy in centers)
            if dist>6 or (dist>2 and rng.random()<.18):continue
            idx=max(0,min(5,5-int(dist*.8)+rng.choice([-1,0,0,1])))
            im.putpixel((x,y),ImageColor(LEAF[idx]))
    return im

def fruiting(leaf):
    im=leaf.copy();d=ImageDraw.Draw(im)
    for x,y in [(3,3),(11,6),(5,12)]:
        d.rectangle((x-1,y-1,x+2,y+2),fill="#852f37")
        d.rectangle((x,y,x+1,y+2),fill="#efb74b")
        d.point((x,y),fill="#ffe5a0");d.point((x+1,y+2),fill="#c47b35")
    return im

def sapling():
    im=Image.new("RGBA",(16,16));d=ImageDraw.Draw(im)
    d.line((7,14,7,5),fill=BARK[1],width=2)
    d.line((8,14,8,5),fill=BARK[4])
    d.line([(7,10),(4,7),(3,5)],fill=BARK[2])
    d.line([(8,8),(11,6),(12,3)],fill=BARK[3])
    for x,y in [(3,5),(5,8),(8,3),(11,5)]:
        d.rectangle((x-1,y-1,x+2,y+1),fill=LEAF[1])
        d.rectangle((x,y-2,x+1,y),fill=LEAF[3])
        d.line((x,y-1,x+1,y-1),fill=LEAF[5])
        d.point((x+2,y),fill=LEAF[4])
    d.line((5,15,10,15),fill=BARK[1])
    return im

def generate():
    OUT.mkdir(parents=True,exist_ok=True)
    leaf=leaves()
    textures={"collatz_sapling":sapling(),"collatz_root":roots(),"collatz_log":bark(),"collatz_log_top":end_grain(),
              "collatz_node":node(),"collatz_leaves":leaf,"fruiting_collatz_leaves":fruiting(leaf)}
    for name,im in textures.items():im.save(OUT/(name+".png"))
    # Enlarged pixels for inspection; labels use the same order as the creative tab.
    preview=Image.new("RGBA",(len(textures)*172+8,210),(35,28,31,255));d=ImageDraw.Draw(preview)
    for i,(name,im) in enumerate(textures.items()):
        preview.alpha_composite(im.resize((160,160),Image.Resampling.NEAREST),(i*172+4,20))
        d.text((i*172+4,185),name.replace("collatz_",""),fill="#efc9c5")
    target=ROOT/"build/collatz-red-texture-preview.png";target.parent.mkdir(exist_ok=True);preview.save(target)
    return textures

if __name__=="__main__":generate()
