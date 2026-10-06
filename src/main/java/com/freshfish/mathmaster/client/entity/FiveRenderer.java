package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.FiveEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class FiveRenderer extends MobRenderer<FiveEntity, FiveModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "textures/entity/five.png");

    public FiveRenderer(EntityRendererProvider.Context context) {
        super(context, new FiveModel(context.bakeLayer(FiveModel.LAYER)), 6.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(FiveEntity entity) {
        return TEXTURE;
    }
}
