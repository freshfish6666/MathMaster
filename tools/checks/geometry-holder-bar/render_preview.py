"""Rasterize the shared rectangle preview for local inspection (Pillow required)."""
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

root = ET.parse('build/geometry-holder-bar-preview.svg').getroot()
image = Image.new('RGBA', (960, 280))
font = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 30)
for node in root:
    color = tuple(bytes.fromhex(node.attrib['fill'][1:]))
    if node.tag.endswith('rect'):
        x, y, width, height = (round(float(node.attrib[key]) * 3) for key in ('x', 'y', 'width', 'height'))
        if width <= 0 or height <= 0:
            continue
        layer = Image.new('RGBA', image.size)
        ImageDraw.Draw(layer).rectangle((x, y, x + width - 1, y + height - 1),
                fill=color + (round(float(node.attrib.get('fill-opacity', 1)) * 255),))
        image = Image.alpha_composite(image, layer)
    elif node.tag.endswith('text'):
        ImageDraw.Draw(image).text((float(node.attrib['x'])*3, float(node.attrib['y'])*3),
                node.text, font=font, fill=color + (255,), anchor='ms')
image.convert('RGB').save(Path('build/geometry-holder-bar-preview.png'))
