from pathlib import Path
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "src/main/resources/assets/mathmaster/textures/item/prime_core.png"
PRIMES = (2, 3, 5, 7, 11, 13, 17, 19, 23, 29)

GLYPHS = {
    "0": ("111", "101", "101", "101", "111"),
    "1": ("010", "110", "010", "010", "010"),
    "2": ("111", "001", "111", "100", "111"),
    "3": ("111", "001", "111", "001", "111"),
    "5": ("111", "100", "111", "001", "111"),
    "7": ("111", "001", "010", "010", "010"),
    "9": ("111", "101", "111", "001", "111"),
}


def set_pixel(image, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        image.putpixel((x, y), color)


def draw_core(number):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.polygon(((8, 0), (14, 3), (15, 9), (11, 14), (6, 15), (1, 11), (0, 5), (4, 1)), fill=(35, 18, 50, 255))
    draw.polygon(((8, 1), (13, 4), (14, 9), (10, 13), (6, 14), (2, 10), (1, 5), (5, 2)), fill=(104, 45, 126, 255))
    draw.polygon(((7, 3), (11, 4), (13, 8), (10, 12), (6, 12), (3, 9), (3, 5)), fill=(48, 24, 70, 255))
    for x, y, color in ((4, 3, (225, 139, 244, 255)), (5, 2, (245, 199, 255, 255)),
                        (12, 6, (174, 84, 207, 255)), (3, 11, (66, 27, 86, 255)),
                        (10, 13, (20, 10, 30, 255))):
        set_pixel(image, x, y, color)

    text = str(number)
    scale = 2 if len(text) == 1 else 1
    width = (len(text) * 3 + len(text) - 1) * scale
    height = 5 * scale
    start_x = (16 - width) // 2
    start_y = (16 - height) // 2
    for index, digit in enumerate(text):
        glyph = GLYPHS[digit]
        origin_x = start_x + index * 4 * scale
        for gy, row in enumerate(glyph):
            for gx, filled in enumerate(row):
                if filled == "1":
                    for sy in range(scale):
                        for sx in range(scale):
                            x = origin_x + gx * scale + sx
                            y = start_y + gy * scale + sy
                            set_pixel(image, x + 1, y + 1, (91, 26, 116, 255))
                            set_pixel(image, x, y, (247, 230, 255, 255))
    return image


def draw_glitch(current, following):
    image = draw_core(current)
    draw = ImageDraw.Draw(image)
    seed = current * 31 + following * 17
    colors = ((247, 230, 255, 255), (218, 72, 190, 255), (79, 218, 226, 255), (45, 17, 61, 255))
    for index in range(15):
        x = (seed + index * 7) % 14 + 1
        y = (seed // 3 + index * 11) % 14 + 1
        length = 1 + ((seed + index) % 3)
        draw.line((x, y, min(14, x + length), y), fill=colors[index % len(colors)])
    draw.rectangle((2, 6, 13, 7), fill=(35, 14, 48, 255))
    draw.line((4, 6, 8, 6), fill=(79, 218, 226, 255))
    draw.line((9, 7, 12, 7), fill=(218, 72, 190, 255))
    return image


frames = []
for index, prime in enumerate(PRIMES):
    frames.append(draw_core(prime))
    frames.append(draw_glitch(prime, PRIMES[(index + 1) % len(PRIMES)]))

sheet = Image.new("RGBA", (16, 16 * len(frames)), (0, 0, 0, 0))
for index, frame in enumerate(frames):
    sheet.paste(frame, (0, index * 16))

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
sheet.save(OUTPUT, optimize=True)
print(f"Wrote {OUTPUT} ({sheet.width}x{sheet.height}, {len(frames)} frames)")
