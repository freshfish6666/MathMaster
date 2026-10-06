"""Generate the approved floating Geometry Holder, without entity/AI registration.

Run with Blender --background --factory-startup --python this_file.
Writes only this boss's versioned sources, texture, Java model and build previews.
Coordinates are model pixels: X right, Y up, Z toward back (front is negative Z).
"""
from pathlib import Path
import base64
import json
import math
import random
import uuid

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "build/geometry-holder-export"
EDIT = ROOT / "dev-assets/models"
TEX = ROOT / "src/main/resources/assets/mathmaster/textures/entity/geometry_holder.png"
JAVA = ROOT / "src/main/java/com/freshfish/mathmaster/client/entity/GeometryHolderModel.java"
for folder in (OUT, EDIT, TEX.parent, JAVA.parent):
    folder.mkdir(parents=True, exist_ok=True)

bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
scene = bpy.context.scene
palette = {"stone": (.31, .48, .52), "light": (.48, .67, .70),
           "dark": (.16, .28, .32), "void": (.009, .023, .03),
           "cyan": (.12, .85, .95), "trim": (.38, .58, .62)}
slots = {name: ((i % 4) * 64, (i // 4) * 64) for i, name in enumerate(palette)}
atlas = bpy.data.images.new("Geometry Holder pixel atlas", width=256, height=256, alpha=True)
pixels = [0.0] * (256 * 256 * 4)
rng = random.Random(130004)
for kind, color in palette.items():
    u, v = slots[kind]
    cells = [[rng.uniform(.76, 1.18) for _ in range(32)] for _ in range(32)]
    for y in range(64):
        for x in range(64):
            factor = 1 if kind in ("void", "cyan") else cells[y // 2][x // 2]
            offset = ((255 - v - y) * 256 + u + x) * 4
            pixels[offset:offset + 4] = [*(min(1, c * factor) for c in color), 1]
atlas.pixels.foreach_set(pixels)
atlas.filepath_raw = str(TEX)
atlas.file_format = "PNG"
atlas.save()
atlas.pack()
emissive = bpy.data.images.new("Geometry Holder emissive mask", width=256, height=256, alpha=True)
glow_pixels = [0.0] * len(pixels)
u, v = slots["cyan"]
for y in range(64):
    for x in range(64):
        offset = ((255-v-y)*256+u+x)*4
        glow_pixels[offset:offset+4] = pixels[offset:offset+4]
emissive.pixels.foreach_set(glow_pixels)
emissive.filepath_raw = str(TEX.with_name("geometry_holder_emissive.png"))
emissive.file_format = "PNG"
emissive.save()
materials = {}
for kind in palette:
    mat = bpy.data.materials.new(kind)
    mat.use_nodes = True
    shader = mat.node_tree.nodes.get("Principled BSDF")
    shader.inputs["Roughness"].default_value = .9
    tex = mat.node_tree.nodes.new("ShaderNodeTexImage")
    tex.image = atlas
    tex.interpolation = "Closest"
    mat.node_tree.links.new(tex.outputs["Color"], shader.inputs["Base Color"])
    if kind == "cyan":
        mat.node_tree.links.new(tex.outputs["Color"], shader.inputs["Emission Color"])
        shader.inputs["Emission Strength"].default_value = .65
    materials[kind] = mat

bones = {}
elements = []
def bone(name, origin, parent="body"):
    bones[name] = {"origin": list(origin), "parent": parent, "cubes": []}

bone("body", (0, 30, 0), None)
bone("head", (0, 43, 0))
bone("halo", (0, 46, 7))
for side, sign in (("left", -1), ("right", 1)):
    bone(side + "_arm", (sign * 10, 38, 0))
    bone(side + "_forearm", (sign * 14, 30, -2), side + "_arm")
    bone(side + "_hand", (sign * 11, 27, -9), side + "_forearm")
bone("robe_center", (0, 29, 0))
bone("robe_left", (-6, 27, 0))
bone("robe_right", (6, 27, 0))
bone("robe_back", (0, 29, 4))
bone("core", (0, 32, -13))

def box(name, group, center, size, kind="stone"):
    x, y, z = center
    w, h, d = size
    minimum = [x - w / 2, y - h / 2, z - d / 2]
    maximum = [x + w / 2, y + h / 2, z + d / 2]
    u, v = slots[kind]
    # Standard Minecraft box UV unfolding, shared by Java and Blockbench.
    face_uv = {"west": [u, v+d, u+d, v+d+h],
               "north": [u+d, v+d, u+d+w, v+d+h],
               "east": [u+d+w, v+d, u+d+w+d, v+d+h],
               "south": [u+d+w+d, v+d, u+2*d+2*w, v+d+h],
               "up": [u+d, v, u+d+w, v+d],
               "down": [u+d+w, v+d, u+d+2*w, v]}
    assert max(2*d+2*w, d+h) <= 64, name
    element = {"name": name, "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "mathmaster:geometry_holder/" + name)),
               "type": "cube", "from": minimum, "to": maximum, "origin": list(center),
               "rotation": [0, 0, 0], "box_uv": False,
               "faces": {face: {"uv": uv, "texture": 0} for face, uv in face_uv.items()}}
    elements.append(element)
    bones[group]["cubes"].append({"name": name, "center": list(center), "size": list(size), "material": kind,
                                   "uv": [u, v], "uuid": element["uuid"]})
    # Blender X right, Y toward back, Z up.
    bpy.ops.mesh.primitive_cube_add(size=1, location=(x, z, y))
    obj = bpy.context.object
    obj.name = group + "/" + name
    obj.dimensions = (w, d, h)
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    obj.data.materials.append(materials[kind])
    obj["model_bone"] = group
    obj["model_units"] = "16 pixels = 1 block"
    uv_data = obj.data.uv_layers.active.data
    for poly in obj.data.polygons:
        n = poly.normal
        face = ("east" if n.x > 0 else "west") if abs(n.x) > .5 else (
            ("south" if n.y > 0 else "north") if abs(n.y) > .5 else ("up" if n.z > 0 else "down"))
        a, b, c, e = face_uv[face]
        for loop in poly.loop_indices:
            p = obj.data.vertices[obj.data.loops[loop].vertex_index].co
            if face == "north": sx, sy = p.x/w+.5, .5-p.z/h
            elif face == "south": sx, sy = .5-p.x/w, .5-p.z/h
            elif face == "east": sx, sy = p.y/d+.5, .5-p.z/h
            elif face == "west": sx, sy = .5-p.y/d, .5-p.z/h
            else: sx, sy = p.x/w+.5, p.y/d+.5
            uv_data[loop].uv = ((a+(c-a)*sx)/256, 1-(b+(e-b)*sy)/256)

# Hood: a real empty opening, backed by dark recess rather than a painted face.
box("torso", "body", (0, 36, 0), (14, 12, 9))
box("collar", "body", (0, 41, -2), (16, 3, 11), "light")
box("chest_recess", "body", (0, 36, -4.6), (8, 8, .5), "dark")
box("chest_glow", "body", (0, 36, -5), (1, 7, .5), "cyan")
box("hood_back", "head", (0, 44, 4), (12, 12, 3), "dark")
box("hood_roof", "head", (0, 50, 0), (12, 3, 12), "light")
box("hood_crown", "head", (0, 52, 1), (8, 1, 9), "light")
box("hood_shadow", "head", (0, 44, 2), (8, 10, .5), "void")
for sign in (-1, 1):
    box(f"hood_side_{sign}", "head", (sign*5.5, 44, 0), (3, 11, 11))
    box(f"hood_step_{sign}", "head", (sign*4.5, 48, -5), (3, 3, 2), "light")
    box(f"collar_lip_{sign}", "head", (sign*4, 39, -4.5), (5, 2, 4), "trim")

# Broken square halo, deliberately offset behind the head.
for name, center, size in (
        ("halo_top_left", (-6, 57, 7), (11, 2, 2)),
        ("halo_top_right", (8, 56, 7), (8, 2, 2)),
        ("halo_left_upper", (-14, 51, 7), (2, 9, 2)),
        ("halo_left_lower", (-14, 43, 7), (2, 4, 2)),
        ("halo_right_upper", (14, 52, 7), (2, 5, 2)),
        ("halo_right_lower", (14, 44, 7), (2, 7, 2))):
    box(name, "halo", center, size, "light")
    x,y,z = center; w,h,d = size
    box(name+"_inlay", "halo", (x,y,z-1.1), (max(.5,w-1),max(.5,h-1),.25), "cyan")

# Bent independent arms and open hands frame the weak-point cube without touching it.
for side, sign in (("left", -1), ("right", 1)):
    box(side+"_shoulder", side+"_arm", (sign*10, 38, 0), (7, 7, 9), "light")
    box(side+"_upper_arm", side+"_arm", (sign*12, 33, 0), (4, 7, 6))
    box(side+"_elbow", side+"_forearm", (sign*14, 29, -2), (4, 3, 4), "dark")
    box(side+"_sleeve", side+"_forearm", (sign*14, 26, -5), (6, 6, 10))
    box(side+"_cuff", side+"_forearm", (sign*14, 26, -9), (7, 6, 2), "light")
    box(side+"_palm", side+"_hand", (sign*10.5, 27, -11), (5, 2, 5), "light")
    for digit in range(3):
        box(side+f"_finger_{digit}", side+"_hand", (sign*8, 29, -9-digit*2), (2, 3, 1.5), "trim")
    box(side+"_thumb", side+"_hand", (sign*12, 29, -9), (2, 3, 2))

# Floating robe shards: no legs, no pedestal, visible gaps between lower segments.
box("waist", "body", (0, 29, 0), (12, 3, 9), "dark")
box("belt", "body", (0, 29, -4.8), (14, 2, 1), "light")
box("central_robe", "robe_center", (0, 18, -2), (6, 20, 5))
box("central_robe_tip", "robe_center", (0, 5, -2), (4, 4, 4), "light")
box("robe_rune", "robe_center", (0, 18, -4.6), (1, 18, .25), "cyan")
for side, sign in (("left", -1), ("right", 1)):
    box(side+"_robe_top", "robe_"+side, (sign*7, 23, 0), (4, 10, 7))
    box(side+"_robe_middle", "robe_"+side, (sign*9, 14, 0), (4, 6, 6), "trim")
    box(side+"_robe_tip", "robe_"+side, (sign*9, 7, 0), (3, 4, 4), "light")
box("back_robe", "robe_back", (0, 20, 5), (9, 18, 3))
for sign in (-1, 1):
    box(f"back_strip_{sign}", "robe_back", (sign*4, 16, 7), (2, 20, 2), "trim")

# Twelve-edge hollow cube. This bone is a sibling of the head/arms, not part of a hand.
cx,cy,cz = bones["core"]["origin"]
for axis in range(3):
    for a in (-1, 1):
        for b in (-1, 1):
            offset = [0,0,0]; other = [i for i in range(3) if i != axis]
            offset[other[0]] = a*4; offset[other[1]] = b*4
            size = [1.5,1.5,1.5]; size[axis] = 9.5
            box(f"core_edge_{axis}_{a}_{b}", "core", (cx+offset[0],cy+offset[1],cz+offset[2]), size, "light")
            inner = list(offset)
            for i in other: inner[i] *= .82
            glow_size = [.35,.35,.35]; glow_size[axis] = 6.5
            box(f"core_glow_{axis}_{a}_{b}", "core", (cx+inner[0],cy+inner[1],cz+inner[2]), glow_size, "cyan")

# Parent empties at exported pivots. Transforms remain editable in the .blend.
anchors = {}
for name, data in bones.items():
    anchor = bpy.data.objects.new(name, None)
    scene.collection.objects.link(anchor)
    anchor.empty_display_size = 2
    x,y,z = data["origin"]; anchor.location = (x,z,y)
    anchors[name] = anchor
bpy.context.view_layer.update()
for name, data in bones.items():
    anchor = anchors[name]
    parent = data["parent"]
    if parent:
        world = anchor.matrix_world.copy()
        anchor.parent = anchors[parent]
        anchor.matrix_world = world
        bpy.context.view_layer.update()
for obj in list(scene.objects):
    if obj.type == "MESH" and "model_bone" in obj:
        world = obj.matrix_world.copy()
        obj.parent = anchors[obj["model_bone"]]
        obj.matrix_world = world

# Editable Blockbench Java-entity project with embedded texture and bone hierarchy.
def outliner(name):
    data = bones[name]
    return {"name": name, "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "geometry_holder/bone/"+name)),
            "origin": data["origin"], "rotation": [0,0,0], "export": True,
            "children": [cube["uuid"] for cube in data["cubes"]] +
                        [outliner(child) for child in bones if bones[child]["parent"] == name]}
bb = {"meta": {"format_version": "4.10", "model_format": "modded_entity", "box_uv": False},
      "name": "GeometryHolder", "model_identifier": "geometry_holder", "visible_box": [4,4,2],
      "resolution": {"width": 256, "height": 256}, "elements": elements,
      "outliner": [outliner("body")], "textures": [{"name": TEX.name, "id": "0", "width":256,"height":256,
      "uv_width":256,"uv_height":256,"particle":False,
      "source": "data:image/png;base64,"+base64.b64encode(TEX.read_bytes()).decode()}]}
(EDIT / "geometry_holder_v1.bbmodel").write_text(json.dumps(bb, indent=2), encoding="utf-8")
(OUT / "geometry.json").write_text(json.dumps({"units": "16 pixels per block", "bones": bones}, indent=2), encoding="utf-8")

def f(value): return f"{value:.3f}F"
lines = []
for name, data in bones.items():
    cubes = []
    ox,oy,oz = data["origin"]
    for cube in data["cubes"]:
        x,y,z = cube["center"]; w,h,d = cube["size"]; u,v = cube["uv"]
        values = [x-w/2-ox, -(y+h/2-oy), z-d/2-oz, w,h,d]
        cubes.append(f"                .texOffs({u}, {v}).addBox({', '.join(map(f,values))})")
    parent = data["parent"]
    px,py,pz = bones[parent]["origin"] if parent else (0,24,0)
    lines.append(f'        PartDefinition {name} = {parent or "mesh.getRoot()"}.addOrReplaceChild("{name}", CubeListBuilder.create()\n'+
                 '\n'.join(cubes)+f',\n                PartPose.offset({f(ox-px)}, {f(-(oy-py))}, {f(oz-pz)}));\n')
JAVA.write_text('''package com.freshfish.mathmaster.client.entity;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** Geometry Holder visuals share the entity casting state.
 * Core has a separate pivot; visual geometry does not define collision boxes.
 */
public final class GeometryHolderModel<T extends Entity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("mathmaster", "geometry_holder"), "main");
    private final ModelPart root;
    private int bodyTint = 0xFFFFFFFF;
    @Override public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int overlay, int color) {
        super.renderToBuffer(pose, buffer, light, overlay, net.minecraft.util.FastColor.ARGB32.multiply(color, bodyTint));
    }
    public GeometryHolderModel(ModelPart root) { this.root = root; }
    @Override public ModelPart root() { return root; }
    public ModelPart core() { return root.getChild("body").getChild("core"); }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
''' + ''.join(lines) + '''        return LayerDefinition.create(mesh, 256, 256);
    }
    public void renderChargingHalo(com.mojang.blaze3d.vertex.PoseStack pose,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, int color) {
        pose.pushPose();
        root.translateAndRotate(pose);
        root.getChild("body").translateAndRotate(pose);
        root.getChild("body").getChild("halo").render(pose, buffer, 15728640,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        pose.popPose();
    }
    public void renderChargingCore(com.mojang.blaze3d.vertex.PoseStack pose,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, int color) {
        pose.pushPose(); root.translateAndRotate(pose); root.getChild("body").translateAndRotate(pose);
        core().render(pose,buffer,15728640,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,color);
        pose.popPose();
    }
    public void renderDetachedCore(com.mojang.blaze3d.vertex.PoseStack pose,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int color) {
        var saved=core().storePose(); boolean visible=core().visible;
        core().resetPose(); core().x=core().y=core().z=0; core().xRot=core().yRot=core().zRot=0; core().visible=true;
        core().render(pose,buffer,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        core().loadPose(saved); core().visible=visible;
    }
    @Override public void setupAnim(T entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        ModelPart body = root.getChild("body");
        var holder = entity instanceof com.freshfish.mathmaster.entity.GeometryHolderEntity h ? h : null;
        bodyTint = holder == null ? 0xFFFFFFFF : holder.bodyTint(age - entity.tickCount);
        boolean prison = holder != null && (holder.nearPhase() == com.freshfish.mathmaster.entity.GeometryHolderEntity.PRISON
                || holder.nearPhase() == com.freshfish.mathmaster.entity.GeometryHolderEntity.PRISON_ACTIVE);
        core().visible = holder == null || holder.rangedAttacks().phase() != com.freshfish.mathmaster.entity.GeometryHolderRangedAttacks.BOMB_FLIGHT;
        if (!prison && (holder == null || (!holder.rangedAttacks().active()
                && !holder.ultimateAttack().active()))) {
            body.y += Mth.sin(age * 0.055F) * 0.65F;
            body.zRot = Mth.sin(age * 0.045F) * 0.02F;
            body.xRot = Mth.sin(age * 0.035F) * 0.012F;
        }
        ModelPart head = body.getChild("head");
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(pitch, -20, 20) * Mth.DEG_TO_RAD;
        core().yRot=age*.025F;
        core().y+=Mth.sin(age*.075F)*.4F;
        if(holder!=null && (holder.rangedAttacks().phase()==com.freshfish.mathmaster.entity.GeometryHolderRangedAttacks.CROSS_CHARGE
                || holder.rangedAttacks().phase()==com.freshfish.mathmaster.entity.GeometryHolderRangedAttacks.CROSS_ACTIVE)) {
            core().yRot=0;core().y=-2;
        }
        for (String name : new String[]{"robe_left", "robe_right", "robe_back"})
            body.getChild(name).xRot = Mth.sin(age * 0.04F) * 0.025F;
    }
}
''', encoding="utf-8")

# Plain geometry previews, rendered from the actual exported cuboids.
scene.render.engine = "BLENDER_EEVEE"
scene.render.resolution_x = 800
scene.render.resolution_y = 1000
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = "PNG"
scene.world.use_nodes = True
scene.world.node_tree.nodes.get("Background").inputs["Color"].default_value = (.045,.055,.065,1)
scene.world.node_tree.nodes.get("Background").inputs["Strength"].default_value = .45
scene.view_settings.view_transform = "Standard"
bpy.ops.object.camera_add()
camera = bpy.context.object
camera.data.type = "ORTHO"
camera.data.ortho_scale = 69
scene.camera = camera
for pos, energy, color in (((-35,-50,80),18000,(.92,.97,1)), ((45,-20,45),11000,(.75,.9,1)),
                            ((0,35,60),22000,(.55,.85,1))):
    bpy.ops.object.light_add(type="AREA", location=pos)
    light = bpy.context.object
    light.data.energy = energy; light.data.color = color; light.data.size = 45
    light.rotation_euler = (Vector((0,0,30))-light.location).to_track_quat("-Z","Y").to_euler()
bpy.ops.mesh.primitive_plane_add(size=200, location=(0,0,0))
floor = bpy.context.object; floor.name = "PREVIEW ONLY floor"
mat = bpy.data.materials.new("Preview floor"); mat.diffuse_color=(.028,.04,.05,1)
floor.data.materials.append(mat)
def view(name, position):
    camera.location = position
    camera.rotation_euler = (Vector((0,-2,29))-camera.location).to_track_quat("-Z","Y").to_euler()
    scene.render.filepath = str(OUT / (name + ".png"))
    bpy.ops.render.render(write_still=True)
view("front_three_quarter", (75,-140,74))
bpy.ops.wm.save_as_mainfile(filepath=str(EDIT / "geometry_holder_v1.blend"))
view("front", (0,-160,45))
view("back_three_quarter", (-85,140,66))
view("side", (160,0,43))
assert len({el["uuid"] for el in elements}) == len(elements)
assert len(bones["core"]["cubes"]) == 24
print(f"Geometry Holder: {len(elements)} cuboids, {len(bones)} bones, 24 independent core cuboids; export PASS")
