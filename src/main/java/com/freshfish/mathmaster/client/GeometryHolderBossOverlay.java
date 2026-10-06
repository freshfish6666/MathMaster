package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

@EventBusSubscriber(modid=MathMaster.MODID,value=Dist.CLIENT)
public final class GeometryHolderBossOverlay {
    private GeometryHolderBossOverlay() {}
    @SubscribeEvent public static void render(CustomizeGuiOverlayEvent.BossEventProgress event) {
        var mc=Minecraft.getInstance();
        if(mc.level==null) return;
        GeometryHolderEntity holder=null;
        for(var entity:mc.level.entitiesForRendering())
            if(entity instanceof GeometryHolderEntity boss && boss.getUUID().equals(event.getBossEvent().getId())) {holder=boss;break;}
        if(holder==null) return; // Vanilla bar remains available until entity tracking has arrived.
        event.setCanceled(true);
        event.setIncrement(40);
        var gui=event.getGuiGraphics();
        int width=Math.min(260,gui.guiWidth()-48);
        if(width<40) return;
        int x=(gui.guiWidth()-width)/2,y=event.getY();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        GeometryHolderBarStyle.draw(gui::fill,x,y,width,event.getBossEvent().getProgress(),holder.phaseBlend(partial));
        var title=event.getBossEvent().getName();
        float scale=Math.min(1F,(float)(width+16)/Math.max(1,mc.font.width(title)));
        gui.pose().pushPose();
        gui.pose().translate(gui.guiWidth()/2F,y-9,0);
        gui.pose().scale(scale,scale,1);
        gui.drawString(mc.font,title,-mc.font.width(title)/2,0,0xFFE2F4F8,true);
        gui.pose().popPose();
    }
}
