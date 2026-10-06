"""Draw silver-white/lavender nugget and storage-block assets without changing the approved ingot."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
TEXTURES = ROOT / "src/main/resources/assets/mathmaster/textures"


def nugget():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.polygon(((7, 3), (10, 3), (12, 5), (12, 8), (10, 11), (6, 12), (3, 10), (3, 7), (5, 4)), fill="#766c86")
    draw.polygon(((7, 4), (9, 4), (11, 6), (9, 9), (5, 9), (4, 7), (6, 5)), fill="#eeeaf5")
    draw.polygon(((5, 10), (9, 10), (11, 7), (11, 9), (9, 11), (6, 11)), fill="#b7adc9")
    draw.line(((6, 5), (8, 4), (9, 5)), fill="#ffffff")
    draw.line(((5, 8), (7, 9), (9, 8)), fill="#c3a2ea")
    return image


def block():
    image = Image.new("RGBA", (16, 16), "#a79db8")
    draw = ImageDraw.Draw(image)
    draw.rectangle((1, 1, 14, 14), fill="#e8e3f1")
    draw.line(((0, 0), (15, 0)), fill="#fdfcff")
    draw.line(((0, 1), (0, 15)), fill="#f5f1fc")
    draw.line(((1, 15), (15, 15), (15, 1)), fill="#8c809f")
    draw.rectangle((2, 2, 13, 13), fill="#f3eff8")
    draw.line(((2, 13), (13, 13), (13, 2)), fill="#d1c7e0")
    # Quiet inset-square motif echoes the ingot's lavender bevel, readable on a full cube.
    draw.rectangle((4, 4, 11, 11), outline="#d6c4ea")
    draw.line(((5, 5), (10, 5)), fill="#ffffff")
    draw.line(((5, 6), (5, 10)), fill="#faf8fd")
    draw.line(((3, 2), (10, 2)), fill="#ffffff")
    return image


if __name__ == "__main__":
    particle, storage = nugget(), block()
    particle.save(TEXTURES / "item/prime_nugget.png")
    storage.save(TEXTURES / "block/prime_block.png")
    ingot = Image.open(TEXTURES / "item/prime_ingot.png").convert("RGBA")
    preview = Image.new("RGBA", (576, 224), "#27272c")
    for i, sprite in enumerate((ingot, particle, storage)):
        preview.alpha_composite(sprite.resize((160, 160), Image.Resampling.NEAREST), (16 + i * 192, 32))
    preview.convert("RGB").save(ROOT / "build/prime-material-preview.png")
    print("Wrote prime nugget, block and family preview; ingot unchanged")
