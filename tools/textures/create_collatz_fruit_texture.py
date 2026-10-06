"""Author a native 16px amber three-lobed fruit matching approved concept C.
The concept is a color/shape reference; its raster pixels are not edited or resampled.
"""
from pathlib import Path
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[2]

def fruit():
    im=Image.new("RGBA",(16,16));d=ImageDraw.Draw(im)
    outline="#641d2b";shadow="#b8452e";amber="#e99b2f";gold="#f6c448";light="#ffe5a0"
    # Dark-red stem and one small red leaf.
    d.line([(7,0),(7,4),(6,5)],fill=outline,width=2)
    d.point((7,1),fill="#b32146")
    d.polygon([(9,1),(12,1),(13,2),(11,3),(9,3),(8,2)],fill="#b32146")
    d.line((10,1,12,1),fill="#e34869")
    # Two side lobes behind the longer central lobe.
    d.polygon([(3,5),(5,4),(7,5),(7,11),(5,12),(2,11),(1,9),(1,7)],fill=outline)
    d.polygon([(3,6),(5,5),(6,6),(6,10),(5,11),(2,10),(2,7)],fill=amber)
    d.line((3,6,5,6),fill=gold);d.line((2,8,3,7),fill=light);d.line((2,10,5,11),fill=shadow)
    d.polygon([(9,5),(11,4),(13,5),(14,7),(14,10),(12,12),(9,11)],fill=outline)
    d.polygon([(10,6),(11,5),(13,6),(13,10),(12,11),(10,10)],fill=amber)
    d.line((11,6,12,6),fill=light);d.line((12,7,13,8),fill=gold);d.line((11,11,13,10),fill=shadow)
    d.polygon([(7,4),(9,5),(10,7),(10,12),(8,14),(6,14),(4,12),(4,8),(5,6)],fill=outline)
    d.polygon([(7,5),(8,6),(9,8),(9,11),(8,13),(6,13),(5,11),(5,8),(6,6)],fill=amber)
    d.rectangle((6,7,8,10),fill=gold);d.line((6,7,6,9),fill=light);d.point((7,11),fill=gold)
    d.line((6,12,8,12),fill=shadow)
    return im

def generate():
    im=fruit();target=ROOT/"src/main/resources/assets/mathmaster/textures/item/collatz_fruit.png"
    im.save(target)
    preview=Image.new("RGBA",(320,320),(35,28,31,255))
    preview.alpha_composite(im.resize((320,320),Image.Resampling.NEAREST))
    preview.save(ROOT/"build/collatz-fruit-preview.png")

if __name__=="__main__":generate()
