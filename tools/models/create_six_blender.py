"""Build the organ-stitched Six model, texture, preview, and Java cuboids.

Run: blender --background --factory-startup --python tools/models/create_six_blender.py
Only Six files and build/six-export are written.
"""

from pathlib import Path
import json
import math
import random

import bpy
from mathutils import Vector


ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "build/six-export"
TEXTURE = ROOT / "src/main/resources/assets/mathmaster/textures/entity/six.png"
MODEL = ROOT / "src/main/java/com/freshfish/mathmaster/client/entity/SixModel.java"
OUT.mkdir(parents=True, exist_ok=True)
TEXTURE.parent.mkdir(parents=True, exist_ok=True)
MODEL.parent.mkdir(parents=True, exist_ok=True)

bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
parts = bpy.data.collections.new("SIX - failed flesh conversion")
bpy.context.scene.collection.children.link(parts)

# Pixel units: roughly 22 wide x 48 tall x 12 deep, close to Warden height.
specs = []


def box(name, x, z, w, h, material, y=0.0, depth=10.0):
    specs.append((name, (x, y, z), (w, depth, h), material))


# The primary silhouette remains a readable 6, assembled from uneven organ masses.
box("01 upper brain lobe", 3.5, 44.0, 13.0, 7.0, "flesh", depth=11.0)
box("02 upper torn tip", 8.5, 40.0, 6.0, 5.0, "muscle", y=0.5, depth=9.0)
box("03 skull curl", -4.0, 41.0, 8.0, 8.0, "fat", y=-0.3, depth=10.5)
box("04 neck knot", -7.0, 35.0, 7.0, 7.0, "tendon", y=0.4, depth=11.5)
box("05 left lung", -7.5, 28.0, 8.0, 8.0, "flesh", y=-0.5, depth=12.0)
box("06 liver spine", -7.0, 21.0, 8.0, 7.0, "liver", y=0.4, depth=10.5)
box("07 hip graft", -6.0, 14.0, 9.0, 8.0, "muscle", y=-0.2, depth=11.5)
box("08 upper bowel bridge", 0.0, 24.0, 10.0, 7.0, "flesh", y=0.2, depth=10.0)
box("09 right lung", 6.5, 21.0, 8.0, 9.0, "organ", y=-0.4, depth=12.0)
box("10 right kidney", 8.0, 13.0, 7.0, 8.0, "liver", y=0.4, depth=10.5)
box("11 lower right bowel", 5.5, 7.0, 9.0, 7.0, "muscle", y=-0.2, depth=11.5)
box("12 lower intestine", -0.5, 4.0, 12.0, 7.0, "flesh", y=0.4, depth=12.0)
box("13 lower left bowel", -6.5, 7.0, 8.0, 7.0, "fat", y=-0.3, depth=10.5)

# Protruding organs and stitches break up the otherwise regular digital body.
box("14 exposed heart", -0.5, 30.0, 6.0, 7.0, "heart", y=-5.7, depth=2.2)
box("15 heart lower chamber", 0.5, 26.5, 4.0, 4.0, "heart_dark", y=-6.0, depth=2.0)
box("16 stomach pouch", 5.0, 16.5, 5.0, 6.0, "fat", y=-6.0, depth=2.0)
box("17 rear organ cyst", -4.0, 18.0, 6.0, 7.0, "organ", y=5.8, depth=2.5)
box("18 shoulder cyst", 5.5, 36.0, 5.0, 6.0, "organ", y=5.7, depth=2.4)
for index, (x, z, width) in enumerate(((-7.0, 31.5, 5.0), (-7.0, 24.5, 5.5),
                                       (-4.5, 10.5, 5.0), (1.0, 5.0, 6.0),
                                       (7.0, 10.5, 4.5), (7.0, 18.0, 4.5))):
    box(f"19 intestine segment {index}", x, z, width, 2.2, "intestine", y=-5.8, depth=1.5)
