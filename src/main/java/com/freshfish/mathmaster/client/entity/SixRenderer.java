package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.SixEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class SixRenderer extends MobRenderer<SixEntity, SixModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "textures/entity/six.png");

    public SixRenderer(EntityRendererProvider.Context context) {
        super(context, new SixModel(context.bakeLayer(SixModel.LAYER)), 0.72F);
    }

    @Override
    public ResourceLocation getTextureLocation(SixEntity entity) {
        return TEXTURE;
    }
}
