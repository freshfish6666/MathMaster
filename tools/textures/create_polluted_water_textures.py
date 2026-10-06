"""Deterministic seamless pixel animation: navy/cyan with sparse blood-red glitches."""
from pathlib import Path
import json, math, random
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/mathmaster"
def tile():
    rng = random.Random(13003)
    im = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            wave = math.sin(2 * math.pi * (x + y) / 16) + math.cos(2 * math.pi * y / 8)
            noise = rng.randrange(-3, 4)
            im.putpixel((x, y), (max(5, int(12 + wave * 2 + noise)),
                int(27 + wave * 4 + noise), int(49 + wave * 6 + noise), 225))
    d = ImageDraw.Draw(im)
    for _ in range(23):
        x, y = rng.randrange(32), rng.randrange(32)
        color = rng.choice([(23, 81, 103, 232), (29, 108, 121, 238), (21, 61, 88, 230)])
        for dx in range(rng.randrange(1, 5)):
            im.putpixel(((x + dx) % 32, y), color)
    for _ in range(9):
        x, y = rng.randrange(32), rng.randrange(32)
        im.putpixel((x, y), (rng.randrange(66, 89), 17, 28, 236))
    return im

def animation(name, size, flowing):
    base = tile()
    strip = Image.new("RGBA", (size, size * 16))
    for frame in range(16):
        out = Image.new("RGBA", (size, size))
        for y in range(size):
            # Sparse displaced rows, without bright flashing or changing the palette.
            shift = 2 if (y + frame * 2) % 32 == 7 else 0
            for x in range(size):
                out.putpixel((x, y), base.getpixel(((x + shift + frame // 4) % 32,
                    (y + (frame * 2 if flowing else frame // 2)) % 32)))
        strip.paste(out, (0, frame * size))
    target = ASSETS / "textures/block" / (name + ".png")
    target.parent.mkdir(parents=True, exist_ok=True)
    strip.save(target)
    target.with_suffix(".png.mcmeta").write_text(json.dumps({"animation": {
        "frametime": 4, "interpolate": False, "width": size, "height": size}}, indent=2) + "\n")

def bucket():
    # Exact vanilla metal/outline: only upper water/lava liquid differences may change; lower metal reflections stay.
    reference = ROOT / "dev-assets/textures/polluted-water"
    water = Image.open(reference / "minecraft_water_bucket.png").convert("RGBA")
    lava = Image.open(reference / "minecraft_lava_bucket.png").convert("RGBA")
    im = water.copy()
    liquid = [(x, y) for y in range(7) for x in range(16)
              if water.getpixel((x, y)) != lava.getpixel((x, y))]
    for x, y in liquid:
        r, g, b, alpha = water.getpixel((x, y))
        if b >= 220:
            color = (29, 108, 121, alpha)
        elif b >= 160:
            color = (21, 61, 88, alpha)
        else:
            color = (12, 27, 49, alpha)
        im.putpixel((x, y), color)
    # Two restrained blood-red accents, strictly within the liquid mask.
    for pos in [(6, 4), (10, 5)]:
        if pos in liquid:
            im.putpixel(pos, (78, 17, 28, water.getpixel(pos)[3]))
    im.save(ASSETS / "textures/item/digitally_polluted_water_bucket.png")

if __name__ == "__main__":
    animation("digitally_polluted_water_still", 32, False)
    animation("digitally_polluted_water_flow", 64, True)
    bucket()