for index, (x, z, width) in enumerate(((-6.5, 37.0, 5.0), (-6.5, 17.0, 5.5),
                                       (-1.0, 8.0, 5.5), (6.5, 14.0, 4.0))):
    box(f"20 tendon stitch {index}", x, z, width, 0.8, "tendon", y=-6.7, depth=0.35)
for index, (x, z, height) in enumerate(((-9.5, 26.0, 11.0), (2.0, 41.0, 7.0),
                                        (9.5, 15.0, 8.0), (1.0, 4.0, 5.0))):
    box(f"21 raised vein {index}", x, z, 0.75, height, "vein", y=-6.75, depth=0.3)

# Two recessed, fully red eyes sit in the upper graft.
for index, x in enumerate((-1.6, 3.2)):
    box(f"22 eye socket {index}", x, 42.6, 3.5, 3.4, "socket", y=-6.0, depth=0.7)
    box(f"23 blood eye {index}", x, 42.6, 2.35, 2.35, "eye", y=-6.5, depth=0.35)
    box(f"24 black pupil {index}", x + (0.25 if x < 0 else -0.25), 42.5,
        0.65, 1.2, "pupil", y=-6.72, depth=0.15)

palette = {
    "flesh": (0.57, 0.17, 0.14),
    "muscle": (0.43, 0.055, 0.065),
    "organ": (0.31, 0.075, 0.12),
    "fat": (0.76, 0.43, 0.32),
    "tendon": (0.69, 0.33, 0.28),
    "liver": (0.25, 0.035, 0.04),
    "heart": (0.68, 0.025, 0.035),
    "heart_dark": (0.34, 0.01, 0.02),
    "intestine": (0.68, 0.27, 0.26),
    "vein": (0.25, 0.012, 0.09),
    "socket": (0.055, 0.002, 0.004),
    "eye": (1.0, 0.005, 0.008),
    "pupil": (0.12, 0.0, 0.0),
}
slots = {name: ((i % 4) * 64, (i // 4) * 64) for i, name in enumerate(palette)}
atlas = bpy.data.images.new("Six flesh atlas", width=256, height=256, alpha=True)
pixels = [0.0] * (256 * 256 * 4)
rng = random.Random(606)
for name, base in palette.items():
    u, v = slots[name]
    cell_noise = [[rng.uniform(-0.12, 0.12) for _ in range(8)] for _ in range(8)]
    for y in range(64):
        for x in range(64):
            mottled = cell_noise[y // 8][x // 8]
            fibers = 0.045 * math.sin((x * 0.65 + y * 0.22) + len(name))
            factor = 1.0 + mottled + (fibers if name not in ("eye", "pupil", "socket") else 0.0)
            rgb = [max(0.0, min(1.0, c * factor)) for c in base]
            if name == "eye" and (x + y) % 11 == 0:
                rgb = [1.0, 0.19, 0.12]
            dest = ((255 - v - y) * 256 + u + x) * 4
            pixels[dest:dest + 4] = [*rgb, 1.0]
atlas.pixels.foreach_set(pixels)
atlas.filepath_raw = str(TEXTURE)
atlas.file_format = "PNG"
atlas.save()
atlas.pack()

material = bpy.data.materials.new("Six - wet stitched flesh")
material.use_nodes = True
shader = material.node_tree.nodes.get("Principled BSDF")
shader.inputs["Roughness"].default_value = 0.58
shader.inputs["Metallic"].default_value = 0.0
texture_node = material.node_tree.nodes.new("ShaderNodeTexImage")
texture_node.image = atlas
texture_node.interpolation = "Closest"
material.node_tree.links.new(texture_node.outputs["Color"], shader.inputs["Base Color"])

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
    obj.data.materials.append(material)
    width, depth, height = size
    u, v = slots[kind]
    uv = obj.data.uv_layers.active.data
    for face in obj.data.polygons:
        for loop_index in face.loop_indices:
            coordinate = obj.data.vertices[obj.data.loops[loop_index].vertex_index].co
            if abs(face.normal.y) > 0.5:
                a, b = coordinate.x + width / 2, height / 2 - coordinate.z
                du, dv = depth, depth
            elif abs(face.normal.x) > 0.5:
                a, b = coordinate.y + depth / 2, height / 2 - coordinate.z
                du, dv = 0, depth
            else:
                a, b = coordinate.x + width / 2, coordinate.y + depth / 2
                du, dv = depth, 0
            uv[loop_index].uv = ((u + du + a) / 256, 1 - (v + dv + b) / 256)
    x, y, z = center
    geometry.append({"name": name, "uv": [u, v],
                     "box": [x - width / 2, -z - height / 2, y - depth / 2,
                             width, height, depth]})

(OUT / "cuboids.json").write_text(json.dumps(geometry, indent=2), encoding="utf-8")

lines = []
for item in geometry:
    values = ", ".join(f"{value:.3f}F" for value in item["box"])
    lines.append(
        f"        cubes.texOffs({item['uv'][0]}, {item['uv'][1]}).addBox({values}); // {item['name']}\n"
    )
model_source = f'''package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.SixEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Vanilla cuboid model exported from the Blender Six v1 source. */
public final class SixModel extends HierarchicalModel<SixEntity> {{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "six"), "main");
    private final ModelPart root;
    private final ModelPart body;

    public SixModel(ModelPart root) {{
        this.root = root;
        this.body = root.getChild("body");
    }}

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder cubes = CubeListBuilder.create();
{''.join(lines)}        mesh.getRoot().addOrReplaceChild("body", cubes, PartPose.offset(0, 24, 0));
        return LayerDefinition.create(mesh, 256, 256);
    }}

    @Override
    public ModelPart root() {{
        return root;
    }}

    @Override
    public void setupAnim(SixEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {{
        body.resetPose();
        // This broad, three-dimensional body follows the entity's body yaw as one mass.
        body.yRot = 0.0F;
        float movement = Math.min(limbSwingAmount, 0.65F);
        body.zRot = Mth.sin(limbSwing * 1.15F) * movement * 0.075F;
        body.y -= Math.abs(Mth.sin(limbSwing * 1.15F)) * movement * 0.8F;
        body.xScale = 1.0F + Mth.sin(ageInTicks * 0.11F) * 0.008F;
        body.zScale = 1.0F - Mth.sin(ageInTicks * 0.11F) * 0.006F;
    }}
}}
'''
MODEL.write_text(model_source, encoding="utf-8")

bpy.ops.object.camera_add(location=(34, -105, 34))
camera = bpy.context.object
camera.rotation_euler = (Vector((0, 0, 24)) - camera.location).to_track_quat("-Z", "Y").to_euler()
camera.data.type = "ORTHO"
camera.data.ortho_scale = 58
scene = bpy.context.scene
scene.camera = camera
scene.world.color = (0.006, 0.002, 0.003)
for location, energy, color in (
        ((-30, -45, 65), 38000, (1.0, 0.24, 0.19)),
        ((35, -30, 38), 26000, (0.8, 0.055, 0.07)),
        ((0, 25, 42), 30000, (0.20, 0.015, 0.035))):
    bpy.ops.object.light_add(type="AREA", location=location)
    light = bpy.context.object
    light.data.energy = energy
    light.data.color = color
    light.data.shape = "DISK"
    light.data.size = 28
    light.rotation_euler = (Vector((0, 0, 24)) - light.location).to_track_quat("-Z", "Y").to_euler()
scene.render.engine = "BLENDER_EEVEE"
scene.render.resolution_x = 640
scene.render.resolution_y = 760
scene.render.resolution_percentage = 100
scene.render.film_transparent = True
scene.render.image_settings.file_format = "PNG"
scene.render.filepath = str(OUT / "six_preview.png")
scene.view_settings.view_transform = "Standard"
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT / "dev-assets/models/six_v1.blend"))
bpy.ops.render.render(write_still=True)
print(f"Exported Six: {len(geometry)} cuboids, packed Blender file, atlas, Java model and preview")
