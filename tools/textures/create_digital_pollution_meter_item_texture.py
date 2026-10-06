from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/mathmaster/textures/item/digital_pollution_meter.png"


def build() -> Image.Image:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    outline = "#150d22"
    shadow = "#27153d"
    casing = "#3d245d"
    casing_light = "#63408a"
    screen = "#18080f"
    red = "#c51f35"
    red_light = "#ff5260"
    cyan = "#22b8c5"
    cyan_light = "#79edf0"
    metal = "#79520d"
    gold = "#d99c1f"
    magenta = "#b72be1"

    # Short sensor prong: readable at icon scale without the old cable tangle.
    draw.line([(9, 3), (9, 1), (11, 1), (11, 3)], fill=outline)
    draw.point((10, 1), fill=gold)
    draw.point((10, 2), fill=cyan_light)

    # Slightly asymmetric handheld meter body.
    draw.polygon([(3, 3), (11, 3), (13, 5), (13, 12), (11, 14),
                  (4, 14), (2, 12), (2, 5)], fill=outline)
    draw.polygon([(4, 4), (10, 4), (12, 5), (12, 11), (10, 13),
                  (4, 13), (3, 11), (3, 5)], fill=casing)
    draw.line((4, 4, 10, 4), fill=casing_light)
    draw.line((3, 5, 3, 10), fill=casing_light)
    draw.point((11, 5), fill=magenta)
    draw.point((12, 9), fill=magenta)

    # Red segmented readout. Two tiny 9s imply a near-critical live reading.
    draw.rectangle((3, 5, 11, 8), fill=screen)
    for offset in (4, 8):
        draw.line((offset, 5, offset + 2, 5), fill=red_light)
        draw.line((offset, 6, offset + 2, 6), fill=red)
        draw.point((offset + 2, 7), fill=red_light)

    # Circular pollution sensor and one clean circuit trace.
    draw.rectangle((5, 9, 10, 13), fill=shadow)
    draw.rectangle((6, 9, 9, 13), fill=outline)
    draw.point((6, 10), fill=casing_light)
    draw.point((9, 10), fill=casing_light)
    draw.point((6, 12), fill=casing_light)
    draw.point((9, 12), fill=casing_light)
    draw.rectangle((7, 10, 8, 11), fill=gold)
    draw.point((7, 12), fill=metal)
    draw.point((8, 12), fill=metal)

    draw.line([(4, 9), (4, 12), (5, 12)], fill=cyan)
    draw.point((4, 10), fill=cyan_light)
    draw.line([(10, 9), (11, 9), (11, 11)], fill=cyan)
    draw.point((11, 10), fill=cyan_light)
    draw.point((3, 12), fill=gold)
    draw.point((11, 12), fill=gold)
    return image


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    build().save(OUTPUT, optimize=True)


if __name__ == "__main__":
    main()
