"""Create the MathMaster Eight creature in Blender and export its Minecraft atlas/geometry.

Run with Blender:
  blender --background --python tools/models/create_eight_blender.py
"""
from pathlib import Path
import json
import math

import bpy
from mathutils import Vector


ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "build" / "eight-export"
BLEND = ROOT / "dev-assets/models/eight_v1.blend"
TEXTURE = ROOT / "src/main/resources/assets/mathmaster/textures/entity/eight.png"
IRON_TEXTURE = Path("C:/software/Minecraft/1.21.1-learn/out/resources/assets/minecraft/textures/models/armor/iron_layer_1.png")
OUT.mkdir(parents=True, exist_ok=True)
TEXTURE.parent.mkdir(parents=True, exist_ok=True)


def material(name, color, source=None, metallic=0.0, roughness=0.72):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*color, 1.0)
    mat.use_nodes = True
    shader = mat.node_tree.nodes.get("Principled BSDF")
    shader.inputs["Base Color"].default_value = (*color, 1.0)
    shader.inputs["Metallic"].default_value = metallic
    shader.inputs["Roughness"].default_value = roughness
    if source:
        tex = mat.node_tree.nodes.new("ShaderNodeTexImage")
        tex.image = bpy.data.images.load(str(source), check_existing=True)
        tex.interpolation = "Closest"
        tex.projection = "BOX"
        tex.projection_blend = 0.05
        coord = mat.node_tree.nodes.new("ShaderNodeTexCoord")
        mat.node_tree.links.new(coord.outputs["Generated"], tex.inputs["Vector"])
        mat.node_tree.links.new(tex.outputs["Color"], shader.inputs["Base Color"])
    return mat


def cube(collection, name, center, size, mat, bevel=0.0):
    bpy.ops.mesh.primitive_cube_add(location=center)
    obj = bpy.context.object
    obj.name = name
    obj.dimensions = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    obj.data.materials.append(mat)
    for old in list(obj.users_collection):
        old.objects.unlink(obj)
    collection.objects.link(obj)
    if bevel:
        modifier = obj.modifiers.new("Preview bevel", "BEVEL")
        modifier.width = bevel
        modifier.segments = 1
    return obj


# New scene, with one explicit collection consumed by the exporter.
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
for collection in list(bpy.data.collections):
    if collection.name != "Collection":
        bpy.data.collections.remove(collection)
base = bpy.data.collections.get("Collection")
base.name = "EIGHT - model parts"

leather = material("Leather body", (0.20, 0.19, 0.20), roughness=0.88)
leather_edge = material("Leather edging", (0.34, 0.22, 0.13), roughness=0.82)
iron = material("Iron armor", (0.73, 0.75, 0.76), IRON_TEXTURE, metallic=0.72, roughness=0.38)
iron_dark = material("Iron armor shadow", (0.38, 0.41, 0.43), IRON_TEXTURE, metallic=0.62, roughness=0.46)
socket = material("Eye socket", (0.055, 0.045, 0.035), roughness=0.8)
eye = material("Eye white", (0.90, 0.84, 0.68), roughness=0.5)
pupil = material("Pupil", (0.12, 0.07, 0.04), roughness=0.65)

# Symmetric 8 silhouette: two clear holes, same 16x32x4 Minecraft-pixel envelope as Nine.
core = [
    ("01 crown", (0.0, 0.0, 29.5), (16.0, 4.0, 5.0)),
    ("02 upper left", (-6.0, 0.0, 23.0), (4.0, 4.0, 9.0)),
    ("03 upper right", (6.0, 0.0, 23.0), (4.0, 4.0, 9.0)),
    ("04 waist", (0.0, 0.0, 16.0), (16.0, 4.0, 5.0)),
    ("05 lower left", (-6.0, 0.0, 8.0), (4.0, 4.0, 11.0)),
    ("06 lower right", (6.0, 0.0, 8.0), (4.0, 4.0, 11.0)),
    ("07 sole", (0.0, 0.0, 1.25), (16.0, 4.0, 2.5)),
]
for name, center, size in core:
    cube(base, name, center, size, leather, 0.12)

# Brown leather seams echo Nine; iron plates resemble a fitted iron-armor harness.
decor = [
    ("Leather crown rim", (0.0, -2.035, 31.55), (16.0, 0.07, 0.9), leather_edge),
    ("Leather waist strap", (0.0, -2.045, 16.0), (16.0, 0.09, 1.1), leather_edge),
    ("Leather sole rim", (0.0, -2.04, 0.55), (16.0, 0.08, 0.7), leather_edge),
    ("Iron crown plate", (0.0, -2.09, 29.5), (13.8, 0.12, 2.7), iron),
    ("Iron waist plate", (0.0, -2.10, 16.2), (13.8, 0.14, 2.1), iron),
    ("Iron sole plate", (0.0, -2.09, 1.45), (13.8, 0.12, 1.2), iron),
    ("Iron upper left guard", (-7.18, -2.10, 23.0), (1.35, 0.14, 6.8), iron_dark),
    ("Iron upper right guard", (7.18, -2.10, 23.0), (1.35, 0.14, 6.8), iron_dark),
    ("Iron lower left guard", (-7.18, -2.10, 8.0), (1.35, 0.14, 8.8), iron_dark),
    ("Iron lower right guard", (7.18, -2.10, 8.0), (1.35, 0.14, 8.8), iron_dark),
]
for name, center, size, mat in decor:
    cube(base, name, center, size, mat, 0.04)

