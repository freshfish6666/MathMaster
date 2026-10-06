"""Author the red Collatz building wood as deterministic native 16px artwork."""
from pathlib import Path
from random import Random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/mathmaster/textures"
rng = Random(331)


def planks():
    image = Image.new("RGBA", (16, 16))
    colors = [(120, 40, 47), (137, 48, 54), (151, 57, 61), (164, 66, 67)]
    for y in range(16):
        for x in range(16):
            image.putpixel((x, y), colors[rng.randrange(len(colors))])
    d = ImageDraw.Draw(image)
    for y in (3, 7, 11, 15):
        d.line((0, y, 15, y), fill="#49171e")
        d.line((0, y-1, 15, y-1), fill="#b45a59")
    for x, y in ((5, 0), (12, 4), (3, 8), (10, 12)):
        d.line((x, y, x, y+2), fill="#65212a")
    for x, y in ((1, 1), (7, 5), (5, 9), (0, 13)):
        d.line((x, y, min(15, x+5), y), fill="#822a33")
    return image


def framed_panel(window=False):
    image = planks()
    d = ImageDraw.Draw(image)
    d.rectangle((1, 1, 14, 14), outline="#49171e")
    d.rectangle((2, 2, 13, 13), outline="#c65c5d")
    if window:
        for box in ((4, 4, 6, 6), (9, 4, 11, 6), (4, 9, 6, 11), (9, 9, 11, 11)):
            d.rectangle(box, fill=(0, 0, 0, 0))
    return image


def main():
    images = {"collatz_planks": planks(), "collatz_door_bottom": framed_panel(),
              "collatz_door_top": framed_panel(True), "collatz_trapdoor": framed_panel(True)}
    d = ImageDraw.Draw(images["collatz_door_bottom"])
    d.rectangle((12, 3, 13, 4), fill="#efb75c")
    for name, image in images.items():
        p = OUT / "block" / (name + ".png")
        p.parent.mkdir(parents=True, exist_ok=True)
        image.save(p)
    # A compact inventory silhouette of the same two-panel door.
    tall = Image.new("RGBA", (16, 32))
    tall.paste(images["collatz_door_top"], (0, 0))
    tall.paste(images["collatz_door_bottom"], (0, 16))
    item = Image.new("RGBA", (16, 16))
    item.paste(tall.resize((7, 14), Image.Resampling.NEAREST), (4, 1))
    (OUT / "item").mkdir(parents=True, exist_ok=True)
    item.save(OUT / "item/collatz_door.png")
    preview = Image.new("RGBA", (80, 16), "#242027")
    for i, image in enumerate([*images.values(), item]):
        preview.paste(image, (i*16, 0), image)
    p = ROOT / "build/collatz-wood-texture-preview.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    preview.resize((800, 160), Image.Resampling.NEAREST).save(p)


if __name__ == "__main__":
    main()
