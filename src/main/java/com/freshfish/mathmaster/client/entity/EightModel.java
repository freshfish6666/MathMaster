package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.EightEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Vanilla cuboid model exported from the Blender Eight v1 source. */
public class EightModel extends HierarchicalModel<EightEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "eight"), "main");
    private final ModelPart root;
    private final ModelPart body;

    public EightModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder cubes = CubeListBuilder.create();
        cubes.texOffs(0, 0).addBox(-8.000F, -32.000F, -2.000F, 16.000F, 5.000F, 4.000F); // 01 crown [Leather body]
        cubes.texOffs(0, 0).addBox(-8.000F, -27.500F, -2.000F, 4.000F, 9.000F, 4.000F); // 02 upper left [Leather body]
        cubes.texOffs(0, 0).addBox(4.000F, -27.500F, -2.000F, 4.000F, 9.000F, 4.000F); // 03 upper right [Leather body]
        cubes.texOffs(0, 0).addBox(-8.000F, -18.500F, -2.000F, 16.000F, 5.000F, 4.000F); // 04 waist [Leather body]
        cubes.texOffs(0, 0).addBox(-8.000F, -13.500F, -2.000F, 4.000F, 11.000F, 4.000F); // 05 lower left [Leather body]
        cubes.texOffs(0, 0).addBox(4.000F, -13.500F, -2.000F, 4.000F, 11.000F, 4.000F); // 06 lower right [Leather body]
        cubes.texOffs(0, 0).addBox(-8.000F, -2.500F, -2.000F, 16.000F, 2.500F, 4.000F); // 07 sole [Leather body]
        cubes.texOffs(0, 64).addBox(1.700F, -30.850F, -2.200F, 3.600F, 2.800F, 0.080F); // Eye socket +3.5 [Eye socket]
        cubes.texOffs(0, 64).addBox(-5.300F, -30.850F, -2.200F, 3.600F, 2.800F, 0.080F); // Eye socket -3.5 [Eye socket]
        cubes.texOffs(64, 64).addBox(2.000F, -30.675F, -2.240F, 3.000F, 2.250F, 0.040F); // Eye white +3.5 [Eye white]
        cubes.texOffs(64, 64).addBox(-5.000F, -30.675F, -2.240F, 3.000F, 2.250F, 0.040F); // Eye white -3.5 [Eye white]
        cubes.texOffs(128, 0).addBox(-6.900F, -30.850F, -2.150F, 13.800F, 2.700F, 0.120F); // Iron crown plate [Iron armor]
        cubes.texOffs(192, 0).addBox(-7.855F, -12.400F, -2.170F, 1.350F, 8.800F, 0.140F); // Iron lower left guard [Iron armor shadow]
        cubes.texOffs(192, 0).addBox(6.505F, -12.400F, -2.170F, 1.350F, 8.800F, 0.140F); // Iron lower right guard [Iron armor shadow]
        cubes.texOffs(128, 0).addBox(-6.900F, -2.050F, -2.150F, 13.800F, 1.200F, 0.120F); // Iron sole plate [Iron armor]
        cubes.texOffs(192, 0).addBox(-7.855F, -26.400F, -2.170F, 1.350F, 6.800F, 0.140F); // Iron upper left guard [Iron armor shadow]
        cubes.texOffs(192, 0).addBox(6.505F, -26.400F, -2.170F, 1.350F, 6.800F, 0.140F); // Iron upper right guard [Iron armor shadow]
        cubes.texOffs(128, 0).addBox(-6.900F, -17.250F, -2.170F, 13.800F, 2.100F, 0.140F); // Iron waist plate [Iron armor]
        cubes.texOffs(64, 0).addBox(-8.000F, -32.000F, -2.070F, 16.000F, 0.900F, 0.070F); // Leather crown rim [Leather edging]
        cubes.texOffs(64, 0).addBox(-8.000F, -0.900F, -2.080F, 16.000F, 0.700F, 0.080F); // Leather sole rim [Leather edging]
        cubes.texOffs(64, 0).addBox(-8.000F, -16.550F, -2.090F, 16.000F, 1.100F, 0.090F); // Leather waist strap [Leather edging]
        cubes.texOffs(128, 64).addBox(2.600F, -30.125F, -2.275F, 0.900F, 1.550F, 0.030F); // Pupil +3.5 [Pupil]
        cubes.texOffs(128, 64).addBox(-3.500F, -30.125F, -2.275F, 0.900F, 1.550F, 0.030F); // Pupil -3.5 [Pupil]
        cubes.texOffs(64, 0).addBox(-7.275F, -0.670F, -2.123F, 0.550F, 0.240F, 0.035F); // Sole stitch.000 [Leather edging]
        cubes.texOffs(64, 0).addBox(-5.275F, -0.670F, -2.123F, 0.550F, 0.240F, 0.035F); // Sole stitch.001 [Leather edging]
        cubes.texOffs(64, 0).addBox(-3.275F, -0.670F, -2.123F, 0.550F, 0.240F, 0.035F); // Sole stitch.002 [Leather edging]
        cubes.texOffs(64, 0).addBox(-1.275F, -0.670F, -2.123F, 0.550F, 0.240F, 0.035F); // Sole stitch.003 [Leather edging]
        cubes.texOffs(64, 0).addBox(0.725F, -0.670F, -2.123F, 0.550F, 0.240F, 0.035F); // Sole stitch.004 [Leather edging]
        cubes.texOffs(64, 0).addBox(2.725F, -0.670F, -2.123F, 0.550F, 0.240F, 0.035F); // Sole stitch.005 [Leather edging]
        cubes.texOffs(64, 0).addBox(4.725F, -0.670F, -2.123F, 0.550F, 0.240F, 0.035F); // Sole stitch.006 [Leather edging]
        cubes.texOffs(64, 0).addBox(6.725F, -0.670F, -2.123F, 0.550F, 0.240F, 0.035F); // Sole stitch.007 [Leather edging]
        cubes.texOffs(64, 0).addBox(-7.275F, -15.370F, -2.123F, 0.550F, 0.240F, 0.035F); // Waist stitch.000 [Leather edging]
        cubes.texOffs(64, 0).addBox(-5.275F, -15.370F, -2.123F, 0.550F, 0.240F, 0.035F); // Waist stitch.001 [Leather edging]
        cubes.texOffs(64, 0).addBox(-3.275F, -15.370F, -2.123F, 0.550F, 0.240F, 0.035F); // Waist stitch.002 [Leather edging]
        cubes.texOffs(64, 0).addBox(-1.275F, -15.370F, -2.123F, 0.550F, 0.240F, 0.035F); // Waist stitch.003 [Leather edging]
        cubes.texOffs(64, 0).addBox(0.725F, -15.370F, -2.123F, 0.550F, 0.240F, 0.035F); // Waist stitch.004 [Leather edging]
        cubes.texOffs(64, 0).addBox(2.725F, -15.370F, -2.123F, 0.550F, 0.240F, 0.035F); // Waist stitch.005 [Leather edging]
        cubes.texOffs(64, 0).addBox(4.725F, -15.370F, -2.123F, 0.550F, 0.240F, 0.035F); // Waist stitch.006 [Leather edging]
        cubes.texOffs(64, 0).addBox(6.725F, -15.370F, -2.123F, 0.550F, 0.240F, 0.035F); // Waist stitch.007 [Leather edging]
        mesh.getRoot().addOrReplaceChild("body", cubes, PartPose.offset(0, 24, 0));
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(EightEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        body.resetPose();
        body.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        body.zRot = Mth.sin(limbSwing * 1.5F) * Math.min(limbSwingAmount, 0.5F) * 0.06F;
        body.y -= Math.abs(Mth.sin(limbSwing * 1.5F)) * Math.min(limbSwingAmount, 0.5F);
    }
}

