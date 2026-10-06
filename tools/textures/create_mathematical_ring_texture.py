"""Generate the 16x16 pixel-art texture for the creative-only Mathematical Ring."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/mathmaster/textures/item/mathematical_ring.png"
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
p = img.load()

# Dark outer contour and a pale quartz/silver ring body.
dark = (34, 24, 49, 255)
shadow = (91, 75, 112, 255)
silver = (221, 218, 230, 255)
light = (255, 250, 238, 255)
gold = (224, 166, 45, 255)
violet = (142, 59, 180, 255)
cyan = (57, 194, 190, 255)

outer = {(x, y) for y in range(3, 14) for x in range(2, 14)
         if 18 <= (x - 7.5) ** 2 + (y - 8.0) ** 2 <= 35}
for x, y in outer:
    p[x, y] = dark
for y in range(4, 13):
    for x in range(3, 13):
        radius = (x - 7.5) ** 2 + (y - 8.0) ** 2
        if 20 <= radius <= 29:
            p[x, y] = silver if x + y < 16 else shadow
for x, y in ((5, 3), (6, 2), (7, 2), (8, 2), (9, 3), (4, 4), (10, 4)):
    p[x, y] = gold
for x in range(5, 10):
    for y in range(3, 7):
        if (x, y) in ((5, 3), (9, 3)):
            continue
        p[x, y] = violet if (x + y) % 2 else (178, 83, 202, 255)

# Tiny ∑/∞-like strokes make the mathematical theme readable without text.
for x, y in ((6, 4), (7, 4), (8, 4), (6, 5), (7, 6), (8, 6)):
    p[x, y] = light
for x, y in ((4, 8), (5, 7), (6, 8), (5, 9), (9, 8), (10, 7), (11, 8), (10, 9), (7, 8), (8, 8)):
    if p[x, y][3] == 0:
        p[x, y] = cyan

OUT.parent.mkdir(parents=True, exist_ok=True)
img.save(OUT)
print(OUT)
