package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.network.InvolutionStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.HashMap;
import java.util.Map;

/** Exchanges only the rendered appearances while leaving both entities and their controls intact. */
public final class InvolutionClientController {
    private static final Map<Integer, VisualSwap> SWAPS = new HashMap<>();
    private static Level currentLevel;
    private static boolean renderingReplacement;
    private static int appliedVisionTargetId = -1;

    private InvolutionClientController() {
    }

    public static void accept(InvolutionStatePayload payload) {
        currentLevel = Minecraft.getInstance().level;
        if (payload.active()) {
            SWAPS.put(payload.playerEntityId(),
                    new VisualSwap(payload.playerEntityId(), payload.targetEntityId()));
        } else {
            SWAPS.remove(payload.playerEntityId());
        }
        refreshLocalVision(Minecraft.getInstance());
    }

    public static boolean isActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && SWAPS.containsKey(minecraft.player.getId());
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level != currentLevel) {
            SWAPS.clear();
            currentLevel = minecraft.level;
            clearVision(minecraft);
            return;
        }
        refreshLocalVision(minecraft);
    }

    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (renderingReplacement) {
            return;
        }
        LivingEntity rendered = event.getEntity();
        VisualSwap swap = findSwap(rendered.getId());
        if (swap == null || rendered.level() != Minecraft.getInstance().level) {
            return;
        }
        int replacementId = rendered.getId() == swap.playerEntityId
                ? swap.targetEntityId
                : swap.playerEntityId;
        Entity replacement = rendered.level().getEntity(replacementId);
        if (!(replacement instanceof LivingEntity replacementLiving) || !replacementLiving.isAlive()) {
            return;
        }

        event.setCanceled(true);
        RotationSnapshot rotation = RotationSnapshot.capture(replacementLiving);
        copyRotation(rendered, replacementLiving);
        renderingReplacement = true;
        try {
            float yaw = Mth.rotLerp(event.getPartialTick(), rendered.yRotO, rendered.getYRot());
            Minecraft.getInstance().getEntityRenderDispatcher().render(
                    replacementLiving, 0.0D, 0.0D, 0.0D, yaw, event.getPartialTick(),
                    event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        } finally {
            renderingReplacement = false;
            rotation.restore(replacementLiving);
        }
    }

    private static VisualSwap findSwap(int entityId) {
        VisualSwap direct = SWAPS.get(entityId);
        if (direct != null) {
            return direct;
        }
        for (VisualSwap swap : SWAPS.values()) {
            if (swap.targetEntityId == entityId) {
                return swap;
            }
        }
        return null;
    }

    private static void refreshLocalVision(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            clearVision(minecraft);
            return;
        }
        VisualSwap local = SWAPS.get(minecraft.player.getId());
        int desiredTargetId = local == null ? -1 : local.targetEntityId;
        if (desiredTargetId == appliedVisionTargetId) {
            return;
        }
        if (desiredTargetId < 0) {
            clearVision(minecraft);
            return;
        }
        Entity target = minecraft.level.getEntity(desiredTargetId);
        if (target != null) {
            minecraft.gameRenderer.checkEntityPostEffect(target);
            appliedVisionTargetId = desiredTargetId;
        }
    }

    private static void clearVision(Minecraft minecraft) {
        if (appliedVisionTargetId < 0) {
            return;
        }
        minecraft.gameRenderer.checkEntityPostEffect(minecraft.player);
        appliedVisionTargetId = -1;
    }

    private static void copyRotation(LivingEntity source, LivingEntity destination) {
        destination.yRotO = source.yRotO;
        destination.setYRot(source.getYRot());
        destination.xRotO = source.xRotO;
        destination.setXRot(source.getXRot());
        destination.yBodyRotO = source.yBodyRotO;
        destination.yBodyRot = source.yBodyRot;
        destination.yHeadRotO = source.yHeadRotO;
        destination.yHeadRot = source.yHeadRot;
    }

    private record VisualSwap(int playerEntityId, int targetEntityId) {
    }

    private record RotationSnapshot(float yRotO, float yRot, float xRotO, float xRot,
                                    float yBodyRotO, float yBodyRot, float yHeadRotO, float yHeadRot) {
        private static RotationSnapshot capture(LivingEntity entity) {
            return new RotationSnapshot(entity.yRotO, entity.getYRot(), entity.xRotO, entity.getXRot(),
                    entity.yBodyRotO, entity.yBodyRot, entity.yHeadRotO, entity.yHeadRot);
        }

        private void restore(LivingEntity entity) {
            entity.yRotO = this.yRotO;
            entity.setYRot(this.yRot);
            entity.xRotO = this.xRotO;
            entity.setXRot(this.xRot);
            entity.yBodyRotO = this.yBodyRotO;
            entity.yBodyRot = this.yBodyRot;
            entity.yHeadRotO = this.yHeadRotO;
            entity.yHeadRot = this.yHeadRot;
        }
    }
}
