package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.SevenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class SevenRenderer extends MobRenderer<SevenEntity, SevenModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "textures/entity/seven.png");

    public SevenRenderer(EntityRendererProvider.Context context) {
        super(context, new SevenModel(context.bakeLayer(SevenModel.LAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(SevenEntity entity) {
        return TEXTURE;
    }
}
