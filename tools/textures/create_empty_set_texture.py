from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/mathmaster/textures/item/empty_set.png"
PREVIEW = ROOT / "build/empty-set-preview.png"

SHADOW = (46, 29, 38, 255)
FLESH_DARK = (111, 48, 56, 255)
FLESH = (194, 101, 96, 255)
QUARTZ = (232, 226, 213, 255)
QUARTZ_LIGHT = (255, 250, 237, 255)


image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
draw = ImageDraw.Draw(image)

# Layered pixel strokes keep the symbol recognizable at inventory scale while
# exposing small flesh-colored seams beneath the quartz-white surface.
draw.ellipse((2, 2, 13, 13), outline=SHADOW, width=3)
draw.ellipse((2, 2, 13, 13), outline=QUARTZ, width=2)
draw.ellipse((3, 3, 12, 12), outline=QUARTZ_LIGHT)

draw.line((2, 14, 14, 1), fill=SHADOW, width=4)
draw.line((3, 13, 13, 2), fill=QUARTZ, width=2)
draw.line((4, 12, 13, 2), fill=QUARTZ_LIGHT)

# Sparse organic discoloration: visible up close, subordinate to the white body.
for x, y in ((3, 6), (5, 3), (10, 11), (12, 7), (8, 8)):
    image.putpixel((x, y), FLESH)
for x, y in ((4, 5), (10, 3), (12, 5), (8, 12), (6, 10)):
    image.putpixel((x, y), QUARTZ_LIGHT)

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
image.save(OUTPUT, optimize=True)

preview = Image.new("RGBA", (20, 20), (24, 18, 24, 255))
preview.alpha_composite(image, (2, 2))
PREVIEW.parent.mkdir(parents=True, exist_ok=True)
preview.resize((320, 320), Image.Resampling.NEAREST).save(PREVIEW)

print(f"Wrote {OUTPUT} ({image.width}x{image.height}) and {PREVIEW}")
