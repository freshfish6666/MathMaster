package com.freshfish.mathmaster.client.entity;

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
public final class SixModel extends HierarchicalModel<SixEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "six"), "main");
    private final ModelPart root;
    private final ModelPart body;

    public SixModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder cubes = CubeListBuilder.create();
        cubes.texOffs(0, 0).addBox(-3.000F, -47.500F, -5.500F, 13.000F, 7.000F, 11.000F); // 01 upper brain lobe
        cubes.texOffs(64, 0).addBox(5.500F, -42.500F, -4.000F, 6.000F, 5.000F, 9.000F); // 02 upper torn tip
        cubes.texOffs(192, 0).addBox(-8.000F, -45.000F, -5.550F, 8.000F, 8.000F, 10.500F); // 03 skull curl
        cubes.texOffs(0, 64).addBox(-10.500F, -38.500F, -5.350F, 7.000F, 7.000F, 11.500F); // 04 neck knot
        cubes.texOffs(0, 0).addBox(-11.500F, -32.000F, -6.500F, 8.000F, 8.000F, 12.000F); // 05 left lung
        cubes.texOffs(64, 64).addBox(-11.000F, -24.500F, -4.850F, 8.000F, 7.000F, 10.500F); // 06 liver spine
        cubes.texOffs(64, 0).addBox(-10.500F, -18.000F, -5.950F, 9.000F, 8.000F, 11.500F); // 07 hip graft
        cubes.texOffs(0, 0).addBox(-5.000F, -27.500F, -4.800F, 10.000F, 7.000F, 10.000F); // 08 upper bowel bridge
        cubes.texOffs(128, 0).addBox(2.500F, -25.500F, -6.400F, 8.000F, 9.000F, 12.000F); // 09 right lung
        cubes.texOffs(64, 64).addBox(4.500F, -17.000F, -4.850F, 7.000F, 8.000F, 10.500F); // 10 right kidney
        cubes.texOffs(64, 0).addBox(1.000F, -10.500F, -5.950F, 9.000F, 7.000F, 11.500F); // 11 lower right bowel
        cubes.texOffs(0, 0).addBox(-6.500F, -7.500F, -5.600F, 12.000F, 7.000F, 12.000F); // 12 lower intestine
        cubes.texOffs(192, 0).addBox(-10.500F, -10.500F, -5.550F, 8.000F, 7.000F, 10.500F); // 13 lower left bowel
        cubes.texOffs(128, 64).addBox(-3.500F, -33.500F, -6.800F, 6.000F, 7.000F, 2.200F); // 14 exposed heart
        cubes.texOffs(192, 64).addBox(-1.500F, -28.500F, -7.000F, 4.000F, 4.000F, 2.000F); // 15 heart lower chamber
        cubes.texOffs(192, 0).addBox(2.500F, -19.500F, -7.000F, 5.000F, 6.000F, 2.000F); // 16 stomach pouch
        cubes.texOffs(128, 0).addBox(-7.000F, -21.500F, 4.550F, 6.000F, 7.000F, 2.500F); // 17 rear organ cyst
        cubes.texOffs(128, 0).addBox(3.000F, -39.000F, 4.500F, 5.000F, 6.000F, 2.400F); // 18 shoulder cyst
        cubes.texOffs(0, 128).addBox(-9.500F, -32.600F, -6.550F, 5.000F, 2.200F, 1.500F); // 19 intestine segment 0
        cubes.texOffs(0, 128).addBox(-9.750F, -25.600F, -6.550F, 5.500F, 2.200F, 1.500F); // 19 intestine segment 1
        cubes.texOffs(0, 128).addBox(-7.000F, -11.600F, -6.550F, 5.000F, 2.200F, 1.500F); // 19 intestine segment 2
        cubes.texOffs(0, 128).addBox(-2.000F, -6.100F, -6.550F, 6.000F, 2.200F, 1.500F); // 19 intestine segment 3
        cubes.texOffs(0, 128).addBox(4.750F, -11.600F, -6.550F, 4.500F, 2.200F, 1.500F); // 19 intestine segment 4
        cubes.texOffs(0, 128).addBox(4.750F, -19.100F, -6.550F, 4.500F, 2.200F, 1.500F); // 19 intestine segment 5
        cubes.texOffs(0, 64).addBox(-9.000F, -37.400F, -6.875F, 5.000F, 0.800F, 0.350F); // 20 tendon stitch 0
        cubes.texOffs(0, 64).addBox(-9.250F, -17.400F, -6.875F, 5.500F, 0.800F, 0.350F); // 20 tendon stitch 1
        cubes.texOffs(0, 64).addBox(-3.750F, -8.400F, -6.875F, 5.500F, 0.800F, 0.350F); // 20 tendon stitch 2
        cubes.texOffs(0, 64).addBox(4.500F, -14.400F, -6.875F, 4.000F, 0.800F, 0.350F); // 20 tendon stitch 3
        cubes.texOffs(64, 128).addBox(-9.875F, -31.500F, -6.900F, 0.750F, 11.000F, 0.300F); // 21 raised vein 0
        cubes.texOffs(64, 128).addBox(1.625F, -44.500F, -6.900F, 0.750F, 7.000F, 0.300F); // 21 raised vein 1
        cubes.texOffs(64, 128).addBox(9.125F, -19.000F, -6.900F, 0.750F, 8.000F, 0.300F); // 21 raised vein 2
        cubes.texOffs(64, 128).addBox(0.625F, -6.500F, -6.900F, 0.750F, 5.000F, 0.300F); // 21 raised vein 3
        cubes.texOffs(128, 128).addBox(-3.350F, -44.300F, -6.350F, 3.500F, 3.400F, 0.700F); // 22 eye socket 0
        cubes.texOffs(192, 128).addBox(-2.775F, -43.775F, -6.675F, 2.350F, 2.350F, 0.350F); // 23 blood eye 0
        cubes.texOffs(0, 192).addBox(-1.675F, -43.100F, -6.795F, 0.650F, 1.200F, 0.150F); // 24 black pupil 0
        cubes.texOffs(128, 128).addBox(1.450F, -44.300F, -6.350F, 3.500F, 3.400F, 0.700F); // 22 eye socket 1
        cubes.texOffs(192, 128).addBox(2.025F, -43.775F, -6.675F, 2.350F, 2.350F, 0.350F); // 23 blood eye 1
        cubes.texOffs(0, 192).addBox(2.625F, -43.100F, -6.795F, 0.650F, 1.200F, 0.150F); // 24 black pupil 1
        mesh.getRoot().addOrReplaceChild("body", cubes, PartPose.offset(0, 24, 0));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SixEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        body.resetPose();
        // This broad, three-dimensional body follows the entity's body yaw as one mass.
        body.yRot = 0.0F;
        float movement = Math.min(limbSwingAmount, 0.65F);
        body.zRot = Mth.sin(limbSwing * 1.15F) * movement * 0.075F;
        body.y -= Math.abs(Mth.sin(limbSwing * 1.15F)) * movement * 0.8F;
        body.xScale = 1.0F + Mth.sin(ageInTicks * 0.11F) * 0.008F;
        body.zScale = 1.0F - Mth.sin(ageInTicks * 0.11F) * 0.006F;
    }
}
