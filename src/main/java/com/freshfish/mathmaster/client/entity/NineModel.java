package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.NineEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Vanilla cuboid model exported from the approved Blender Nine v1 prototype. */
public class NineModel extends HierarchicalModel<NineEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "nine"), "main");
    private final ModelPart root;
    private final ModelPart body;

    public NineModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder cubes = CubeListBuilder.create();
        cubes.texOffs(0, 0).addBox(-8.000F, -32.000F, -2.000F, 16.000F, 5.000F, 4.000F); // 01 crown
        cubes.texOffs(0, 0).addBox(-8.000F, -27.000F, -2.000F, 4.000F, 9.000F, 4.000F); // 02 left bowl
        cubes.texOffs(0, 0).addBox(-8.000F, -18.000F, -2.000F, 12.000F, 4.000F, 4.000F); // 03 middle bar
        cubes.texOffs(0, 0).addBox(4.000F, -27.000F, -2.000F, 4.000F, 23.000F, 4.000F); // 04 descending right stem
        cubes.texOffs(0, 0).addBox(-4.000F, -4.000F, -2.000F, 12.000F, 4.000F, 4.000F); // 05 hooked foot
        cubes.texOffs(64, 0).addBox(4.600F, -15.250F, -2.160F, 2.800F, 0.350F, 0.100F); // Belt buckle
        cubes.texOffs(64, 0).addBox(4.600F, -17.050F, -2.160F, 2.800F, 0.350F, 0.100F); // Belt buckle.001
        cubes.texOffs(64, 0).addBox(4.600F, -16.700F, -2.160F, 0.350F, 1.450F, 0.100F); // Belt buckle.002
        cubes.texOffs(64, 0).addBox(7.050F, -16.700F, -2.160F, 0.350F, 1.450F, 0.100F); // Belt buckle.003
        cubes.texOffs(128, 0).addBox(-7.000F, -31.600F, -2.090F, 0.550F, 0.250F, 0.035F); // Crown stitch
        cubes.texOffs(128, 0).addBox(-5.000F, -31.600F, -2.090F, 0.550F, 0.250F, 0.035F); // Crown stitch.001
        cubes.texOffs(128, 0).addBox(-3.000F, -31.600F, -2.090F, 0.550F, 0.250F, 0.035F); // Crown stitch.002
        cubes.texOffs(128, 0).addBox(-1.000F, -31.600F, -2.090F, 0.550F, 0.250F, 0.035F); // Crown stitch.003
        cubes.texOffs(128, 0).addBox(1.000F, -31.600F, -2.090F, 0.550F, 0.250F, 0.035F); // Crown stitch.004
        cubes.texOffs(128, 0).addBox(3.000F, -31.600F, -2.090F, 0.550F, 0.250F, 0.035F); // Crown stitch.005
        cubes.texOffs(128, 0).addBox(5.000F, -31.600F, -2.090F, 0.550F, 0.250F, 0.035F); // Crown stitch.006
        cubes.texOffs(128, 0).addBox(7.000F, -31.600F, -2.090F, 0.550F, 0.250F, 0.035F); // Crown stitch.007
        cubes.texOffs(192, 0).addBox(-5.300F, -30.650F, -2.070F, 3.600F, 3.000F, 0.080F); // Eye socket
        cubes.texOffs(192, 0).addBox(1.700F, -30.650F, -2.070F, 3.600F, 3.000F, 0.080F); // Eye socket.001
        cubes.texOffs(0, 64).addBox(-5.000F, -30.350F, -2.130F, 3.000F, 2.350F, 0.040F); // Eye white
        cubes.texOffs(0, 64).addBox(2.000F, -30.350F, -2.130F, 3.000F, 2.350F, 0.040F); // Eye white.001
        cubes.texOffs(128, 0).addBox(-3.000F, -2.350F, -2.100F, 0.550F, 0.250F, 0.035F); // Foot stitch
        cubes.texOffs(128, 0).addBox(-1.000F, -2.350F, -2.100F, 0.550F, 0.250F, 0.035F); // Foot stitch.001
        cubes.texOffs(128, 0).addBox(1.000F, -2.350F, -2.100F, 0.550F, 0.250F, 0.035F); // Foot stitch.002
        cubes.texOffs(128, 0).addBox(3.000F, -2.350F, -2.100F, 0.550F, 0.250F, 0.035F); // Foot stitch.003
        cubes.texOffs(128, 0).addBox(5.000F, -2.350F, -2.100F, 0.550F, 0.250F, 0.035F); // Foot stitch.004
        cubes.texOffs(128, 0).addBox(7.000F, -2.350F, -2.100F, 0.550F, 0.250F, 0.035F); // Foot stitch.005
        cubes.texOffs(64, 64).addBox(-8.000F, -32.000F, -2.035F, 16.000F, 1.000F, 0.060F); // Leather crown rim
        cubes.texOffs(64, 64).addBox(-4.000F, -3.000F, 1.980F, 12.000F, 3.000F, 0.060F); // Leather foot back
        cubes.texOffs(64, 64).addBox(-4.000F, -3.000F, -2.040F, 12.000F, 3.000F, 0.080F); // Leather foot guard
        cubes.texOffs(64, 64).addBox(7.960F, -3.000F, -2.000F, 0.040F, 3.000F, 4.000F); // Leather foot right
        cubes.texOffs(64, 64).addBox(-8.000F, -27.000F, -2.035F, 1.000F, 9.000F, 0.060F); // Leather left shoulder edge
        cubes.texOffs(64, 64).addBox(7.960F, -17.000F, -2.000F, 0.040F, 2.000F, 4.000F); // Leather right wrap
        cubes.texOffs(64, 64).addBox(-8.000F, -17.000F, -2.040F, 16.000F, 2.000F, 0.080F); // Leather waist strap
        cubes.texOffs(64, 64).addBox(-8.000F, -17.000F, 1.980F, 16.000F, 2.000F, 0.060F); // Leather waist strap back
        cubes.texOffs(192, 0).addBox(-4.000F, -29.650F, -2.180F, 1.000F, 1.650F, 0.030F); // Pupil
        cubes.texOffs(192, 0).addBox(3.000F, -29.650F, -2.180F, 1.000F, 1.650F, 0.030F); // Pupil.001
        cubes.texOffs(128, 0).addBox(-7.000F, -15.500F, -2.100F, 0.550F, 0.250F, 0.035F); // Waist stitch
        cubes.texOffs(128, 0).addBox(-5.000F, -15.500F, -2.100F, 0.550F, 0.250F, 0.035F); // Waist stitch.001
        cubes.texOffs(128, 0).addBox(-3.000F, -15.500F, -2.100F, 0.550F, 0.250F, 0.035F); // Waist stitch.002
        cubes.texOffs(128, 0).addBox(-1.000F, -15.500F, -2.100F, 0.550F, 0.250F, 0.035F); // Waist stitch.003
        cubes.texOffs(128, 0).addBox(1.000F, -15.500F, -2.100F, 0.550F, 0.250F, 0.035F); // Waist stitch.004
        cubes.texOffs(128, 0).addBox(3.000F, -15.500F, -2.100F, 0.550F, 0.250F, 0.035F); // Waist stitch.005
        cubes.texOffs(128, 0).addBox(5.000F, -15.500F, -2.100F, 0.550F, 0.250F, 0.035F); // Waist stitch.006
        cubes.texOffs(128, 0).addBox(7.000F, -15.500F, -2.100F, 0.550F, 0.250F, 0.035F); // Waist stitch.007
        mesh.getRoot().addOrReplaceChild("body", cubes, PartPose.offset(0, 24, 0));
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(NineEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        body.resetPose();
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        body.zRot = Mth.sin(limbSwing * 1.5F) * Math.min(limbSwingAmount, 0.5F) * 0.06F;
        body.y -= Math.abs(Mth.sin(limbSwing * 1.5F)) * Math.min(limbSwingAmount, 0.5F);
    }
}

