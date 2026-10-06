from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/mathmaster/textures/item"

# Vanilla diamond armor silhouettes and light levels, embedded so regeneration
# does not depend on a local Minecraft installation. A dot is transparent.
TEMPLATES = {
    "helmet": (
        "................",
        "................",
        "................",
        ".....111111.....",
        "....13444420....",
        "...1356544320...",
        "...1455443330...",
        "...1441110230...",
        "...1410000030...",
        "...1410000020...",
        "...1300000020...",
        "....10....00....",
        "................",
        "................",
        "................",
        "................",
    ),
    "chestplate": (
        "................",
        "................",
        ".11111....11111.",
        ".16541....16541.",
        ".154431..135441.",
        ".14445311344441.",
        ".12346555444321.",
        ".00255544444200.",
        "...0554444440...",
        "...0454444430...",
        "...0444444430...",
        "...0344444330...",
        "...0233333320...",
        "....02333320....",
        ".....000000.....",
        "................",
    ),
    "leggings": (
        "................",
        "................",
        "....11111110....",
        "...1566554420...",
        "...1554444430...",
        "...1544334430...",
        "...1543002430...",
        "...1440..0430...",
        "...1430..1430...",
        "...1430..1430...",
        "...1330..0330...",
        "...1320..0320...",
        "...0220..0220...",
        "...0000..0000...",
        "................",
        "................",
    ),
    "boots": (
        "................",
        "................",
        "................",
        "....111..111....",
        "...1650..1650...",
        "...1550..1540...",
        "...1540..1440...",
        "...1440..1440...",
        "...1430..1440...",
        "..14430..13430..",
        ".144320..123430.",
        ".133200..003320.",
        ".1000......0000.",
        "................",
        "................",
        "................",
    ),
}

PALETTE = (
    (35, 17, 46, 255),    # near-black violet outline
    (65, 31, 82, 255),    # deep violet border
    (105, 43, 132, 255),  # shaded plate
    (145, 58, 177, 255),  # dark Lingxu crystal
    (195, 82, 228, 255),  # Lingxu purple
    (232, 139, 252, 255), # polished highlight
    (253, 218, 255, 255), # pearl gleam
)

CORE = (190, 145, 247, 255)
CORE_LIGHT = (250, 230, 255, 255)
PEARL = (243, 176, 255, 255)
SEAM = (79, 34, 101, 255)

# Small symmetrical inlays replace the old chain-like noise with a coherent
# crystalline set motif. Entries outside each silhouette are ignored.
DETAILS = {
    "helmet": {
        (7, 4): CORE_LIGHT, (8, 4): CORE,
        (7, 5): CORE, (8, 5): CORE_LIGHT,
        (5, 6): PEARL, (10, 6): PEARL,
        (4, 9): SEAM, (11, 9): SEAM,
    },
    "chestplate": {
        (2, 3): PEARL, (13, 3): PEARL,
        (5, 5): SEAM, (10, 5): SEAM,
        (6, 6): PEARL, (9, 6): PEARL,
        (7, 7): CORE_LIGHT, (8, 7): CORE,
        (7, 8): CORE, (8, 8): CORE_LIGHT,
        (5, 11): SEAM, (10, 11): SEAM,
        (6, 12): PEARL, (9, 12): PEARL,
    },
    "leggings": {
        (6, 3): PEARL, (9, 3): PEARL,
        (7, 3): CORE_LIGHT, (8, 3): CORE,
        (7, 4): CORE, (8, 4): CORE_LIGHT,
        (4, 6): SEAM, (11, 6): SEAM,
        (4, 9): PEARL, (11, 9): PEARL,
        (4, 12): SEAM, (11, 12): SEAM,
    },
    "boots": {
        (5, 4): CORE_LIGHT, (10, 4): CORE_LIGHT,
        (5, 5): CORE, (10, 5): CORE,
        (3, 9): PEARL, (12, 9): PEARL,
        (2, 11): SEAM, (13, 11): SEAM,
    },
}


def build(name: str) -> Image.Image:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()
    template = TEMPLATES[name]
    for y, row in enumerate(template):
        for x, value in enumerate(row):
            if value != ".":
                pixels[x, y] = PALETTE[int(value)]
    for (x, y), color in DETAILS[name].items():
        if pixels[x, y][3] != 0:
            pixels[x, y] = color
    return image


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for name in TEMPLATES:
        build(name).save(OUTPUT / f"lingxu_{name}.png", optimize=True)


if __name__ == "__main__":
    main()
