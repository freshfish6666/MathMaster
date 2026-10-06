package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.SevenEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Vanilla cuboid model exported from the Blender Seven v1 source. */
public class SevenModel extends HierarchicalModel<SevenEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "seven"), "main");
    private final ModelPart root;
    private final ModelPart body;

    public SevenModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder cubes = CubeListBuilder.create();
        cubes.texOffs(0, 0).addBox(-8.000F, -32.000F, -2.000F, 16.000F, 5.000F, 4.000F); // 01 crown
        cubes.texOffs(0, 0).addBox(3.000F, -27.000F, -2.000F, 5.000F, 4.000F, 4.000F); // 02 shank 0
        cubes.texOffs(128, 0).addBox(3.500F, -26.000F, -2.250F, 4.000F, 2.000F, 0.250F); // Diamond shank 0 -1
        cubes.texOffs(128, 0).addBox(3.500F, -26.000F, 2.000F, 4.000F, 2.000F, 0.250F); // Diamond shank 0 1
        cubes.texOffs(0, 0).addBox(1.500F, -23.500F, -2.000F, 5.000F, 4.000F, 4.000F); // 02 shank 1
        cubes.texOffs(0, 0).addBox(0.000F, -20.000F, -2.000F, 5.000F, 4.000F, 4.000F); // 02 shank 2
        cubes.texOffs(128, 0).addBox(0.500F, -19.000F, -2.250F, 4.000F, 2.000F, 0.250F); // Diamond shank 2 -1
        cubes.texOffs(128, 0).addBox(0.500F, -19.000F, 2.000F, 4.000F, 2.000F, 0.250F); // Diamond shank 2 1
        cubes.texOffs(0, 0).addBox(-1.500F, -16.500F, -2.000F, 5.000F, 4.000F, 4.000F); // 02 shank 3
        cubes.texOffs(0, 0).addBox(-3.000F, -13.000F, -2.000F, 5.000F, 4.000F, 4.000F); // 02 shank 4
        cubes.texOffs(128, 0).addBox(-2.500F, -12.000F, -2.250F, 4.000F, 2.000F, 0.250F); // Diamond shank 4 -1
        cubes.texOffs(128, 0).addBox(-2.500F, -12.000F, 2.000F, 4.000F, 2.000F, 0.250F); // Diamond shank 4 1
        cubes.texOffs(0, 0).addBox(-4.500F, -9.500F, -2.000F, 5.000F, 4.000F, 4.000F); // 02 shank 5
        cubes.texOffs(0, 0).addBox(-6.000F, -6.000F, -2.000F, 5.000F, 4.000F, 4.000F); // 02 shank 6
        cubes.texOffs(128, 0).addBox(-5.500F, -5.000F, -2.250F, 4.000F, 2.000F, 0.250F); // Diamond shank 6 -1
        cubes.texOffs(128, 0).addBox(-5.500F, -5.000F, 2.000F, 4.000F, 2.000F, 0.250F); // Diamond shank 6 1
        cubes.texOffs(0, 0).addBox(-7.000F, -2.500F, -2.000F, 6.000F, 2.500F, 4.000F); // 03 toe
        cubes.texOffs(128, 0).addBox(-8.000F, -31.500F, -2.250F, 16.000F, 4.000F, 0.250F); // Diamond crown -1
        cubes.texOffs(128, 0).addBox(-7.000F, -2.000F, -2.250F, 6.000F, 1.500F, 0.250F); // Diamond boot -1
        cubes.texOffs(64, 0).addBox(-8.000F, -32.000F, -2.300F, 16.000F, 0.800F, 0.300F); // Crown leather seam -1
        cubes.texOffs(128, 0).addBox(-8.000F, -31.500F, 2.000F, 16.000F, 4.000F, 0.250F); // Diamond crown 1
        cubes.texOffs(128, 0).addBox(-7.000F, -2.000F, 2.000F, 6.000F, 1.500F, 0.250F); // Diamond boot 1
        cubes.texOffs(64, 0).addBox(-8.000F, -32.000F, 2.000F, 16.000F, 0.800F, 0.300F); // Crown leather seam 1
        cubes.texOffs(192, 0).addBox(-5.300F, -30.850F, -2.350F, 3.600F, 2.800F, 0.080F); // Eye socket -3.5
        cubes.texOffs(0, 64).addBox(-5.000F, -30.675F, -2.400F, 3.000F, 2.250F, 0.040F); // Eye white -3.5
        cubes.texOffs(64, 64).addBox(-3.500F, -30.125F, -2.450F, 0.900F, 1.550F, 0.040F); // Pupil -3.5
        cubes.texOffs(192, 0).addBox(1.700F, -30.850F, -2.350F, 3.600F, 2.800F, 0.080F); // Eye socket 3.5
        cubes.texOffs(0, 64).addBox(2.000F, -30.675F, -2.400F, 3.000F, 2.250F, 0.040F); // Eye white 3.5
        cubes.texOffs(64, 64).addBox(2.600F, -30.125F, -2.450F, 0.900F, 1.550F, 0.040F); // Pupil 3.5
        mesh.getRoot().addOrReplaceChild("body", cubes, PartPose.offset(0, 24, 0));
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SevenEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        body.resetPose();
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        body.zRot = Mth.sin(limbSwing * 1.5F) * Math.min(limbSwingAmount, 0.5F) * 0.06F;
        body.y -= Math.abs(Mth.sin(limbSwing * 1.5F)) * Math.min(limbSwingAmount, 0.5F);
    }
}

