package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.entity.GeometryConstructEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class GeometryConstructModel extends HierarchicalModel<GeometryConstructEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("mathmaster", "geometry_construct"), "main");
    private final ModelPart root;
    public GeometryConstructModel(ModelPart root) { this.root = root; }
    @Override public ModelPart root() { return root; }
    public static LayerDefinition createBodyLayer() { return GeometryConstructMesh.create(); }
    @Override public void setupAnim(GeometryConstructEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        ModelPart body = root.getChild("body");
        body.xRot = pitch * Mth.DEG_TO_RAD;
        body.y += Mth.sin(age * .09F) * .45F;
        for (int i = 0; i < 4; i++) {
            ModelPart fragment = body.getChild("fragment_" + i);
            fragment.y += Mth.sin(age * .1F + i * 1.7F) * .45F;
            fragment.zRot = Mth.sin(age * .045F + i) * .1F;
        }
    }
}
