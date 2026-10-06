"""Run with Blender --background nine_v1.blend --python tools/models/export_nine.py.

Exports the approved cuboids and a vanilla box-UV atlas; does not edit the blend.
"""
import json
from pathlib import Path
import bpy

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/nine-export'
OUT.mkdir(parents=True, exist_ok=True)
parts = sorted((o for o in bpy.data.collections['NINE - model parts'].objects
                if o.type == 'MESH'), key=lambda o: o.name)
materials = list(dict.fromkeys(o.active_material for o in parts))
atlas = bpy.data.images.new('nine', width=256, height=128, alpha=True)
pixels = [0.0] * (256 * 128 * 4)
slots = {}
for index, mat in enumerate(materials):
    u, v = (index % 4) * 64, (index // 4) * 64
    slots[mat.name] = [u, v]
    tint = mat.diffuse_color[:3]
    textures = [n.image for n in mat.node_tree.nodes if n.type == 'TEX_IMAGE']
    source = textures[0] if textures else None
    source_pixels = list(source.pixels) if source else None
    for y in range(64):
        for x in range(64):
            rgb = list(tint)
            if source:
                sx, sy = 20 + x % 8, source.size[1] - 1 - (20 + y % 10)
                pos = (sy * source.size[0] + sx) * 4
                rgb = [rgb[c] * source_pixels[pos+c] for c in range(3)]
            # Blender's original colors are linear material factors.
            rgb = [12.92*c if c <= .0031308 else 1.055*c**(1/2.4)-.055 for c in rgb]
            dest = ((127-v-y)*256+u+x)*4
            pixels[dest:dest+4] = [*rgb, 1.0]
atlas.pixels.foreach_set(pixels)
atlas.filepath_raw = str(ROOT/'src/main/resources/assets/mathmaster/textures/entity/nine.png')
Path(atlas.filepath_raw).parent.mkdir(parents=True, exist_ok=True)
atlas.file_format = 'PNG'
atlas.save()
geometry = []
for ob in parts:
    x,y,z = ob.location
    w,d,h = ob.dimensions
    geometry.append({'name':ob.name,'uv':slots[ob.active_material.name],
                     'box':[x-w/2,-z-h/2,y-d/2,w,h,d]})
(OUT/'cuboids.json').write_text(json.dumps(geometry,indent=2), encoding='utf-8')
print('EXPORTED', len(geometry), 'cuboids', len(materials), 'materials')
