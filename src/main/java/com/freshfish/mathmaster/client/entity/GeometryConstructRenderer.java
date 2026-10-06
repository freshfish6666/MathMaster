package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.entity.GeometryConstructEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class GeometryConstructRenderer extends MobRenderer<GeometryConstructEntity, GeometryConstructModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mathmaster", "textures/entity/geometry_construct.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath("mathmaster", "textures/entity/geometry_construct_glow.png"));
    public GeometryConstructRenderer(EntityRendererProvider.Context context) {
        super(context, new GeometryConstructModel(context.bakeLayer(GeometryConstructModel.LAYER)), .45F);
        addLayer(new EyesLayer<>(this) {
            @Override public RenderType renderType() { return GLOW; }
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, GeometryConstructEntity entity,
                    float swing, float amount, float partial, float age, float yaw, float pitch) {
                int alpha = (int) (95 + 160 * entity.chargeGlow(partial));
                getParentModel().root().render(pose, buffers.getBuffer(GLOW), 15728640, OverlayTexture.NO_OVERLAY,
                        (alpha << 24) | 0xFFFFFF);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(GeometryConstructEntity entity) { return TEXTURE; }
    @Override public void render(GeometryConstructEntity entity, float yaw, float partial,
            PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partial, pose, buffers, light);
        if (entity.attackPhase() == GeometryConstructEntity.IDLE || entity.beamLength() <= 0) return;
        Vec3 start = entity.beamStart().subtract(entity.position());
        Vec3 end = start.add(entity.beamDirection().scale(entity.beamLength()));
        boolean firing = entity.attackPhase() == GeometryConstructEntity.FIRING;
        // A hairline warning has no damage; the active ray uses the server's radius and length.
        drawRay(pose, buffers, start, end, firing ? GeometryConstructEntity.BEAM_RADIUS : .012,
                firing ? 0x8855DDEB : 0x3855DDEB);
        if (firing) drawRay(pose, buffers, start, end, .025, 0xFFE0FFFF);
    }
    private static void drawRay(PoseStack pose, MultiBufferSource buffers, Vec3 start, Vec3 end, double radius, int color) {
        Vec3 direction = end.subtract(start).normalize();
        if (direction.lengthSqr() < .5) return;
        Vec3 reference = Math.abs(direction.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = direction.cross(reference).normalize().scale(radius);
        Vec3 up = direction.cross(side).normalize().scale(radius);
        Vec3[] corners = {side.add(up), side.subtract(up), side.scale(-1).subtract(up), side.scale(-1).add(up)};
        var buffer = buffers.getBuffer(RenderType.lightning());
        var matrix = pose.last().pose();
        for (int i = 0; i < 4; i++) for (Vec3 point : new Vec3[]{start.add(corners[i]), end.add(corners[i]),
                end.add(corners[(i + 1) % 4]), start.add(corners[(i + 1) % 4])})
            buffer.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z).setColor(color);
    }
}
