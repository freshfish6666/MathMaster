package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class GraduationCapModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "graduation_cap"),
            "main"
    );

    public GraduationCapModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.5F, -9.0F, -4.5F, 9.0F, 2.0F, 9.0F)
                        .texOffs(0, 12)
                        .addBox(-6.0F, -10.0F, -6.0F, 12.0F, 1.0F, 12.0F)
                        .texOffs(48, 8)
                        .addBox(-0.5F, -10.6F, -0.5F, 1.0F, 0.6F, 1.0F)
                        .texOffs(48, 10)
                        .addBox(0.0F, -10.5F, -0.25F, 5.5F, 0.4F, 0.5F)
                        .texOffs(48, 12)
                        .addBox(5.0F, -10.3F, -0.25F, 0.5F, 5.2F, 0.5F)
                        .texOffs(48, 20)
                        .addBox(4.6F, -5.2F, -0.6F, 1.3F, 2.2F, 1.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        addEmptyPart(root, "hat");
        addEmptyPart(root, "body");
        addEmptyPart(root, "right_arm");
        addEmptyPart(root, "left_arm");
        addEmptyPart(root, "right_leg");
        addEmptyPart(root, "left_leg");
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addEmptyPart(PartDefinition root, String name) {
        root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
    }
}
