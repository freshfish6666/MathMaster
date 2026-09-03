package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.item.LingxuMirrorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = MathMaster.MODID, value = Dist.CLIENT)
public final class InsightMirrorHud {
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 8;
    private static final long FAILURE_DURATION_MILLIS = 5_000L;
    private static Component failureMessage = Component.empty();
    private static long failureExpiresAt;

    private InsightMirrorHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        if (net.minecraft.Util.getMillis() < failureExpiresAt) {
            graphics.drawCenteredString(
                    minecraft.font,
                    failureMessage,
                    graphics.guiWidth() / 2,
                    graphics.guiHeight() - 83,
                    0xFFFF7777
            );
        }

        if (!minecraft.player.isUsingItem()
                || minecraft.player.getUsedItemHand() != InteractionHand.MAIN_HAND
                || !minecraft.player.getUseItem().is(ModItems.LINGXU_MIRROR.get())) {
            return;
        }

        int ticks = minecraft.player.getTicksUsingItem();
        int percent = Math.min(
                100,
                (ticks + 1) * 100 / LingxuMirrorItem.INSIGHT_DURATION_TICKS
        );
        int filledWidth = percent * BAR_WIDTH / 100;
        int x = (graphics.guiWidth() - BAR_WIDTH) / 2;
        int y = graphics.guiHeight() - 64;

        Component label = Component.translatable("screen.mathmaster.insight.progress", percent);
        graphics.drawCenteredString(minecraft.font, label, graphics.guiWidth() / 2, y - 11, 0xF4D7FF);
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xE0000000);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF32253A);
        if (filledWidth > 0) {
            graphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, 0xFFD86EE8);
            graphics.fill(x, y, x + filledWidth, y + 2, 0xFFFFB7FF);
        }
    }

    public static void showTooDifficult(int targetIntellect, int playerIq) {
        failureMessage = Component.translatable(
                "message.mathmaster.insight.too_difficult",
                targetIntellect,
                playerIq
        );
        failureExpiresAt = net.minecraft.Util.getMillis() + FAILURE_DURATION_MILLIS;
    }
}
