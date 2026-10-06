"""Build Seven in Blender; export the same cuboids and box-UV atlas to Minecraft.

Run: blender --background --python tools/models/create_seven_blender.py
Only Seven files and build/seven-export are written. Existing Eight/Nine assets are read-only.
"""
from pathlib import Path
import json
import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/seven-export'
TEXTURE = ROOT / 'src/main/resources/assets/mathmaster/textures/entity/seven.png'
ARMOR = Path('C:/software/Minecraft/1.21.1-learn/out/resources/assets/minecraft/textures/models/armor/diamond_layer_1.png')
OUT.mkdir(parents=True, exist_ok=True)
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)
parts = bpy.data.collections.new('SEVEN - Minecraft cuboids')
bpy.context.scene.collection.children.link(parts)

# 16 x 32 x 4 pixels, matching Eight and Nine. A descending stair makes a readable 7.
specs = []
def box(name, x, z, w, h, mat, y=0, d=4):
    specs.append((name, (x, y, z), (w, d, h), mat))
box('01 crown', 0, 29.5, 16, 5, 'skin')
for i in range(7):
    x, z = 5.5 - i * 1.5, 25 - i * 3.5
    box(f'02 shank {i}', x, z, 5, 4, 'skin')
    if i in (0, 2, 4, 6):
        # Separate front/back plates leave warm yellow joints visible.
        for side in (-1, 1):
            box(f'Diamond shank {i} {side}', x, z, 4, 2, 'diamond', side * 2.125, .25)
box('03 toe', -4, 1.25, 6, 2.5, 'skin')
for side in (-1, 1):
    box(f'Diamond crown {side}', 0, 29.5, 16, 4, 'diamond', side * 2.125, .25)
    box(f'Diamond boot {side}', -4, 1.25, 6, 1.5, 'diamond', side * 2.125, .25)
    box(f'Crown leather seam {side}', 0, 31.6, 16, .8, 'edge', side * 2.15, .3)
for x in (-3.5, 3.5):
    box(f'Eye socket {x}', x, 29.45, 3.6, 2.8, 'socket', -2.31, .08)
    box(f'Eye white {x}', x, 29.55, 3, 2.25, 'eye', -2.38, .04)
    box(f'Pupil {x}', x + (.45 if x < 0 else -.45), 29.35, .9, 1.55, 'pupil', -2.43, .04)

# Use an intact opaque patch of vanilla diamond armor, retaining its pixel pattern.
source = bpy.data.images.load(str(ARMOR), check_existing=True)
sp = list(source.pixels)
sw, sh = source.size
patch = None
for py in range(sh - 4):
    for px in range(sw - 8):
        if all(sp[((py + y) * sw + px + x) * 4 + 3] > .99 for y in range(4) for x in range(8)):
            patch = (px, py)
            break
    if patch:
        break
assert patch, 'Expected an opaque 8x4 diamond armor patch'
palette = {'skin': (.76, .60, .25), 'edge': (.36, .23, .10),
           'diamond': (.22, .84, .81), 'socket': (.055, .045, .025),
           'eye': (.94, .89, .69), 'pupil': (.12, .075, .025)}
