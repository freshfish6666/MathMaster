"""Generate only Geometry Construct's editable cuboids, vanilla UV atlas and model layer.

Animation is maintained in the Java model; this exporter never overwrites that file.
"""
from pathlib import Path
import base64
import json
import random
import uuid
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
DEV = ROOT / 'dev-assets/geometry-construct'
ASSETS = ROOT / 'src/main/resources/assets/mathmaster'
JAVA = ROOT / 'src/main/java/com/freshfish/mathmaster/client/entity/GeometryConstructMesh.java'
SIZE = 128


def main():
    DEV.mkdir(parents=True, exist_ok=True)
    specs = [
        ('shell_back', 'body', [-7, -7, -3], [14, 14, 10], 'glyph'),
        ('frame_top', 'body', [-7, -7, -7], [14, 3, 4], 'channel'),
        ('frame_bottom', 'body', [-7, 4, -7], [14, 3, 4], 'channel'),
        ('frame_left', 'body', [-7, -4, -7], [3, 8, 4], 'channel'),
        ('frame_right', 'body', [4, -4, -7], [3, 8, 4], 'channel'),
        ('socket', 'body', [-4, -4, -3.25], [8, 8, .25], 'dark'),
        ('core', 'body', [-2, -2, -6.8], [4, 4, 3], 'core'),
    ]
    for i, (x, y) in enumerate(((-9, -9), (9, -9), (-9, 9), (9, 9))):
        specs.append((f'fragment_{i}', f'fragment_{i}', [-2, -2, -2], [4, 4, 4], 'channel'))
    texture = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    glow = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    rng = random.Random(95081)
    boxes, elements = [], []
    cursor_x = cursor_y = row_height = 0
    for name, group, start, dims, material in specs:
        w, h, d = [max(1, round(value)) for value in dims]
        span_w, span_h = 2 * (w + d), h + d
        if cursor_x + span_w > SIZE:
            cursor_x = 0; cursor_y += row_height + 1; row_height = 0
        assert cursor_y + span_h <= SIZE
        u, v = cursor_x, cursor_y
        cursor_x += span_w + 1; row_height = max(row_height, span_h)
        # Java's UV layout uses int depth (the quarter-pixel socket has zero UV depth).
        td = int(dims[2])
        tw, th = int(dims[0]), int(dims[1])
        faces = {'west': [u, v + td, u + td, v + td + th],
                 'north': [u + td, v + td, u + td + tw, v + td + th],
                 'east': [u + td + tw, v + td, u + 2 * td + tw, v + td + th],
                 'south': [u + 2 * td + tw, v + td, u + 2 * td + 2 * tw, v + td + th],
                 'up': [u + td, v, u + td + tw, v + td],
                 'down': [u + td + tw, v, u + td + 2 * tw, v + td]}
        for face, rect in faces.items():
            x0, y0, x1, y1 = rect
            for py in range(y0, y1):
                for px in range(x0, x1):
                    xx, yy, fw, fh = px - x0, py - y0, x1 - x0, y1 - y0
                    noise = rng.choice((-12, -7, -3, 0, 5, 9))
                    base = (31, 48, 59) if material == 'dark' else (80, 103, 115)
                    color = tuple(max(0, c + noise) for c in base)
                    luminous = False
                    if material == 'core':
                        color = rng.choice(((44, 180, 200), (70, 211, 230), (129, 239, 248), (195, 254, 255)))
                        luminous = True
                    elif material == 'channel':
                        luminous = (xx == fw // 2 if fw < fh else yy == fh // 2)
                    elif material == 'glyph' and fw >= 8 and fh >= 8:
                        cx, cy = fw // 2, fh // 2
                        ring = max(abs(xx - cx), abs(yy - cy))
                        if ring in (3, 5): color = (34, 56, 66)
                        if ring in (2, 4): color = (113, 138, 149)
                        luminous = ring == 0 or xx == 1 or yy == 1
                    if luminous and material != 'core': color = (55, 207, 225)
                    texture.putpixel((px, py), color + (255,))
                    if luminous: glow.putpixel((px, py), color + (255,))
        pivot = [0, 13, 0] if group == 'body' else [(-9, 9, -9, 9)[int(group[-1])],
                13 + (-9, -9, 9, 9)[int(group[-1])], 0]
        boxes.append(dict(name=name, group=group, origin=start, size=dims, uv=[u, v], pivot=pivot))
        model_from = [start[0] + pivot[0], 24 - pivot[1] - start[1] - dims[1], start[2] + pivot[2]]
        model_to = [start[0] + pivot[0] + dims[0], 24 - pivot[1] - start[1], start[2] + pivot[2] + dims[2]]
        elements.append(dict(name=name, uuid=str(uuid.uuid5(uuid.NAMESPACE_URL, 'mathmaster:geometry_construct/' + name)),
                             type='cube', **{'from': model_from, 'to': model_to},
                             origin=[pivot[0], 24 - pivot[1], pivot[2]],
                             faces={f: dict(uv=uv, texture=0) for f, uv in faces.items()}))
    texture_dir = ASSETS / 'textures/entity'
    texture_dir.mkdir(parents=True, exist_ok=True)
    texture.save(texture_dir / 'geometry_construct.png')
    glow.save(texture_dir / 'geometry_construct_glow.png')
    (DEV / 'geometry-construct-model-source.json').write_text(json.dumps(dict(texture_size=[SIZE, SIZE], boxes=boxes), indent=2) + '\n')
    tex_data = base64.b64encode((texture_dir / 'geometry_construct.png').read_bytes()).decode()
    outliner = []
    for group in ('body', 'fragment_0', 'fragment_1', 'fragment_2', 'fragment_3'):
        pivot = next(b['pivot'] for b in boxes if b['group'] == group)
        outliner.append(dict(name=group, origin=[pivot[0], 24 - pivot[1], pivot[2]],
                             uuid=str(uuid.uuid5(uuid.NAMESPACE_URL, 'geometry_construct/group/' + group)),
                             children=[e['uuid'] for e, b in zip(elements, boxes) if b['group'] == group]))
    (DEV / 'geometry_construct.bbmodel').write_text(json.dumps(dict(meta=dict(format_version='4.10', model_format='free', box_uv=False),
        name='Geometry Construct', resolution=dict(width=SIZE, height=SIZE), elements=elements, outliner=outliner,
        textures=[dict(name='geometry_construct.png', id='0', width=SIZE, height=SIZE,
                       uuid=str(uuid.uuid5(uuid.NAMESPACE_URL, 'geometry_construct/texture')), source='data:image/png;base64,' + tex_data)]), indent=2) + '\n')
    java = ['package com.freshfish.mathmaster.client.entity;', '',
            'import net.minecraft.client.model.geom.PartPose;', 'import net.minecraft.client.model.geom.builders.*;', '',
            '/** Generated geometry only; animations live in GeometryConstructModel. */',
            'final class GeometryConstructMesh {', '    static LayerDefinition create() {',
            '        MeshDefinition mesh = new MeshDefinition();',
            '        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()']
    for b in boxes:
        if b['group'] == 'body':
            java.append(f'                .texOffs({b["uv"][0]}, {b["uv"][1]}).addBox(' + ', '.join(f'{n:.3f}F' for n in b['origin'] + b['size']) + ')')
    java[-1] += ', PartPose.offset(0, 13, 0));'
    for b in boxes:
        if b['group'] != 'body':
            # Satellites are children of the rotating body; convert absolute pivots to body-relative.
            pivot = [b['pivot'][0], b['pivot'][1] - 13, b['pivot'][2]]
            java.append(f'        body.addOrReplaceChild("{b["group"]}", CubeListBuilder.create().texOffs({b["uv"][0]}, {b["uv"][1]})')
            java.append('                .addBox(' + ', '.join(f'{n:.3f}F' for n in b['origin'] + b['size']) + '), PartPose.offset(' + ', '.join(f'{n:.3f}F' for n in pivot) + '));')
    java += [f'        return LayerDefinition.create(mesh, {SIZE}, {SIZE});', '    }', '}', '']
    JAVA.write_text('\n'.join(java))
    # Render the actual cuboids and atlas UVs, rather than reusing the AI concept as a game texture.
    from create_n_altar import preview
    preview_parts, preview_textures = [], {}
    for e in elements:
        faces = {}
        for face, info in e['faces'].items():
            uv = info['uv']
            if uv[0] == uv[2] or uv[1] == uv[3]:
                uv = [uv[0], uv[1], uv[0] + 1, uv[1] + 1]
            key = e['name'] + '_' + face
            preview_textures[key] = texture.crop(uv).resize((16, 16), Image.Resampling.NEAREST)
            faces[face] = dict(texture='#' + key)
        transform = lambda p: [p[0] * .65 + 8, p[1] * .65, p[2] * .65 + 8]
        preview_parts.append({'from': transform(e['from']), 'to': transform(e['to']), 'faces': faces})
    preview(preview_parts, preview_textures, 'geometry-construct-preview.png')
    output = ROOT / 'build/geometry-construct-preview.png'
    with Image.open(output) as image:
        image.crop((30, 275, 450, 640)).save(output)
    print(f'Geometry Construct: {len(boxes)} cuboids, editable .bbmodel, {SIZE}px vanilla UV/color and cyan glow atlases.')


if __name__ == '__main__':
    main()
