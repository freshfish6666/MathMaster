package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.NineEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NineRenderer extends MobRenderer<NineEntity, NineModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "textures/entity/nine.png");

    public NineRenderer(EntityRendererProvider.Context context) {
        super(context, new NineModel(context.bakeLayer(NineModel.LAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(NineEntity entity) {
        return TEXTURE;
    }
}
