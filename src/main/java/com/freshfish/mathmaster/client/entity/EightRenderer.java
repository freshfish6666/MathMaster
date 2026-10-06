package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.EightEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class EightRenderer extends MobRenderer<EightEntity, EightModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "textures/entity/eight.png");

    public EightRenderer(EntityRendererProvider.Context context) {
        super(context, new EightModel(context.bakeLayer(EightModel.LAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(EightEntity entity) {
        return TEXTURE;
    }
}
