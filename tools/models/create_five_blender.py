"""Build the editable Number 5 source, fused texture atlas, Java model, and preview."""
from pathlib import Path
from mathutils import Vector
import bpy
import json
import math
import random
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "build" / "five-export"
OUT.mkdir(parents=True, exist_ok=True)
TEXTURE = ROOT / "src/main/resources/assets/mathmaster/textures/entity/five.png"
MODEL = ROOT / "src/main/java/com/freshfish/mathmaster/client/entity/FiveModel.java"
CLIENT_JAR = Path(r"C:\Users\31375\.gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar")
TEMP = OUT / "texture-sources"
TEMP.mkdir(exist_ok=True)

bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
scene = bpy.context.scene
parts = bpy.data.collections.new("Number 5 - fused organism")
scene.collection.children.link(parts)

# Mix transformed fragments from existing digital creatures and recognizable vanilla species.
vanilla_paths = [
    "assets/minecraft/textures/entity/zombie/zombie.png",
    "assets/minecraft/textures/entity/cow/cow.png",
    "assets/minecraft/textures/entity/pig/pig.png",
    "assets/minecraft/textures/entity/spider/spider.png",
    "assets/minecraft/textures/entity/warden/warden.png",
    "assets/minecraft/textures/entity/guardian.png",
]
source_files = [ROOT / f"src/main/resources/assets/mathmaster/textures/entity/{name}.png"
                for name in ("six", "seven", "eight", "nine")]
with zipfile.ZipFile(CLIENT_JAR) as jar:
    for entry in vanilla_paths:
        target = TEMP / Path(entry).name
        target.write_bytes(jar.read(entry))
        source_files.append(target)

images = []
for path in source_files:
    image = bpy.data.images.load(str(path), check_existing=False)
    image.colorspace_settings.name = "sRGB"
    images.append(image)

