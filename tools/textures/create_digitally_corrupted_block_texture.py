from pathlib import Path
import random

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/mathmaster/textures/block/digitally_corrupted_block.png"
PREVIEW = ROOT / "build/digitally-corrupted-block-preview.png"

random.seed(7122)
palette = (
    (24, 5, 10, 255),
    (38, 7, 14, 255),
    (55, 9, 19, 255),
    (72, 12, 25, 255),
    (91, 18, 32, 255),
    (111, 24, 39, 255),
)
image = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        shade = random.choices(range(len(palette)), weights=(5, 9, 11, 8, 4, 1))[0]
        image.putpixel((x, y), palette[shade])

draw = ImageDraw.Draw(image)
deep = (17, 4, 12, 255)
flesh = (132, 30, 45, 255)
wet = (166, 42, 54, 255)
sculk = (10, 55, 58, 255)
sculk_light = (19, 91, 91, 255)
glyph = (34, 113, 108, 255)

# Blood-red organic ridges dominate the surface. Sculk colors remain in thin,
# interrupted veins rather than taking over the palette.
draw.line(((0, 3), (3, 2), (5, 4), (8, 3), (11, 5), (15, 4)), fill=flesh)
draw.line(((0, 12), (3, 11), (6, 13), (9, 12), (12, 14), (15, 13)), fill=deep)
draw.line(((6, 0), (7, 3), (6, 6), (8, 9), (7, 12), (8, 15)), fill=wet)
draw.line(((0, 8), (3, 8), (5, 7)), fill=sculk)
draw.line(((10, 1), (12, 2), (15, 1)), fill=sculk)
draw.line(((10, 9), (12, 8), (15, 9)), fill=sculk_light)

# Tiny broken glyphs: a few 0/1 fragments mixed with punctuation-like noise.
# Every mark is only one or two pixels wide, so the block reads as corrupted
# texture first and encoded data only on closer inspection.
for x, y in ((1, 1), (4, 6), (12, 6), (2, 14)):
    draw.point((x, y), fill=glyph)
    draw.point((x + 1, y), fill=sculk_light)
for x, y in ((2, 5), (10, 11)):
    draw.point((x, y), fill=glyph)
    draw.point((x, y + 1), fill=glyph)
for x, y in ((13, 3), (4, 14)):
    draw.point((x, y), fill=glyph)
    draw.point((x + 1, y + 1), fill=sculk_light)
draw.point((14, 6), fill=glyph)
draw.point((14, 8), fill=glyph)
draw.point((9, 6), fill=sculk_light)
draw.point((10, 6), fill=glyph)
draw.point((9, 7), fill=glyph)

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
PREVIEW.parent.mkdir(parents=True, exist_ok=True)
image.save(OUTPUT)
image.resize((256, 256), Image.Resampling.NEAREST).save(PREVIEW)
print(f"Wrote {OUTPUT}")
print(f"Wrote {PREVIEW}")