# Expressive eyes sit inside the armored crown, matching Nine's face language.
for x in (-3.5, 3.5):
    cube(base, f"Eye socket {x:+.1f}", (x, -2.16, 29.45), (3.6, 0.08, 2.8), socket, 0.04)
    cube(base, f"Eye white {x:+.1f}", (x, -2.22, 29.55), (3.0, 0.04, 2.25), eye, 0.02)
    cube(base, f"Pupil {x:+.1f}", (x + (0.45 if x < 0 else -0.45), -2.26, 29.35), (0.9, 0.03, 1.55), pupil)

# Small leather stitches across the waist and sole.
for row_name, z, count in (("Waist stitch", 15.25, 8), ("Sole stitch", 0.55, 8)):
    for index in range(count):
        cube(base, f"{row_name}.{index:03d}", (-7.0 + index * 2.0, -2.105, z),
             (0.55, 0.035, 0.24), leather_edge)

# Generate a Minecraft box-UV atlas. Iron slots reuse actual vanilla iron armor pixels.
materials = list(dict.fromkeys(obj.active_material for obj in base.objects if obj.type == "MESH"))
atlas = bpy.data.images.new("eight", width=256, height=128, alpha=True)
pixels = [0.0] * (256 * 128 * 4)
slots = {}
iron_image = bpy.data.images.load(str(IRON_TEXTURE), check_existing=True)
iron_pixels = list(iron_image.pixels)
iron_samples = []
for y in range(iron_image.size[1]):
    for x in range(iron_image.size[0]):
        pos = (y * iron_image.size[0] + x) * 4
        if iron_pixels[pos + 3] > 0.5:
            iron_samples.append(tuple(iron_pixels[pos:pos + 3]))

for index, mat in enumerate(materials):
    u, v = (index % 4) * 64, (index // 4) * 64
    slots[mat.name] = [u, v]
    tint = tuple(mat.diffuse_color[:3])
    for y in range(64):
        for x in range(64):
            rgb = tint
            if mat.name.startswith("Iron") and iron_samples:
                sample = iron_samples[(x * 7 + y * 13) % len(iron_samples)]
                rgb = tuple(min(1.0, sample[c] * (0.82 + tint[c] * 0.28)) for c in range(3))
            elif mat.name.startswith("Leather"):
                factor = (0.86, 1.0, 1.10, 0.94)[(x + y * 3) % 4]
                rgb = tuple(min(1.0, c * factor) for c in tint)
            dest = ((127 - v - y) * 256 + u + x) * 4
            pixels[dest:dest + 4] = [*rgb, 1.0]
atlas.pixels.foreach_set(pixels)
atlas.filepath_raw = str(TEXTURE)
atlas.file_format = "PNG"
atlas.save()

geometry = []
for obj in sorted((item for item in base.objects if item.type == "MESH"), key=lambda item: item.name):
    x, y, z = obj.location
    w, d, h = obj.dimensions
    geometry.append({
        "name": obj.name,
        "uv": slots[obj.active_material.name],
        "box": [x - w / 2, -z - h / 2, y - d / 2, w, h, d],
        "material": obj.active_material.name,
    })
(OUT / "cuboids.json").write_text(json.dumps(geometry, indent=2), encoding="utf-8")

# Camera and lights remain in the .blend for inspection; they are excluded from model export.
bpy.ops.object.camera_add(location=(28.0, -70.0, 25.0))
camera = bpy.context.object
camera.name = "Preview Camera"
camera.rotation_euler = ((Vector((0.0, 0.0, 16.0)) - camera.location).to_track_quat("-Z", "Y").to_euler())
camera.data.type = "ORTHO"
camera.data.ortho_scale = 39.0
bpy.context.scene.camera = camera
for name, location, energy, size in (
    ("Key", (-24.0, -35.0, 45.0), 950.0, 18.0),
    ("Fill", (25.0, -20.0, 22.0), 650.0, 15.0),
    ("Rim", (0.0, 18.0, 32.0), 850.0, 12.0),
):
    bpy.ops.object.light_add(type="AREA", location=location)
    light = bpy.context.object
    light.name = name
    light.data.energy = energy
    light.data.shape = "DISK"
    light.data.size = size
    light.rotation_euler = ((Vector((0.0, 0.0, 16.0)) - light.location).to_track_quat("-Z", "Y").to_euler())

scene = bpy.context.scene
scene.render.engine = "BLENDER_EEVEE"
scene.render.resolution_x = 512
scene.render.resolution_y = 512
scene.render.resolution_percentage = 100
scene.render.film_transparent = True
scene.render.image_settings.file_format = "PNG"
scene.render.filepath = str(OUT / "eight_preview.png")
scene.world.color = (0.025, 0.025, 0.035)
scene.view_settings.exposure = 1.0

bpy.ops.wm.save_as_mainfile(filepath=str(BLEND))
bpy.ops.render.render(write_still=True)
print(f"CREATED {BLEND}")
print(f"EXPORTED {len(geometry)} cuboids to {TEXTURE}")
