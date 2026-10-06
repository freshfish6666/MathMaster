package com.freshfish.mathmaster.client.entity;

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
        CubeListBuilder cubes_body = CubeListBuilder.create();
        cubes_body.texOffs(512, 0).addBox(-226.000F, -39.000F, -338.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(0, 256).addBox(-114.000F, -33.000F, -338.000F, 116.000F, 32.000F, 116.000F);
        cubes_body.texOffs(512, 256).addBox(-2.000F, -27.000F, -338.000F, 116.000F, 26.000F, 116.000F);
        cubes_body.texOffs(0, 512).addBox(110.000F, -39.000F, -338.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(512, 256).addBox(-338.000F, -33.000F, -226.000F, 116.000F, 32.000F, 116.000F);
        cubes_body.texOffs(0, 512).addBox(-226.000F, -27.000F, -226.000F, 116.000F, 26.000F, 116.000F);
        cubes_body.texOffs(512, 512).addBox(-114.000F, -39.000F, -226.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(0, 768).addBox(-2.000F, -33.000F, -226.000F, 116.000F, 32.000F, 116.000F);
        cubes_body.texOffs(512, 768).addBox(110.000F, -27.000F, -226.000F, 116.000F, 26.000F, 116.000F);
        cubes_body.texOffs(0, 0).addBox(222.000F, -39.000F, -226.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(0, 768).addBox(-338.000F, -39.000F, -114.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(512, 768).addBox(-226.000F, -33.000F, -114.000F, 116.000F, 32.000F, 116.000F);
        cubes_body.texOffs(0, 0).addBox(-114.000F, -27.000F, -114.000F, 116.000F, 26.000F, 116.000F);
        cubes_body.texOffs(512, 0).addBox(-2.000F, -39.000F, -114.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(0, 256).addBox(110.000F, -33.000F, -114.000F, 116.000F, 32.000F, 116.000F);
        cubes_body.texOffs(512, 256).addBox(222.000F, -27.000F, -114.000F, 116.000F, 26.000F, 116.000F);
        cubes_body.texOffs(512, 0).addBox(-338.000F, -27.000F, -2.000F, 116.000F, 26.000F, 116.000F);
        cubes_body.texOffs(0, 256).addBox(-226.000F, -39.000F, -2.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(512, 256).addBox(-114.000F, -33.000F, -2.000F, 116.000F, 32.000F, 116.000F);
        cubes_body.texOffs(0, 512).addBox(-2.000F, -27.000F, -2.000F, 116.000F, 26.000F, 116.000F);
        cubes_body.texOffs(512, 512).addBox(110.000F, -39.000F, -2.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(0, 768).addBox(222.000F, -33.000F, -2.000F, 116.000F, 32.000F, 116.000F);
        cubes_body.texOffs(0, 512).addBox(-338.000F, -33.000F, 110.000F, 116.000F, 32.000F, 116.000F);
        cubes_body.texOffs(512, 512).addBox(-226.000F, -27.000F, 110.000F, 116.000F, 26.000F, 116.000F);
        cubes_body.texOffs(0, 768).addBox(-114.000F, -39.000F, 110.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(512, 768).addBox(-2.000F, -33.000F, 110.000F, 116.000F, 32.000F, 116.000F);
        cubes_body.texOffs(0, 0).addBox(110.000F, -27.000F, 110.000F, 116.000F, 26.000F, 116.000F);
        cubes_body.texOffs(512, 0).addBox(222.000F, -39.000F, 110.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(0, 0).addBox(-226.000F, -33.000F, 222.000F, 116.000F, 32.000F, 116.000F);
        cubes_body.texOffs(512, 0).addBox(-114.000F, -27.000F, 222.000F, 116.000F, 26.000F, 116.000F);
        cubes_body.texOffs(0, 256).addBox(-2.000F, -39.000F, 222.000F, 116.000F, 38.000F, 116.000F);
        cubes_body.texOffs(512, 256).addBox(110.000F, -33.000F, 222.000F, 116.000F, 32.000F, 116.000F);
        root.addOrReplaceChild("body", cubes_body, PartPose.ZERO);
        CubeListBuilder cubes_tentacle_nw = CubeListBuilder.create();
        cubes_tentacle_nw.texOffs(512, 256).addBox(-346.000F, -34.000F, -340.000F, 92.000F, 54.000F, 80.000F);
        cubes_tentacle_nw.texOffs(0, 256).addBox(-374.000F, -15.500F, -365.000F, 80.000F, 47.000F, 70.000F);
        cubes_tentacle_nw.texOffs(512, 0).addBox(-402.000F, 3.000F, -390.000F, 68.000F, 40.000F, 60.000F);
        cubes_tentacle_nw.texOffs(512, 256).addBox(-430.000F, 21.500F, -415.000F, 56.000F, 33.000F, 50.000F);
        root.addOrReplaceChild("tentacle_nw", cubes_tentacle_nw, PartPose.ZERO);
        CubeListBuilder cubes_tentacle_ne = CubeListBuilder.create();
        cubes_tentacle_ne.texOffs(512, 768).addBox(254.000F, -34.000F, -340.000F, 92.000F, 54.000F, 80.000F);
        cubes_tentacle_ne.texOffs(0, 768).addBox(294.000F, -15.500F, -365.000F, 80.000F, 47.000F, 70.000F);
        cubes_tentacle_ne.texOffs(512, 512).addBox(334.000F, 3.000F, -390.000F, 68.000F, 40.000F, 60.000F);
        cubes_tentacle_ne.texOffs(512, 768).addBox(374.000F, 21.500F, -415.000F, 56.000F, 33.000F, 50.000F);
        root.addOrReplaceChild("tentacle_ne", cubes_tentacle_ne, PartPose.ZERO);
        CubeListBuilder cubes_tentacle_sw = CubeListBuilder.create();
        cubes_tentacle_sw.texOffs(512, 256).addBox(-346.000F, -34.000F, 260.000F, 92.000F, 54.000F, 80.000F);
        cubes_tentacle_sw.texOffs(0, 256).addBox(-374.000F, -15.500F, 295.000F, 80.000F, 47.000F, 70.000F);
        cubes_tentacle_sw.texOffs(512, 0).addBox(-402.000F, 3.000F, 330.000F, 68.000F, 40.000F, 60.000F);
        cubes_tentacle_sw.texOffs(512, 256).addBox(-430.000F, 21.500F, 365.000F, 56.000F, 33.000F, 50.000F);
        root.addOrReplaceChild("tentacle_sw", cubes_tentacle_sw, PartPose.ZERO);
        CubeListBuilder cubes_tentacle_se = CubeListBuilder.create();
        cubes_tentacle_se.texOffs(512, 768).addBox(254.000F, -34.000F, 260.000F, 92.000F, 54.000F, 80.000F);
        cubes_tentacle_se.texOffs(0, 768).addBox(294.000F, -15.500F, 295.000F, 80.000F, 47.000F, 70.000F);
        cubes_tentacle_se.texOffs(512, 512).addBox(334.000F, 3.000F, 330.000F, 68.000F, 40.000F, 60.000F);
        cubes_tentacle_se.texOffs(512, 768).addBox(374.000F, 21.500F, 365.000F, 56.000F, 33.000F, 50.000F);
        root.addOrReplaceChild("tentacle_se", cubes_tentacle_se, PartPose.ZERO);
        CubeListBuilder cubes_eye_stalk = CubeListBuilder.create();
        cubes_eye_stalk.texOffs(0, 0).addBox(280.000F, -42.000F, 0.000F, 100.000F, 58.000F, 70.000F);
        cubes_eye_stalk.texOffs(512, 0).addBox(355.000F, -46.000F, 16.500F, 90.000F, 52.000F, 63.000F);
        cubes_eye_stalk.texOffs(0, 256).addBox(430.000F, -50.000F, 33.000F, 80.000F, 46.000F, 56.000F);
        cubes_eye_stalk.texOffs(512, 256).addBox(505.000F, -54.000F, 49.500F, 70.000F, 40.000F, 49.000F);
        cubes_eye_stalk.texOffs(0, 512).addBox(580.000F, -58.000F, 66.000F, 60.000F, 34.000F, 42.000F);
        cubes_eye_stalk.texOffs(0, 512).addBox(607.000F, -83.000F, 5.000F, 56.000F, 50.000F, 30.000F);
        cubes_eye_stalk.texOffs(0, 512).addBox(607.000F, -83.000F, 59.000F, 56.000F, 50.000F, 30.000F);
        cubes_eye_stalk.texOffs(512, 512).addBox(641.000F, -72.000F, 14.000F, 22.000F, 24.000F, 12.000F);
        cubes_eye_stalk.texOffs(512, 512).addBox(641.000F, -72.000F, 68.000F, 22.000F, 24.000F, 12.000F);
        root.addOrReplaceChild("eye_stalk", cubes_eye_stalk, PartPose.ZERO);
        return LayerDefinition.create(mesh, 1024, 1024);
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
