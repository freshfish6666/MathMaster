package com.freshfish.mathmaster.client.entity;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Generated geometry only; animations live in GeometryConstructModel. */
final class GeometryConstructMesh {
    static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.000F, -7.000F, -3.000F, 14.000F, 14.000F, 10.000F)
                .texOffs(49, 0).addBox(-7.000F, -7.000F, -7.000F, 14.000F, 3.000F, 4.000F)
                .texOffs(86, 0).addBox(-7.000F, 4.000F, -7.000F, 14.000F, 3.000F, 4.000F)
                .texOffs(0, 25).addBox(-7.000F, -4.000F, -7.000F, 3.000F, 8.000F, 4.000F)
                .texOffs(15, 25).addBox(4.000F, -4.000F, -7.000F, 3.000F, 8.000F, 4.000F)
                .texOffs(30, 25).addBox(-4.000F, -4.000F, -3.250F, 8.000F, 8.000F, 0.250F)
                .texOffs(49, 25).addBox(-2.000F, -2.000F, -6.800F, 4.000F, 4.000F, 3.000F), PartPose.offset(0, 13, 0));
        body.addOrReplaceChild("fragment_0", CubeListBuilder.create().texOffs(64, 25)
                .addBox(-2.000F, -2.000F, -2.000F, 4.000F, 4.000F, 4.000F), PartPose.offset(-9.000F, -9.000F, 0.000F));
        body.addOrReplaceChild("fragment_1", CubeListBuilder.create().texOffs(81, 25)
                .addBox(-2.000F, -2.000F, -2.000F, 4.000F, 4.000F, 4.000F), PartPose.offset(9.000F, -9.000F, 0.000F));
        body.addOrReplaceChild("fragment_2", CubeListBuilder.create().texOffs(98, 25)
                .addBox(-2.000F, -2.000F, -2.000F, 4.000F, 4.000F, 4.000F), PartPose.offset(-9.000F, 9.000F, 0.000F));
        body.addOrReplaceChild("fragment_3", CubeListBuilder.create().texOffs(0, 38)
                .addBox(-2.000F, -2.000F, -2.000F, 4.000F, 4.000F, 4.000F), PartPose.offset(9.000F, 9.000F, 0.000F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