slots = {name: ((i % 4) * 64, (i // 4) * 64) for i, name in enumerate(palette)}
atlas = bpy.data.images.new('Seven Minecraft atlas', width=256, height=128, alpha=True)
pixels = [0.] * (256 * 128 * 4)
for name, color in palette.items():
    u, v = slots[name]
    for y in range(64):
        for x in range(64):
            rgb = color
            if name == 'diamond':
                pos = ((patch[1] + (3 - y % 4)) * sw + patch[0] + x % 8) * 4
                rgb = sp[pos:pos + 3]
            elif name in ('skin', 'edge'):
                factor = (.94, 1, 1.05, .98)[(x + y * 3) % 4]
                rgb = [c * factor for c in color]
            dest = ((127 - v - y) * 256 + u + x) * 4
            pixels[dest:dest + 4] = [*rgb, 1]
atlas.pixels.foreach_set(pixels)
atlas.filepath_raw = str(TEXTURE)
atlas.file_format = 'PNG'
atlas.save()
atlas.pack()
mat = bpy.data.materials.new('Seven - exported pixel atlas')
mat.use_nodes = True
shader = mat.node_tree.nodes.get('Principled BSDF')
shader.inputs['Roughness'].default_value = .78
tex = mat.node_tree.nodes.new('ShaderNodeTexImage')
tex.image = atlas
tex.interpolation = 'Closest'
mat.node_tree.links.new(tex.outputs['Color'], shader.inputs['Base Color'])

geometry = []
for name, center, size, kind in specs:
    bpy.ops.mesh.primitive_cube_add(size=1, location=center)
    obj = bpy.context.object
    obj.name = name
    obj.dimensions = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    for collection in list(obj.users_collection):
        collection.objects.unlink(obj)
    parts.objects.link(obj)
    obj.data.materials.append(mat)
    w, d, h = size
    u, v = slots[kind]
    # Box UV with the same per-material tile used by the generated Java cuboids.
    uv = obj.data.uv_layers.active.data
    for face in obj.data.polygons:
        for li in face.loop_indices:
            co = obj.data.vertices[obj.data.loops[li].vertex_index].co
            if abs(face.normal.y) > .5:
                a, b = co.x + w/2, h/2 - co.z
                du, dv = d, d
            elif abs(face.normal.x) > .5:
                a, b = co.y + d/2, h/2 - co.z
                du, dv = 0, d
            else:
                a, b = co.x + w/2, co.y + d/2
                du, dv = d, 0
            uv[li].uv = ((u + du + a)/256, 1 - (v + dv + b)/128)
    x, y, z = center
    geometry.append({'name': name, 'uv': [u, v], 'box': [x-w/2, -z-h/2, y-d/2, w, h, d]})
(OUT/'cuboids.json').write_text(json.dumps(geometry, indent=2), encoding='utf-8')

# Reuse the existing Eight animation and rendering signatures; geometry has one source.
model_path = ROOT/'src/main/java/com/freshfish/mathmaster/client/entity/SevenModel.java'
template = (model_path.with_name('EightModel.java')).read_text(encoding='utf-8').replace('Eight', 'Seven').replace('eight', 'seven')
start = template.index('        cubes.texOffs(')
end = template.index('        mesh.getRoot()', start)
lines = []
for g in geometry:
    values = ', '.join(f'{n:.3f}F' for n in g['box'])
    lines.append(f"        cubes.texOffs({g['uv'][0]}, {g['uv'][1]}).addBox({values}); // {g['name']}\n")
model_path.write_text(template[:start] + ''.join(lines) + template[end:], encoding='utf-8')

bpy.ops.object.camera_add(location=(22, -75, 26))
camera = bpy.context.object
camera.rotation_euler = (Vector((0, 0, 16)) - camera.location).to_track_quat('-Z', 'Y').to_euler()
camera.data.type = 'ORTHO'
camera.data.ortho_scale = 39
scene = bpy.context.scene
scene.camera = camera
for location, energy in (((-25, -40, 50), 30000), ((25, -25, 30), 18000), ((0, 15, 30), 24000)):
    bpy.ops.object.light_add(type='AREA', location=location)
    light = bpy.context.object
    light.data.energy = energy
    light.data.shape = 'DISK'
    light.data.size = 25
    light.rotation_euler = (Vector((0, 0, 16)) - light.location).to_track_quat('-Z', 'Y').to_euler()
scene.render.engine = 'BLENDER_EEVEE'
scene.render.resolution_x = 640
scene.render.resolution_y = 640
scene.render.resolution_percentage = 100
scene.render.film_transparent = True
scene.render.image_settings.file_format = 'PNG'
scene.render.filepath = str(OUT/'seven_preview.png')
scene.view_settings.view_transform = 'Standard'
source.pack()
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT / "dev-assets/models/seven_v1.blend"))
bpy.ops.render.render(write_still=True)
print(f'Exported Seven: {len(geometry)} cuboids, packed Blender file, atlas, Java model and preview')
