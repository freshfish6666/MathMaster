"""Draw the approved silver-white/lavender concept as a native 16px inventory sprite.

The concept sheet remains an unmodified reference; this draws a fresh pixel texture,
not a crop or a downscaled AI sheet. Run only when intentionally regenerating this item.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/mathmaster/textures/item/prime_ingot.png"
PREVIEW = ROOT / "build/prime-ingot-preview.png"


# Match the existing Lingxu ingot's stepped silhouette and three-face shading.
# These pixel cells are drawn afresh; approved concept colors are adapted to that geometry.
SHAPE = (
    "................",
    "................",
    "..........CC....",
    ".......CCCHHD...",
    "....CCCHEEEEHD..",
    ".CCCHEEEEEEEEHD.",
    "CIEEEEEEEEEEIIGD",
    "CHIEEEEEEIIIGEHA",
    "CHHIEEIIIGFEEEEA",
    "CHHHIIFFEEEEEEHA",
    "CEHHGEEEEEEEEAA.",
    ".CEHGEEEEEAAA...",
    "..CEHEBAAA......",
    "...CCAA.........",
    "................",
    "................",
)
PALETTE = {
    "A": "#736c83",  # bottom edge
    "B": "#a297b3",
    "C": "#a69eb5",  # stepped rim
    "D": "#c6bed5",
    "E": "#eeeaf5",  # broad silver-white surfaces
    "F": "#baa0d8",  # small lavender seam
    "G": "#cfc2e5",
    "H": "#dcd5e8",  # end face
    "I": "#ffffff",  # bright bevel
}


def create_texture():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for y, row in enumerate(SHAPE):
        for x, cell in enumerate(row):
            if cell != ".":
                draw.point((x, y), fill=PALETTE[cell])
    return image


if __name__ == "__main__":
    sprite = create_texture()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    sprite.save(OUTPUT)
    preview = Image.new("RGBA", (384, 384), "#27272c")
    preview.alpha_composite(sprite.resize((320, 320), Image.Resampling.NEAREST), (32, 32))
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    preview.convert("RGB").save(PREVIEW)
    print(f"Wrote {OUTPUT} and {PREVIEW}")
