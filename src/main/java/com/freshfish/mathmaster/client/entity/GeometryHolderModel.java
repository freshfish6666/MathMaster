package com.freshfish.mathmaster.client.entity;

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
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.000F, -12.000F, -4.500F, 14.000F, 12.000F, 9.000F)
                .texOffs(64, 0).addBox(-8.000F, -12.500F, -7.500F, 16.000F, 3.000F, 11.000F)
                .texOffs(128, 0).addBox(-4.000F, -10.000F, -4.850F, 8.000F, 8.000F, 0.500F)
                .texOffs(0, 64).addBox(-0.500F, -9.500F, -5.250F, 1.000F, 7.000F, 0.500F)
                .texOffs(128, 0).addBox(-6.000F, -0.500F, -4.500F, 12.000F, 3.000F, 9.000F)
                .texOffs(64, 0).addBox(-7.000F, -0.000F, -5.300F, 14.000F, 2.000F, 1.000F),
                PartPose.offset(0.000F, -6.000F, 0.000F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(128, 0).addBox(-6.000F, -7.000F, 2.500F, 12.000F, 12.000F, 3.000F)
                .texOffs(64, 0).addBox(-6.000F, -8.500F, -6.000F, 12.000F, 3.000F, 12.000F)
                .texOffs(64, 0).addBox(-4.000F, -9.500F, -3.500F, 8.000F, 1.000F, 9.000F)
                .texOffs(192, 0).addBox(-4.000F, -6.000F, 1.750F, 8.000F, 10.000F, 0.500F)
                .texOffs(0, 0).addBox(-7.000F, -6.500F, -5.500F, 3.000F, 11.000F, 11.000F)
                .texOffs(64, 0).addBox(-6.000F, -6.500F, -6.000F, 3.000F, 3.000F, 2.000F)
                .texOffs(64, 64).addBox(-6.500F, 3.000F, -6.500F, 5.000F, 2.000F, 4.000F)
                .texOffs(0, 0).addBox(4.000F, -6.500F, -5.500F, 3.000F, 11.000F, 11.000F)
                .texOffs(64, 0).addBox(3.000F, -6.500F, -6.000F, 3.000F, 3.000F, 2.000F)
                .texOffs(64, 64).addBox(1.500F, 3.000F, -6.500F, 5.000F, 2.000F, 4.000F),
                PartPose.offset(0.000F, -13.000F, 0.000F));
        PartDefinition halo = body.addOrReplaceChild("halo", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-11.500F, -12.000F, -1.000F, 11.000F, 2.000F, 2.000F)
                .texOffs(0, 64).addBox(-11.000F, -11.500F, -1.225F, 10.000F, 1.000F, 0.250F)
                .texOffs(64, 0).addBox(4.000F, -11.000F, -1.000F, 8.000F, 2.000F, 2.000F)
                .texOffs(0, 64).addBox(4.500F, -10.500F, -1.225F, 7.000F, 1.000F, 0.250F)
                .texOffs(64, 0).addBox(-15.000F, -9.500F, -1.000F, 2.000F, 9.000F, 2.000F)
                .texOffs(0, 64).addBox(-14.500F, -9.000F, -1.225F, 1.000F, 8.000F, 0.250F)
                .texOffs(64, 0).addBox(-15.000F, 1.000F, -1.000F, 2.000F, 4.000F, 2.000F)
                .texOffs(0, 64).addBox(-14.500F, 1.500F, -1.225F, 1.000F, 3.000F, 0.250F)
                .texOffs(64, 0).addBox(13.000F, -8.500F, -1.000F, 2.000F, 5.000F, 2.000F)
                .texOffs(0, 64).addBox(13.500F, -8.000F, -1.225F, 1.000F, 4.000F, 0.250F)
                .texOffs(64, 0).addBox(13.000F, -1.500F, -1.000F, 2.000F, 7.000F, 2.000F)
                .texOffs(0, 64).addBox(13.500F, -1.000F, -1.225F, 1.000F, 6.000F, 0.250F),
                PartPose.offset(0.000F, -16.000F, 7.000F));
        PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-3.500F, -3.500F, -4.500F, 7.000F, 7.000F, 9.000F)
                .texOffs(0, 0).addBox(-4.000F, 1.500F, -3.000F, 4.000F, 7.000F, 6.000F),
                PartPose.offset(-10.000F, -8.000F, 0.000F));
        PartDefinition left_forearm = left_arm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(128, 0).addBox(-2.000F, -0.500F, -2.000F, 4.000F, 3.000F, 4.000F)
                .texOffs(0, 0).addBox(-3.000F, 1.000F, -8.000F, 6.000F, 6.000F, 10.000F)
                .texOffs(64, 0).addBox(-3.500F, 1.000F, -8.000F, 7.000F, 6.000F, 2.000F),
                PartPose.offset(-4.000F, 8.000F, -2.000F));
        PartDefinition left_hand = left_forearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-2.000F, -1.000F, -4.500F, 5.000F, 2.000F, 5.000F)
                .texOffs(64, 64).addBox(2.000F, -3.500F, -0.750F, 2.000F, 3.000F, 1.500F)
                .texOffs(64, 64).addBox(2.000F, -3.500F, -2.750F, 2.000F, 3.000F, 1.500F)
                .texOffs(64, 64).addBox(2.000F, -3.500F, -4.750F, 2.000F, 3.000F, 1.500F)
                .texOffs(0, 0).addBox(-2.000F, -3.500F, -1.000F, 2.000F, 3.000F, 2.000F),
                PartPose.offset(3.000F, 3.000F, -7.000F));
        PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-3.500F, -3.500F, -4.500F, 7.000F, 7.000F, 9.000F)
                .texOffs(0, 0).addBox(0.000F, 1.500F, -3.000F, 4.000F, 7.000F, 6.000F),
                PartPose.offset(10.000F, -8.000F, 0.000F));
        PartDefinition right_forearm = right_arm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(128, 0).addBox(-2.000F, -0.500F, -2.000F, 4.000F, 3.000F, 4.000F)
                .texOffs(0, 0).addBox(-3.000F, 1.000F, -8.000F, 6.000F, 6.000F, 10.000F)
                .texOffs(64, 0).addBox(-3.500F, 1.000F, -8.000F, 7.000F, 6.000F, 2.000F),
                PartPose.offset(4.000F, 8.000F, -2.000F));
        PartDefinition right_hand = right_forearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-3.000F, -1.000F, -4.500F, 5.000F, 2.000F, 5.000F)
                .texOffs(64, 64).addBox(-4.000F, -3.500F, -0.750F, 2.000F, 3.000F, 1.500F)
                .texOffs(64, 64).addBox(-4.000F, -3.500F, -2.750F, 2.000F, 3.000F, 1.500F)
                .texOffs(64, 64).addBox(-4.000F, -3.500F, -4.750F, 2.000F, 3.000F, 1.500F)
                .texOffs(0, 0).addBox(0.000F, -3.500F, -1.000F, 2.000F, 3.000F, 2.000F),
                PartPose.offset(-3.000F, 3.000F, -7.000F));
        PartDefinition robe_center = body.addOrReplaceChild("robe_center", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.000F, 1.000F, -4.500F, 6.000F, 20.000F, 5.000F)
                .texOffs(64, 0).addBox(-2.000F, 22.000F, -4.000F, 4.000F, 4.000F, 4.000F)
                .texOffs(0, 64).addBox(-0.500F, 2.000F, -4.725F, 1.000F, 18.000F, 0.250F),
                PartPose.offset(0.000F, 1.000F, 0.000F));
        PartDefinition robe_left = body.addOrReplaceChild("robe_left", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.000F, -1.000F, -3.500F, 4.000F, 10.000F, 7.000F)
                .texOffs(64, 64).addBox(-5.000F, 10.000F, -3.000F, 4.000F, 6.000F, 6.000F)
                .texOffs(64, 0).addBox(-4.500F, 18.000F, -2.000F, 3.000F, 4.000F, 4.000F),
                PartPose.offset(-6.000F, 3.000F, 0.000F));
        PartDefinition robe_right = body.addOrReplaceChild("robe_right", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.000F, -1.000F, -3.500F, 4.000F, 10.000F, 7.000F)
                .texOffs(64, 64).addBox(1.000F, 10.000F, -3.000F, 4.000F, 6.000F, 6.000F)
                .texOffs(64, 0).addBox(1.500F, 18.000F, -2.000F, 3.000F, 4.000F, 4.000F),
                PartPose.offset(6.000F, 3.000F, 0.000F));
        PartDefinition robe_back = body.addOrReplaceChild("robe_back", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.500F, -0.000F, -0.500F, 9.000F, 18.000F, 3.000F)
                .texOffs(64, 64).addBox(-5.000F, 3.000F, 2.000F, 2.000F, 20.000F, 2.000F)
                .texOffs(64, 64).addBox(3.000F, 3.000F, 2.000F, 2.000F, 20.000F, 2.000F),
                PartPose.offset(0.000F, 1.000F, 4.000F));
        PartDefinition core = body.addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-4.750F, 3.250F, -4.750F, 9.500F, 1.500F, 1.500F)
                .texOffs(0, 64).addBox(-3.250F, 3.105F, -3.455F, 6.500F, 0.350F, 0.350F)
                .texOffs(64, 0).addBox(-4.750F, 3.250F, 3.250F, 9.500F, 1.500F, 1.500F)
                .texOffs(0, 64).addBox(-3.250F, 3.105F, 3.105F, 6.500F, 0.350F, 0.350F)
                .texOffs(64, 0).addBox(-4.750F, -4.750F, -4.750F, 9.500F, 1.500F, 1.500F)
                .texOffs(0, 64).addBox(-3.250F, -3.455F, -3.455F, 6.500F, 0.350F, 0.350F)
                .texOffs(64, 0).addBox(-4.750F, -4.750F, 3.250F, 9.500F, 1.500F, 1.500F)
                .texOffs(0, 64).addBox(-3.250F, -3.455F, 3.105F, 6.500F, 0.350F, 0.350F)
                .texOffs(64, 0).addBox(-4.750F, -4.750F, -4.750F, 1.500F, 9.500F, 1.500F)
                .texOffs(0, 64).addBox(-3.455F, -3.250F, -3.455F, 0.350F, 6.500F, 0.350F)
                .texOffs(64, 0).addBox(-4.750F, -4.750F, 3.250F, 1.500F, 9.500F, 1.500F)
                .texOffs(0, 64).addBox(-3.455F, -3.250F, 3.105F, 0.350F, 6.500F, 0.350F)
                .texOffs(64, 0).addBox(3.250F, -4.750F, -4.750F, 1.500F, 9.500F, 1.500F)
                .texOffs(0, 64).addBox(3.105F, -3.250F, -3.455F, 0.350F, 6.500F, 0.350F)
                .texOffs(64, 0).addBox(3.250F, -4.750F, 3.250F, 1.500F, 9.500F, 1.500F)
                .texOffs(0, 64).addBox(3.105F, -3.250F, 3.105F, 0.350F, 6.500F, 0.350F)
                .texOffs(64, 0).addBox(-4.750F, 3.250F, -4.750F, 1.500F, 1.500F, 9.500F)
                .texOffs(0, 64).addBox(-3.455F, 3.105F, -3.250F, 0.350F, 0.350F, 6.500F)
                .texOffs(64, 0).addBox(-4.750F, -4.750F, -4.750F, 1.500F, 1.500F, 9.500F)
                .texOffs(0, 64).addBox(-3.455F, -3.455F, -3.250F, 0.350F, 0.350F, 6.500F)
                .texOffs(64, 0).addBox(3.250F, 3.250F, -4.750F, 1.500F, 1.500F, 9.500F)
                .texOffs(0, 64).addBox(3.105F, 3.105F, -3.250F, 0.350F, 0.350F, 6.500F)
                .texOffs(64, 0).addBox(3.250F, -4.750F, -4.750F, 1.500F, 1.500F, 9.500F)
                .texOffs(0, 64).addBox(3.105F, -3.455F, -3.250F, 0.350F, 0.350F, 6.500F),
                PartPose.offset(0.000F, -2.000F, -13.000F));
        return LayerDefinition.create(mesh, 256, 256);
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
