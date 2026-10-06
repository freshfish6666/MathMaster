from pathlib import Path
import json

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
TEXTURE = ROOT / "src/main/resources/assets/mathmaster/textures/item/set.png"
METADATA = TEXTURE.with_suffix(".png.mcmeta")
PREVIEW = ROOT / "build/set-preview.png"

QUARTZ = (235, 229, 216, 255)
QUARTZ_LIGHT = (255, 250, 237, 255)
FLESH = (190, 91, 91, 255)
FLESH_LIGHT = (225, 126, 116, 255)
FLESH_DARK = (92, 38, 48, 255)
SHADOW = (43, 27, 38, 255)

OBJECTS = (
    ("coal", "ore", ((46, 49, 55, 255), (18, 20, 24, 255), (91, 94, 101, 255))),
    ("raw_copper", "ore", ((96, 91, 76, 255), (202, 114, 77, 255), (83, 153, 126, 255))),
    ("iron", "ingot", ((104, 105, 106, 255), (220, 218, 205, 255), (250, 246, 225, 255))),
    ("gold", "ingot", ((151, 95, 13, 255), (246, 191, 32, 255), (255, 235, 91, 255))),
    ("redstone", "dust", ((86, 9, 13, 255), (190, 24, 32, 255), (255, 79, 66, 255))),
    ("lapis", "gem", ((23, 52, 130, 255), (42, 91, 197, 255), (93, 145, 235, 255))),
    ("diamond", "gem", ((24, 122, 130, 255), (75, 210, 202, 255), (184, 255, 238, 255))),
    ("emerald", "gem", ((14, 113, 57, 255), (29, 203, 89, 255), (133, 255, 174, 255))),
    ("amethyst", "gem", ((75, 42, 116, 255), (155, 102, 205, 255), (229, 190, 255, 255))),
    ("quartz", "gem", ((158, 145, 132, 255), (232, 220, 201, 255), (255, 247, 230, 255))),
    ("lingxu", "ingot", ((74, 31, 83, 255), (185, 78, 178, 255), (244, 160, 235, 255))),
)

EMPTY_AFTER = {1, 3, 6, 10}


def pixel(image, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        image.putpixel((x, y), color)


def draw_braces(image, phase=0):
    left = ((4, 2), (3, 2), (2, 3), (2, 4), (2, 5), (1, 6),
            (2, 7), (2, 8), (2, 9), (3, 10), (4, 10), (3, 11))
    right = tuple((15 - x, y) for x, y in left)
    for side_index, points in enumerate((left, right)):
        for index, (x, y) in enumerate(points):
            pixel(image, x, y + 1, SHADOW)
            selector = (index + side_index + phase) % 5
            color = QUARTZ if selector in (0, 1) else FLESH
            if selector == 0:
                color = QUARTZ_LIGHT
            elif selector == 4:
                color = FLESH_LIGHT
            pixel(image, x, y, color)
    pixel(image, 1, 12, FLESH_DARK)
    pixel(image, 14, 3, QUARTZ_LIGHT)


def draw_object(image, style, colors, phase=0):
    dark, mid, light = colors
    draw = ImageDraw.Draw(image)
    if style == "ore":
        draw.polygon(((6, 4), (10, 4), (11, 7), (10, 11), (7, 12), (5, 9), (5, 6)), fill=dark)
        for x, y in ((7, 5), (9, 6), (6, 8), (9, 10), (7, 11)):
            pixel(image, x + (phase & 1), y, mid)
        pixel(image, 8, 7, light)
    elif style == "ingot":
        draw.polygon(((7, 5), (11, 6), (10, 10), (6, 11), (5, 9), (6, 6)), fill=dark)
        draw.polygon(((7, 5), (10, 6), (9, 8), (6, 8)), fill=light)
        draw.polygon(((6, 8), (9, 8), (10, 10), (6, 10)), fill=mid)
    elif style == "dust":
        for x, y in ((8, 5), (6, 7), (9, 7), (5, 9), (7, 9), (10, 9), (6, 11), (9, 11)):
            pixel(image, x, y, mid if (x + y + phase) % 3 else light)
        pixel(image, 8, 8, dark)
    else:
        draw.polygon(((8, 4), (11, 7), (9, 12), (6, 11), (5, 7)), fill=dark)
        draw.polygon(((8, 5), (10, 7), (8, 10), (6, 8)), fill=mid)
        pixel(image, 8, 6, light)
        pixel(image, 7, 7, light)

    # A few dim pixels make the tiny object read as an unstable, indistinct member.
    pixel(image, 5 + phase % 2, 5, (*mid[:3], 135))
    pixel(image, 10, 11 - phase % 2, (*light[:3], 120))


def draw_empty_set(image, phase=0):
    draw = ImageDraw.Draw(image)
    outline = QUARTZ_LIGHT if phase % 2 == 0 else FLESH_LIGHT
    draw.ellipse((5, 4, 11, 11), outline=FLESH_DARK, width=2)
    draw.ellipse((5, 4, 10, 10), outline=outline)
    draw.line((5, 11, 11, 4), fill=FLESH_DARK, width=2)
    draw.line((6, 10, 11, 5), fill=QUARTZ_LIGHT)


def draw_glitch(image, current, following, phase):
    _, style_a, colors_a = current
    _, style_b, colors_b = following
    draw_object(image, style_a, colors_a, phase)
    draw = ImageDraw.Draw(image)
    draw.rectangle((5, 7, 11, 8), fill=(62, 30, 45, 230))
    draw.line((5, 6, 8, 6), fill=colors_b[1])
    draw.line((8, 9, 11, 9), fill=colors_b[2])
    pixel(image, 6, 11, QUARTZ_LIGHT)
    pixel(image, 10, 5, FLESH_LIGHT)


frames = []
durations = []
preview_frames = []
for index, entry in enumerate(OBJECTS):
    frame = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw_braces(frame, index)
    draw_object(frame, entry[1], entry[2], index)
    frames.append(frame)
    durations.append(7)
    preview_frames.append(frame)

    transition = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw_braces(transition, index + 1)
    following = OBJECTS[(index + 1) % len(OBJECTS)]
    if index in EMPTY_AFTER:
        draw_empty_set(transition, index)
        durations.append(3)
        preview_frames.append(transition)
    else:
        draw_glitch(transition, entry, following, index)
        durations.append(2)
    frames.append(transition)

sheet = Image.new("RGBA", (16, 16 * len(frames)), (0, 0, 0, 0))
for index, frame in enumerate(frames):
    sheet.paste(frame, (0, index * 16))

TEXTURE.parent.mkdir(parents=True, exist_ok=True)
sheet.save(TEXTURE, optimize=True)
METADATA.write_text(json.dumps({
    "animation": {
        "interpolate": False,
        "frames": [{"index": index, "time": duration} for index, duration in enumerate(durations)]
    }
}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

preview = Image.new("RGBA", (16 * len(preview_frames), 16), (24, 18, 24, 255))
for index, frame in enumerate(preview_frames):
    preview.alpha_composite(frame, (index * 16, 0))
PREVIEW.parent.mkdir(parents=True, exist_ok=True)
preview.resize((preview.width * 8, preview.height * 8), Image.Resampling.NEAREST).save(PREVIEW)

print(f"Wrote {TEXTURE} ({sheet.width}x{sheet.height}, {len(frames)} frames)")
print(f"Wrote {METADATA} and {PREVIEW}")
