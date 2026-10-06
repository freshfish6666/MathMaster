package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.network.PrimeComboStatePayload;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid=MathMaster.MODID,value=Dist.CLIENT)
public final class PrimeComboOverlay {
    private static int prime, remainingTicks;
    private static ResourceKey<Level> dimension;
    private PrimeComboOverlay() {}
    public static void accept(PrimeComboStatePayload payload) {
        prime=Math.max(0,payload.prime()); remainingTicks=Math.clamp(payload.remainingTicks(),0,100);
        var player=Minecraft.getInstance().player;
        dimension=player==null?null:player.level().dimension();
    }
    @SubscribeEvent public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc=Minecraft.getInstance();
        if (mc.player==null || !mc.player.isAlive() || mc.player.level().dimension()!=dimension) {
            prime=0; remainingTicks=0; return;
        }
        if (remainingTicks>0 && !mc.isPaused()) remainingTicks--;
    }
    @SubscribeEvent public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        prime=0; remainingTicks=0; dimension=null;
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        Minecraft mc=Minecraft.getInstance();
        if (prime==0 || remainingTicks<=0 || mc.player==null || mc.screen!=null || mc.options.hideGui) return;
        var graphics=event.getGuiGraphics(); int width=graphics.guiWidth(), height=graphics.guiHeight();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time=remainingTicks-partial;
        double pulse=time<=30 ? .72+.28*Math.sin((mc.player.tickCount+partial)*Math.PI/10) : 1;
        int bands=12, edge=Math.max(8,Math.min(width,height)/12);
        for(int i=0;i<bands;i++) {
            int inset=edge*i/bands, next=edge*(i+1)/bands;
            int color=((int)(30*pulse*(1-i/(float)bands))<<24)|0xE7C967;
            graphics.fill(inset,inset,width-inset,next,color);
            graphics.fill(inset,height-next,width-inset,height-inset,color);
            graphics.fill(inset,next,next,height-next,color);
            graphics.fill(width-next,next,width-inset,height-next,color);
        }
        graphics.drawCenteredString(mc.font,Component.translatable("hud.mathmaster.prime_combo",prime,
                String.format(Locale.ROOT,"%.1f",Math.max(0,time)/20)),width/2,height-64,0xFFE5C774);
    }
}
