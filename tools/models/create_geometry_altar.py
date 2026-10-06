"""Native cuboid model/pixel textures derived from the accepted Geometry Altar concept.

Only exports the new altar/core assets. Does not alter the N altar or source concept.
"""
from pathlib import Path
import json
import random
from copy import deepcopy
from PIL import Image, ImageDraw
from create_n_altar import preview

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/mathmaster'
DEV = ROOT / 'dev-assets/geometry-holder/altar'


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def noise(base, seed):
    rng = random.Random(seed)
    image = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            n = rng.choice((-12, -7, -3, 0, 0, 4, 9))
            image.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in base) + (255,))
    return image


def textures(active):
    cyan = (75, 226, 239) if active else (55, 129, 145)
    light = (154, 250, 255) if active else (94, 165, 177)
    stone = noise((85, 105, 121), 721)
    d = ImageDraw.Draw(stone)
    d.line((0, 0, 15, 0), fill=(125, 144, 157))
    d.line((0, 0, 0, 15), fill=(113, 134, 149))
    d.line((0, 15, 15, 15), fill=(45, 66, 82))
    side = noise((68, 90, 105), 722)
    d = ImageDraw.Draw(side)
    d.rectangle((2, 2, 13, 13), outline=(33, 51, 64), width=2)
    d.rectangle((4, 4, 11, 11), outline=(127, 148, 157))
    d.rectangle((6, 6, 9, 9), fill=(28, 55, 69))
    d.rectangle((7, 7, 8, 8), fill=light)
    d.line((0, 3, 0, 12), fill=cyan)
    d.line((15, 3, 15, 12), fill=cyan)
    rim = stone.copy()
    d = ImageDraw.Draw(rim)
    d.rectangle((4, 0, 11, 15), fill=(35, 60, 75))
    d.rectangle((6, 0, 9, 15), fill=cyan)
    d.line((6, 0, 6, 15), fill=light)
    cap = stone.copy()
    d = ImageDraw.Draw(cap)
    d.rectangle((3, 3, 12, 12), fill=(39, 65, 81))
    d.rectangle((5, 5, 10, 10), fill=cyan)
    d.rectangle((6, 5, 9, 6), fill=light)
    post = stone.copy()
    d = ImageDraw.Draw(post)
    d.rectangle((5, 0, 10, 15), fill=(38, 60, 78))
    d.rectangle((7, 2, 8, 12), fill=cyan)
    pit = noise((24, 43, 53), 723)
    core = noise((43, 184, 207), 724)
    d = ImageDraw.Draw(core)
    d.rectangle((0, 0, 15, 15), outline=(15, 118, 148))
    d.rectangle((1, 1, 14, 14), outline=(112, 231, 240))
    for bounds, color in (((2, 2, 5, 5), (178, 252, 255)), ((9, 4, 12, 8), (86, 219, 234)),
                          ((4, 10, 7, 13), (24, 152, 184)), ((7, 7, 9, 9), (203, 255, 255))):
        d.rectangle(bounds, fill=color)
    return dict(stone=stone, side=side, rim=rim, cap=cap, post=post, pit=pit, core=core)


def box(a, b, tex, up=None, emissive=False):
    faces = {f: dict(uv=[0, 0, 16, 16], texture='#' + (up if f == 'up' and up else tex))
             for f in ('north', 'east', 'south', 'west', 'up', 'down')}
    if emissive:
        for face in faces.values():
            face['neoforge_data'] = dict(block_light=15, sky_light=15, ambient_occlusion=False)
    return dict(from_=a, to=b, faces=faces, shade=not emissive)


def cuboid(a, b, tex, up=None, emissive=False):
    part = box(a, b, tex, up, emissive)
    part['from'] = part.pop('from_')
    return part


def render_preview(parts, images, filename):
    # The shared software renderer uses full-face UVs; resolve model UV rotations first.
    rendered_parts, rendered_images = deepcopy(parts), dict(images)
    for i, part in enumerate(rendered_parts):
        for name, face in part['faces'].items():
            if face.get('rotation'):
                key = f'preview_{i}_{name}'
                rendered_images[key] = images[face['texture'][1:]].rotate(-face['rotation'])
                face['texture'] = '#' + key
    preview(rendered_parts, rendered_images, filename)
    path = ROOT / 'build' / filename
    with Image.open(path) as image:
        image.crop((32, 275, 448, 640)).save(path)


