package com.freshfish.mathmaster.client.entity;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

public final class GeometryHolderRenderer extends MobRenderer<GeometryHolderEntity, GeometryHolderModel<GeometryHolderEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MathMaster.MODID, "textures/entity/geometry_holder.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(
            MathMaster.MODID, "textures/entity/geometry_holder_emissive.png"));

    public GeometryHolderRenderer(EntityRendererProvider.Context context) {
        super(context, new GeometryHolderModel<>(context.bakeLayer(GeometryHolderModel.LAYER)), 0.75F);
        addLayer(new EyesLayer<>(this) {
            @Override public RenderType renderType() { return GLOW; }
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.renderer.MultiBufferSource buffer, int light, GeometryHolderEntity entity,
                    float swing, float amount, float partial, float age, float yaw, float pitch) {
                getParentModel().root().render(pose, buffer.getBuffer(GLOW), 15728640,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, entity.glowTint(partial));
            }
        });
        addLayer(new EyesLayer<>(this) {
            @Override public RenderType renderType() { return RenderType.eyes(TEXTURE); }
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.renderer.MultiBufferSource buffer, int light, GeometryHolderEntity entity,
                    float swing, float amount, float partial, float age, float yaw, float pitch) {
                if (entity.nearPhase() == GeometryHolderEntity.HALO)
                    getParentModel().renderChargingHalo(pose, buffer.getBuffer(renderType()), entity.glowTint(partial));
            }
        });
        addLayer(new EyesLayer<>(this) {
            @Override public RenderType renderType() { return RenderType.eyes(TEXTURE); }
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.renderer.MultiBufferSource buffer, int light, GeometryHolderEntity entity,
                    float swing, float amount, float partial, float age, float yaw, float pitch) {
                float glow=entity.rangedAttacks().chargeGlow(partial);
                if(glow>0) getParentModel().renderChargingCore(pose,buffer.getBuffer(renderType()),
                        ((int)(glow*255)<<24)|(entity.coreChargeTint(partial)&0xFFFFFF));
            }
        });
    }

    @Override public void render(GeometryHolderEntity entity, float yaw, float partial,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        super.render(entity,yaw,partial,pose,buffers,light);
        var ultimate = entity.ultimateAttack();
        for (int i=0;i<com.freshfish.mathmaster.entity.GeometryHolderUltimate.MAX_CUBES;i++) if (ultimate.cubeActive(i)) {
            var offset = ultimate.cubePosition(i).subtract(entity.position());
            pose.pushPose(); pose.translate(offset.x,offset.y,offset.z);
            // The axis-aligned .5-block model matches its contact volume exactly.
            float scale = 8F / 9.5F;
            pose.scale(-scale,-scale,scale);
            model.renderDetachedCore(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,entity.bodyTint(partial));
            model.renderDetachedCore(pose,buffers.getBuffer(GLOW),15728640,entity.glowTint(partial));
            model.renderDetachedCore(pose,buffers.getBuffer(RenderType.eyes(TEXTURE)),15728640,
                    ((int)(ultimate.brightness(partial)*220)<<24)|(entity.coreChargeTint(partial)&0xFFFFFF));
            pose.popPose();
        }
        if(entity.nearPhase()==GeometryHolderEntity.PRISON || entity.nearPhase()==GeometryHolderEntity.PRISON_ACTIVE) {
            var center=entity.prisonCenter().subtract(entity.position());
            double half=net.minecraft.util.Mth.lerp(entity.prisonExpansion(partial),9.5/32,entity.prisonSize()/2);
            int color=(entity.glowTint(partial)&0xFFFFFF)|0xB0000000;
            for(int axis=0;axis<3;axis++) for(int a:new int[]{-1,1}) for(int b:new int[]{-1,1}) {
                var from=axis==0 ? new net.minecraft.world.phys.Vec3(-half,a*half,b*half)
                        : axis==1 ? new net.minecraft.world.phys.Vec3(a*half,-half,b*half) : new net.minecraft.world.phys.Vec3(a*half,b*half,-half);
                var to=axis==0 ? new net.minecraft.world.phys.Vec3(half,a*half,b*half)
                        : axis==1 ? new net.minecraft.world.phys.Vec3(a*half,half,b*half) : new net.minecraft.world.phys.Vec3(a*half,b*half,half);
                drawRay(pose,buffers,center.add(from),center.add(to),.045,color);
            }
        }
        var spells=entity.rangedAttacks();
        for (int i=0;i<3;i++) if (spells.bombActive(i)) {
            var offset=spells.bombPosition(i,partial).subtract(entity.position());
            pose.pushPose(); pose.translate(offset.x,offset.y,offset.z);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((entity.tickCount+partial)*5));
            pose.scale(-1,-1,1);
            model.renderDetachedCore(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,entity.bodyTint(partial));
            model.renderDetachedCore(pose,buffers.getBuffer(GLOW),15728640,entity.glowTint(partial));
            pose.popPose();
        }
        var start=entity.castingCenter().subtract(entity.position());
        boolean preview=spells.phase()==com.freshfish.mathmaster.entity.GeometryHolderRangedAttacks.CROSS_CHARGE;
        for(int i=0;i<spells.rayCount();i++) {
            var end=start.add(spells.rayDirection(i,partial).scale(spells.rayLength(i)));
            drawRay(pose,buffers,start,end,preview ? .045 : spells.rayRadius(),preview ? 0x5578EFFF : 0x7078EFFF);
            if(!preview) drawRay(pose,buffers,start,end,spells.rayRadius()*.09/.35,0xFFE0FFFF);
        }
    }

    private static void drawRay(com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers, net.minecraft.world.phys.Vec3 start,
            net.minecraft.world.phys.Vec3 end, double radius, int color) {
        var direction=end.subtract(start).normalize();
        if(direction.lengthSqr()<.5) return;
        var reference=Math.abs(direction.y)>.9 ? new net.minecraft.world.phys.Vec3(1,0,0) : new net.minecraft.world.phys.Vec3(0,1,0);
        var side=direction.cross(reference).normalize().scale(radius);
        var up=direction.cross(side).normalize().scale(radius);
        var corners=new net.minecraft.world.phys.Vec3[]{side.add(up),side.subtract(up),side.scale(-1).subtract(up),side.scale(-1).add(up)};
        var buffer=buffers.getBuffer(RenderType.lightning()); var matrix=pose.last().pose();
        for(int i=0;i<4;i++) {
            var a=start.add(corners[i]); var b=end.add(corners[i]);
            var c=end.add(corners[(i+1)%4]); var d=start.add(corners[(i+1)%4]);
            for(var point:new net.minecraft.world.phys.Vec3[]{a,b,c,d})
                buffer.addVertex(matrix,(float)point.x,(float)point.y,(float)point.z).setColor(color);
        }
    }

    @Override public ResourceLocation getTextureLocation(GeometryHolderEntity entity) { return TEXTURE; }
}
