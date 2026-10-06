package com.freshfish.mathmaster.compat.jade;

import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IWailaClientRegistration;

/** Loaded only by Jade's optional client plugin. Never changes the boss bar or saved Jade settings. */
final class JadeBossTooltipPosition {
    private final BossTooltipLayout layout = new BossTooltipLayout();
    private GuiGraphics frame;

    static void register(IWailaClientRegistration registration) {
        var position = new JadeBossTooltipPosition();
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, position::beginFrame);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, position::recordBar);
        registration.addBeforeRenderCallback((tooltip, rect, gui, accessor) -> {
            var mc = Minecraft.getInstance();
            if (position.frame != gui || mc.level == null || mc.screen != null || mc.options.hideGui) return false;
            if (accessor instanceof EntityAccessor entityAccessor
                    && entityAccessor.getEntity() instanceof GeometryHolderEntity holder) {
                var box = rect.rect;
                // Jade translates to rect.rect before applying its own scale; these are already GUI pixels.
                box.setY(position.layout.tooltipY(holder.getUUID(), box.getX(), box.getY(), box.getWidth(), box.getHeight()));
            }
            return false; // Do not cancel Jade's render.
        });
    }

    private void beginFrame(RenderGuiEvent.Pre event) {
        frame = event.getGuiGraphics();
        layout.clear();
    }

    private void recordBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        var mc = Minecraft.getInstance();
        if (frame != event.getGuiGraphics() || mc.level == null) return;
        GeometryHolderEntity holder = null;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof GeometryHolderEntity boss && boss.getUUID().equals(event.getBossEvent().getId())) {
                holder = boss;
                break;
            }
        }
        int width = holder == null ? 182 : Math.min(260, frame.guiWidth() - 48);
        if (holder != null && width < 40) return;
        // Our custom bar cancels the vanilla drawing event; other canceled bars have no known bounds.
        if (holder == null && event.isCanceled()) return;
        int halfWidth = holder == null
                ? Math.max(91, (mc.font.width(event.getBossEvent().getName()) + 1) / 2)
                : (width + 1) / 2 + 15;
        int center = frame.guiWidth() / 2;
        layout.addBar(center - halfWidth, event.getY() - 9, center + halfWidth,
                event.getY() + (holder == null ? 5 : 28), holder == null ? null : holder.getUUID());
    }
}