def main():
    idle, active = textures(False), textures(True)
    texture_dir = ASSETS / 'textures/block'
    texture_dir.mkdir(parents=True, exist_ok=True)
    for label, image in idle.items():
        image.save(texture_dir / f'geometry_altar_{label}.png')
    for label in ('side', 'rim', 'cap', 'post'):
        active[label].save(texture_dir / f'geometry_altar_{label}_active.png')
        glow = Image.new('RGBA', (16, 16))
        for y in range(16):
            for x in range(16):
                r, g, b, alpha = active[label].getpixel((x, y))
                if g - r > 45 and b - r > 45:
                    glow.putpixel((x, y), (r, g, b, alpha))
        glow.save(texture_dir / f'geometry_altar_{label}_glow.png')

    parts = [cuboid([0, 0, 0], [16, 2, 16], 'stone'),
             cuboid([2, 2, 2], [14, 7, 14], 'pit')]
    # Four walls leave a real central recess rather than merely painting a hole.
    for a, b in (([1, 2, 1], [15, 10, 4]), ([1, 2, 12], [15, 10, 15]),
                 ([1, 2, 4], [4, 10, 12]), ([12, 2, 4], [15, 10, 12])):
        parts.append(cuboid(a, b, 'side', 'pit'))
    for i, (a, b) in enumerate((([1, 10, 1], [15, 12, 4]), ([1, 10, 12], [15, 12, 15]),
                               ([1, 10, 4], [4, 12, 12]), ([12, 10, 4], [15, 12, 12]))):
        part = cuboid(a, b, 'stone', 'rim')
        if i < 2:
            part['faces']['up']['rotation'] = 90
        parts.append(part)
    for x, z in ((0, 0), (12, 0), (0, 12), (12, 12)):
        parts.extend([cuboid([x, 0, z], [x + 4, 3, z + 4], 'stone'),
                      cuboid([x + .5, 3, z + .5], [x + 3.5, 12, z + 3.5], 'post'),
                      cuboid([x, 12, z], [x + 4, 14, z + 4], 'stone', 'cap')])
    tex = {label: f'mathmaster:block/geometry_altar_{label}' for label in idle}
    tex['particle'] = tex['stone']
    active_tex = {**tex, **{label: tex[label] + '_active' for label in ('side', 'rim', 'cap', 'post')}}
    overlays = []
    # Per-face overlays illuminate only cyan pixels, leaving the stone responsive to world lighting.
    for part in parts:
        glowing_faces = {face: {**info, 'texture': info['texture'] + '_glow',
                              'neoforge_data': dict(block_light=15, sky_light=15, ambient_occlusion=False)}
                         for face, info in part['faces'].items()
                         if info['texture'] in ('#side', '#rim', '#cap', '#post')}
        if glowing_faces:
            overlays.append({'from': [v - .002 for v in part['from']],
                             'to': [v + .002 for v in part['to']], 'shade': False, 'faces': glowing_faces})
    active_tex.update({label + '_glow': tex[label] + '_glow' for label in ('side', 'rim', 'cap', 'post')})
    core = cuboid([5, 9, 5], [11, 15, 11], 'core', emissive=True)
    write(ASSETS / 'models/block/geometry_altar.json', dict(textures=tex, elements=parts))
    write(ASSETS / 'models/block/geometry_altar_summoning.json',
          dict(textures=active_tex, elements=parts + overlays + [core], render_type='minecraft:cutout'))
    write(ASSETS / 'blockstates/geometry_altar.json', {'variants': {
        'summoning=false': {'model': 'mathmaster:block/geometry_altar'},
        'summoning=true': {'model': 'mathmaster:block/geometry_altar_summoning'}}})
    write(ASSETS / 'models/item/geometry_altar.json', {'parent': 'mathmaster:block/geometry_altar',
          'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 1, 0], 'scale': [.7, .7, .7]},
                      'ground': {'translation': [0, 3, 0], 'scale': [.25, .25, .25]},
                      'fixed': {'scale': [.5, .5, .5]},
                      'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.375] * 3},
                      'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [.4] * 3}}})
    write(ASSETS / 'models/item/geometry_core.json', {'textures': {'core': tex['core'], 'particle': tex['core']},
          'elements': [cuboid([3, 3, 3], [13, 13, 13], 'core', emissive=True)],
          'display': {'gui': {'rotation': [30, 225, 0], 'scale': [.9] * 3},
                      'ground': {'translation': [0, 2, 0], 'scale': [.35] * 3},
                      'fixed': {'rotation': [0, 180, 0], 'scale': [.7] * 3},
                      'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.4] * 3},
                      'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [.5] * 3}}})
    write(ROOT / 'src/main/resources/data/mathmaster/loot_table/blocks/geometry_altar.json', {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'mathmaster:geometry_altar'}],
                                            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    write(DEV / 'geometry-altar-model-source.json', {'idle': {'textures': tex, 'elements': parts},
                                                  'summoning': {'textures': active_tex, 'elements': parts + overlays + [core]}})
    render_preview(parts, idle, 'geometry-altar-preview.png')
    render_preview(parts + [core], active, 'geometry-altar-summoning-preview.png')
    render_preview([cuboid([3, 3, 3], [13, 13, 13], 'core')], idle, 'geometry-core-preview.png')
    print(f'Geometry altar: {len(parts)} cuboids, hollow socket, cyan-only emissive overlays; separate 3D core item.')


if __name__ == '__main__':
    main()
