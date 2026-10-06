package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.network.DigitalPollutionMeterPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = MathMaster.MODID, value = Dist.CLIENT)
public final class DigitalPollutionMeterHud {
    private static final int WIDTH = 76;
    private static final int HEIGHT = 44;
    private static final int[][] DIGIT_SEGMENTS = {
            {1, 1, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 0, 0, 0},
            {1, 1, 0, 1, 1, 0, 1},
            {1, 1, 1, 1, 0, 0, 1},
            {0, 1, 1, 0, 0, 1, 1},
            {1, 0, 1, 1, 0, 1, 1},
            {1, 0, 1, 1, 1, 1, 1},
            {1, 1, 1, 0, 0, 0, 0},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 0, 1, 1}
    };

    private static boolean equipped;
    private static int pollution;

    private DigitalPollutionMeterHud() {
    }

    public static void accept(DigitalPollutionMeterPayload payload) {
        equipped = payload.equipped();
        pollution = Math.max(0, Math.min(99, payload.pollution()));
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!equipped || minecraft.player == null || !minecraft.player.isAlive() || minecraft.screen != null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        long ticks = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        int x = graphics.guiWidth() - WIDTH - 8;
        int y = graphics.guiHeight() - HEIGHT - 48;
        int color = pollutionColor(pollution);
        boolean highGlitch = pollution >= 75;
        int jitter = highGlitch && ticks % 13L == 0L ? 1 : 0;

        drawCasing(graphics, x + jitter, y, color, ticks);
        drawPollutionValue(graphics, x + 22 + jitter, y + 12, pollution, color);
        drawNoise(graphics, minecraft, x + jitter, y, color, ticks);

        if (pollution >= 95 && ticks % 40L < 8L) {
            drawOutOfControlPattern(graphics, x, y, ticks);
        }
    }

    private static void drawCasing(GuiGraphics graphics, int x, int y, int color, long ticks) {
        graphics.fill(x + 3, y + 2, x + WIDTH - 5, y + HEIGHT - 2, 0xC90B0712);
        graphics.fill(x + 1, y + 6, x + WIDTH - 2, y + HEIGHT - 6, 0xC90B0712);
        graphics.fill(x + 6, y + 5, x + WIDTH - 8, y + HEIGHT - 5, 0xE5161024);
        graphics.fill(x + 10, y + 9, x + WIDTH - 12, y + HEIGHT - 8, 0xD906040B);

        int dim = (color & 0x00FEFEFE) >> 1;
        int dimColor = 0xFF000000 | dim;
        graphics.fill(x + 5, y + 3, x + 25, y + 4, dimColor);
        graphics.fill(x + 31, y + 3, x + WIDTH - 8, y + 4, color);
        graphics.fill(x + 3, y + 7, x + 4, y + 20, color);
        graphics.fill(x + 3, y + 25, x + 4, y + HEIGHT - 8, dimColor);
        graphics.fill(x + 8, y + HEIGHT - 4, x + 34, y + HEIGHT - 3, color);
        graphics.fill(x + 40, y + HEIGHT - 4, x + WIDTH - 6, y + HEIGHT - 3, dimColor);
        graphics.fill(x + WIDTH - 4, y + 9, x + WIDTH - 3, y + 27, dimColor);
        graphics.fill(x + WIDTH - 4, y + 31, x + WIDTH - 3, y + HEIGHT - 9, color);

        int scanX = x + 9 + (int) (ticks % (WIDTH - 20));
        graphics.fill(scanX, y + 7, scanX + 1, y + HEIGHT - 7, 0x2038FFF4);
        graphics.fill(x + 8, y + 7, x + 13, y + 8, 0xFF38D8D1);
        graphics.fill(x + 11, y + 7, x + 12, y + 11, 0xFF38D8D1);
        graphics.fill(x + WIDTH - 15, y + HEIGHT - 11, x + WIDTH - 9, y + HEIGHT - 10, 0xFF38D8D1);
        graphics.fill(x + WIDTH - 12, y + HEIGHT - 13, x + WIDTH - 11, y + HEIGHT - 9, 0xFF38D8D1);
    }

    private static void drawPollutionValue(GuiGraphics graphics, int x, int y, int value, int color) {
        int display = Math.max(0, Math.min(999, value));
        drawDigit(graphics, x, y, display / 100, color);
        drawDigit(graphics, x + 12, y, display / 10 % 10, color);
        drawDigit(graphics, x + 24, y, display % 10, color);
    }

    private static void drawDigit(GuiGraphics graphics, int x, int y, int digit, int color) {
        int[] segments = DIGIT_SEGMENTS[digit];
        int shadow = 0x502A172A;
        drawSegment(graphics, x + 2, y, 6, 2, segments[0] == 1 ? color : shadow);
        drawSegment(graphics, x + 8, y + 2, 2, 6, segments[1] == 1 ? color : shadow);
        drawSegment(graphics, x + 8, y + 10, 2, 6, segments[2] == 1 ? color : shadow);
        drawSegment(graphics, x + 2, y + 16, 6, 2, segments[3] == 1 ? color : shadow);
        drawSegment(graphics, x, y + 10, 2, 6, segments[4] == 1 ? color : shadow);
        drawSegment(graphics, x, y + 2, 2, 6, segments[5] == 1 ? color : shadow);
        drawSegment(graphics, x + 2, y + 8, 6, 2, segments[6] == 1 ? color : shadow);
    }

    private static void drawSegment(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    private static void drawNoise(
            GuiGraphics graphics,
            Minecraft minecraft,
            int x,
            int y,
            int color,
            long ticks
    ) {
        int count = 2 + pollution / 18;
        String glyphs = "0189";
        for (int i = 0; i < count; i++) {
            long seed = ticks / Math.max(2, 8 - pollution / 18) * 31L + i * 47L + pollution * 13L;
            int gx = x + 7 + (int) Math.floorMod(seed * 17L, WIDTH - 17);
            int gy = y + 5 + (int) Math.floorMod(seed * 7L, HEIGHT - 14);
            char glyph = glyphs.charAt((int) Math.floorMod(seed, glyphs.length()));
            graphics.drawString(minecraft.font, Character.toString(glyph), gx, gy, (color & 0x00FFFFFF) | 0x55000000, false);
        }
    }

    private static void drawOutOfControlPattern(GuiGraphics graphics, int x, int y, long ticks) {
        int pulse = ticks % 4L < 2L ? 0xE8FF1018 : 0xB0B5000A;
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + 3, pulse);
        graphics.fill(x + 1, y + HEIGHT - 3, x + WIDTH - 1, y + HEIGHT - 1, pulse);
        graphics.fill(x + 1, y + 1, x + 3, y + HEIGHT - 1, pulse);
        graphics.fill(x + WIDTH - 3, y + 1, x + WIDTH - 1, y + HEIGHT - 1, pulse);
        for (int i = 0; i < 6; i++) {
            int px = x + 8 + i * 11;
            graphics.fill(px, y + 6, px + 2, y + 12, pulse);
            graphics.fill(x + WIDTH - 10 - i * 10, y + HEIGHT - 12, x + WIDTH - 8 - i * 10, y + HEIGHT - 6, pulse);
        }
        graphics.fill(x + 9, y + HEIGHT / 2, x + WIDTH - 9, y + HEIGHT / 2 + 1, 0xA0FF0000);
    }

    private static int pollutionColor(int value) {
        if (value >= 95) {
            return 0xFFFF1824;
        }
        if (value >= 75) {
            return 0xFFE23B31;
        }
        if (value >= 50) {
            return 0xFFFF8A28;
        }
        if (value >= 25) {
            return 0xFFF4D44D;
        }
        return 0xFF58DF91;
    }
}