size = 1024
cell = 256
atlas = bpy.data.images.new("Five fused atlas", width=size, height=size, alpha=True)
pixels = [0.0] * (size * size * 4)
rng = random.Random(5005)
for cy in range(4):
    for cx in range(4):
        primary = images[(cx + cy * 3) % len(images)]
        secondary = images[(cx * 5 + cy + 4) % len(images)]
        pw, ph = primary.size
        sw, sh = secondary.size
        tint = ((0.43, 0.10, 0.12), (0.30, 0.16, 0.15),
                (0.23, 0.23, 0.20), (0.18, 0.28, 0.27))[(cx + cy) % 4]
        pp = list(primary.pixels)
        sp = list(secondary.pixels)
        for y in range(cell):
            for x in range(cell):
                # Coarse sampling preserves the pixel-art character when enlarged.
                sx1 = ((x // 4) * 4 + cx * 7) % pw
                sy1 = ((y // 4) * 4 + cy * 11) % ph
                sx2 = ((x // 8) * 8 + cy * 5) % sw
                sy2 = ((y // 8) * 8 + cx * 13) % sh
                i1 = (sy1 * pw + sx1) * 4
                i2 = (sy2 * sw + sx2) * 4
                seam = ((x + 2 * y + cx * 31) % 71) < 9
                mix = 0.62 if seam else 0.32
                rgb = []
                for k in range(3):
                    sampled = pp[i1 + k] * (1.0 - mix) + sp[i2 + k] * mix
                    rgb.append(max(0.0, min(1.0, sampled * 0.48 + tint[k] * 0.52)))
                if ((x * 3 + y * 5 + cx * 17 + cy * 29) % 173) < 3:
                    rgb = [0.72, 0.06, 0.08]  # sparse blood vessels/digital scars
                dest = (((size - 1) - (cy * cell + y)) * size + cx * cell + x) * 4
                pixels[dest:dest + 4] = [*rgb, 1.0]
atlas.pixels.foreach_set(pixels)
atlas.filepath_raw = str(TEXTURE)
atlas.file_format = "PNG"
atlas.save()
atlas.pack()

material = bpy.data.materials.new("Fused digital flesh")
material.use_nodes = True
shader = material.node_tree.nodes.get("Principled BSDF")
shader.inputs["Roughness"].default_value = 0.82
texture_node = material.node_tree.nodes.new("ShaderNodeTexImage")
texture_node.image = atlas
texture_node.interpolation = "Closest"
material.node_tree.links.new(texture_node.outputs["Color"], shader.inputs["Base Color"])

groups = {name: [] for name in ("body", "tentacle_nw", "tentacle_ne", "tentacle_sw", "tentacle_se", "eye_stalk")}

def add_box(group, name, x, y, z, w, h, d, slot):
    # Blender uses Y depth/Z height; Java values are Minecraft model pixels.
    bpy.ops.mesh.primitive_cube_add(size=1, location=(x / 16, z / 16, -y / 16))
    obj = bpy.context.object
    obj.name = name
    obj.dimensions = (w / 16, d / 16, h / 16)
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    for collection in list(obj.users_collection):
        collection.objects.unlink(obj)
    parts.objects.link(obj)
    obj.data.materials.append(material)
    u, v = (slot % 4) * cell, (slot // 4) * cell
    # Keep every Blender face inside the same atlas cell used by the Java cuboid.
    for polygon in obj.data.polygons:
        for loop_index in polygon.loop_indices:
            coordinate = obj.data.vertices[obj.data.loops[loop_index].vertex_index].co
            if abs(polygon.normal.z) > 0.5:
                a, b, aw, bh = coordinate.x, coordinate.y, w / 16, d / 16
            elif abs(polygon.normal.x) > 0.5:
                a, b, aw, bh = coordinate.y, coordinate.z, d / 16, h / 16
            else:
                a, b, aw, bh = coordinate.x, coordinate.z, w / 16, h / 16
            uu = 0.08 + (a + aw / 2) / max(aw, 0.01) * 0.84
            vv = 0.08 + (b + bh / 2) / max(bh, 0.01) * 0.84
            obj.data.uv_layers.active.data[loop_index].uv = ((u + uu * cell) / size,
                                                            1.0 - (v + vv * cell) / size)
    # Java's unfolded cube UV can be wider than one visual source cell. Two 512px
    # columns keep every large cuboid inside the 1024px atlas without clamping.
    groups[group].append({"name": name, "uv": [(slot % 2) * 512, ((slot // 2) % 4) * cell],
                          "box": [x - w / 2, y - h / 2, z - d / 2, w, h, d]})

# A 42x42-block quilt, deliberately uneven so the silhouette is living tissue rather than a square.
for iz in range(6):
    for ix in range(6):
        if (ix, iz) in ((0, 0), (5, 0), (0, 5), (5, 5)):
            continue
        height = (26, 32, 38)[(ix * 2 + iz) % 3]
        x = (ix - 2.5) * 112
        z = (iz - 2.5) * 112
        add_box("body", f"graft_{ix}_{iz}", x, -20 + (38 - height) / 2, z,
                116, height, 116, (ix + iz * 3) % 16)

# Each corner has a segmented, downward-curving pseudopod.
for group, sx, sz, slot in (("tentacle_nw", -1, -1, 3), ("tentacle_ne", 1, -1, 7),
                            ("tentacle_sw", -1, 1, 11), ("tentacle_se", 1, 1, 15)):
    for i in range(4):
        add_box(group, f"{group}_{i}", sx * (300 + i * 34), -7 + i * 15,
                sz * (300 + i * 30), 92 - i * 12, 54 - i * 7, 80 - i * 10, slot - i % 3)

# A long side feeler carries two unrelated eyes and never forms a numeral.
for i in range(5):
    add_box("eye_stalk", f"eye_stalk_{i}", 330 + i * 70, -13 - i * 7, 35 + i * 13,
            100 - i * 10, 58 - i * 6, 70 - i * 7, (8 + i) % 16)
add_box("eye_stalk", "eye_socket_left", 635, -58, 20, 56, 50, 30, 12)
add_box("eye_stalk", "eye_socket_right", 635, -58, 74, 56, 50, 30, 12)
add_box("eye_stalk", "eye_left", 652, -60, 20, 22, 24, 12, 13)
add_box("eye_stalk", "eye_right", 652, -60, 74, 22, 24, 12, 13)

(OUT / "cuboids.json").write_text(json.dumps(groups, indent=2), encoding="utf-8")

def group_source(name):
    variable = "cubes_" + name
    lines = [f"        CubeListBuilder {variable} = CubeListBuilder.create();\n"]
    for part in groups[name]:
        values = ", ".join(f"{v:.3f}F" for v in part["box"])
        lines.append(f"        {variable}.texOffs({part['uv'][0]}, {part['uv'][1]}).addBox({values});\n")
    lines.append(f'        root.addOrReplaceChild("{name}", {variable}, PartPose.ZERO);\n')
    return "".join(lines)

java = '''package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.FiveEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Cuboid model exported from the editable Blender Number 5 source. */
public final class FiveModel extends HierarchicalModel<FiveEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "five"), "main");
    private final ModelPart root;
    private final ModelPart[] tentacles;
    private final ModelPart eyeStalk;

    public FiveModel(ModelPart root) {
        this.root = root;
        this.tentacles = new ModelPart[] {root.getChild("tentacle_nw"), root.getChild("tentacle_ne"),
                root.getChild("tentacle_sw"), root.getChild("tentacle_se")};
        this.eyeStalk = root.getChild("eye_stalk");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
''' + "".join(group_source(n) for n in groups) + '''        return LayerDefinition.create(mesh, 1024, 1024);
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(FiveEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        for (int i = 0; i < tentacles.length; i++) {
            tentacles[i].xRot = Mth.sin(ageInTicks * 0.035F + i * 1.7F) * 0.055F;
            tentacles[i].zRot = Mth.cos(ageInTicks * 0.028F + i * 1.3F) * 0.045F;
        }
        eyeStalk.yRot = Mth.sin(ageInTicks * 0.021F) * 0.07F;
        eyeStalk.zRot = Mth.cos(ageInTicks * 0.026F) * 0.025F;
    }
}
'''
MODEL.write_text(java, encoding="utf-8")

# Preview uses the exact atlas and geometry saved into the .blend source.
bpy.ops.object.camera_add(location=(58, -70, 56))
camera = bpy.context.object
camera.rotation_euler = (Vector((0, 0, 0)) - camera.location).to_track_quat("-Z", "Y").to_euler()
camera.data.type = "ORTHO"
camera.data.ortho_scale = 73
scene.camera = camera
scene.world.color = (0.006, 0.003, 0.004)
for location, energy, color in (((-35, -40, 70), 90000, (1.0, 0.20, 0.17)),
                                ((65, -20, 35), 65000, (0.13, 0.55, 0.49)),
                                ((0, 50, 30), 50000, (0.33, 0.03, 0.04))):
    bpy.ops.object.light_add(type="AREA", location=location)
    light = bpy.context.object
    light.data.energy = energy
    light.data.color = color
    light.data.size = 40
    light.rotation_euler = (Vector((0, 0, 0)) - light.location).to_track_quat("-Z", "Y").to_euler()
scene.render.engine = "BLENDER_EEVEE"
scene.render.resolution_x = 900
scene.render.resolution_y = 720
scene.render.resolution_percentage = 100
scene.render.film_transparent = True
scene.render.image_settings.file_format = "PNG"
scene.render.filepath = str(OUT / "five_preview.png")
scene.view_settings.view_transform = "Standard"
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT / "dev-assets/models/five_v1.blend"))
bpy.ops.render.render(write_still=True)
print(f"Exported Number 5: {sum(map(len, groups.values()))} cuboids")
