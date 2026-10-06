from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/mathmaster/textures/item"


def image() -> tuple[Image.Image, ImageDraw.ImageDraw]:
    result = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return result, ImageDraw.Draw(result)


def axiom_case() -> Image.Image:
    result, draw = image()
    outline = "#2a120d"
    darkest = "#4a2116"
    leather = "#7d3d24"
    light = "#a85b32"
    shine = "#d07b40"
    metal = "#76500d"
    gold = "#c68b20"
    gold_light = "#f2c957"

    # A compact leather field case with one broad lid and a simple lock.
    draw.line([(5, 3), (5, 2), (10, 2), (10, 3)], fill=outline, width=1)
    draw.line([(6, 2), (9, 2)], fill=leather, width=1)
    draw.polygon([(2, 4), (13, 4), (14, 5), (14, 12), (13, 14),
                  (2, 14), (1, 12), (1, 5)], fill=outline)
    draw.rectangle((2, 5, 13, 12), fill=leather)
    draw.rectangle((3, 6, 12, 7), fill=light)
    draw.line((3, 6, 11, 6), fill=shine)
    draw.rectangle((2, 9, 13, 12), fill=darkest)
    draw.line((3, 9, 12, 9), fill=leather)
    draw.point((2, 6), fill=light)
    draw.point((13, 6), fill=light)
    draw.point((3, 12), fill=leather)
    draw.point((12, 12), fill=leather)
    draw.rectangle((6, 8, 9, 10), fill=metal)
    draw.rectangle((7, 8, 8, 9), fill=gold_light)
    draw.rectangle((7, 10, 8, 10), fill=gold)
    draw.point((2, 13), fill=gold)
    draw.point((13, 13), fill=gold)
    return result


def oracle_case() -> Image.Image:
    result, draw = image()
    outline = "#180d24"
    darkest = "#29143f"
    purple = "#472361"
    light = "#6e3b8e"
    shine = "#a46ac0"
    metal = "#6a460d"
    gold = "#c28719"
    gold_light = "#f4cf56"
    gem = "#a642d0"
    gem_light = "#ed9cff"

    # A rounded divination case with a single readable central focus.
    draw.line([(5, 3), (5, 2), (10, 2), (10, 3)], fill=outline)
    draw.line((6, 2, 9, 2), fill=purple)
    draw.polygon([(3, 4), (12, 4), (14, 6), (14, 11), (12, 14),
                  (3, 14), (1, 11), (1, 6)], fill=outline)
    draw.polygon([(3, 5), (12, 5), (13, 6), (13, 11), (11, 13),
                  (4, 13), (2, 11), (2, 6)], fill=purple)
    draw.line((4, 5, 11, 5), fill=light)
    draw.line((3, 6, 3, 10), fill=light)
    draw.line((12, 6, 12, 10), fill=darkest)
    draw.line((3, 9, 12, 9), fill=darkest)
    draw.point((4, 6), fill=shine)
    draw.point((11, 12), fill=light)

    for x, y in ((2, 6), (13, 6), (2, 11), (13, 11)):
        draw.point((x, y), fill=gold)
    for x, y in ((2, 5), (13, 5), (2, 12), (13, 12)):
        draw.point((x, y), fill=gold_light)
    draw.polygon([(7, 6), (9, 8), (7, 10), (5, 8)], fill=metal)
    draw.polygon([(7, 7), (8, 8), (7, 9), (6, 8)], fill=gem)
    draw.point((7, 7), fill=gem_light)
    return result


def truth_case() -> Image.Image:
    result, draw = image()
    outline = "#321021"
    shadow = "#6b1747"
    crimson = "#9c205f"
    pink = "#d94a91"
    pale = "#ead0b9"
    ivory = "#fff0d4"
    metal = "#7a4e0b"
    gold = "#d59a1d"
    gold_light = "#ffe074"
    core = "#7d24b8"
    core_light = "#d677ff"

    # The final case is a clear reliquary: ivory frame, crimson panels, one core.
    draw.line([(5, 3), (5, 2), (10, 2), (10, 3)], fill=outline)
    draw.line((6, 2, 9, 2), fill=crimson)
    draw.polygon([(2, 4), (13, 4), (14, 5), (14, 13), (13, 14),
                  (2, 14), (1, 13), (1, 5)], fill=outline)
    draw.rectangle((2, 5, 13, 13), fill=pale)
    draw.rectangle((3, 6, 12, 12), fill=crimson)
    draw.line((3, 6, 12, 6), fill=ivory)
    draw.line((3, 7, 3, 12), fill=ivory)
    draw.line((12, 7, 12, 12), fill="#caa98f")
    draw.point((4, 7), fill=pink)
    draw.point((11, 7), fill=pink)
    draw.rectangle((4, 11, 5, 12), fill=shadow)
    draw.rectangle((10, 11, 11, 12), fill=shadow)

    for x, y in ((2, 5), (13, 5), (2, 13), (13, 13)):
        draw.point((x, y), fill=gold_light)
    for x, y in ((3, 5), (12, 5), (3, 13), (12, 13)):
        draw.point((x, y), fill=gold)
    draw.polygon([(7, 7), (10, 9), (7, 12), (4, 9)], fill=metal)
    draw.polygon([(7, 8), (9, 9), (7, 11), (5, 9)], fill=core)
    draw.rectangle((7, 9, 8, 9), fill=core_light)
    draw.point((7, 8), fill=ivory)
    return result


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    textures = {
        "axiom_case": axiom_case(),
        "oracle_case": oracle_case(),
        "truth_case": truth_case(),
    }
    for name, texture in textures.items():
        texture.save(OUTPUT / f"{name}.png", optimize=True)


if __name__ == "__main__":
    main()
