package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.network.SelfInsightPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SelfInsightScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 190;
    private final SelfInsightPayload snapshot;

    public SelfInsightScreen(SelfInsightPayload snapshot) {
        super(Component.translatable("screen.mathmaster.self_insight.title"));
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        int panelHeight = Math.min(PANEL_HEIGHT, height - 20);
        int panelBottom = (height + panelHeight) / 2;
        addRenderableWidget(Button.builder(
                Component.translatable("screen.mathmaster.self_insight.close"),
                button -> onClose()
        ).bounds(width / 2 - 50, panelBottom - 30, 100, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // This screen uses its own translucent overlay and must not enable the
        // vanilla menu blur after its information has already been drawn.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA8000000);

        int panelWidth = Math.min(PANEL_WIDTH, width - 20);
        int panelHeight = Math.min(PANEL_HEIGHT, height - 20);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, 0xED17171D);
        graphics.fill(left, top, right, top + 2, 0xFF6C566F);
        graphics.fill(left, bottom - 2, right, bottom, 0xFF3D303F);
        graphics.fill(left, top, left + 2, bottom, 0xFF6C566F);
        graphics.fill(right - 2, top, right, bottom, 0xFF3D303F);

        int centerX = width / 2;
        graphics.drawCenteredString(font, title, centerX, top + 14, 0xFFF0DFF3);
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.mathmaster.self_insight.player", snapshot.playerName()),
                centerX,
                top + 42,
                0xFFFFFFFF
        );
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.mathmaster.self_insight.uuid", snapshot.uuid()),
                centerX,
                top + 60,
                0xFF9999A3
        );
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.mathmaster.self_insight.intellect", snapshot.intellect()),
                centerX,
                top + 82,
                0xFFDFA6EE
        );

        Component experience = snapshot.experienceNeeded() <= 0
                ? Component.translatable("screen.mathmaster.self_insight.experience.max")
                : Component.translatable(
                        "screen.mathmaster.self_insight.experience",
                        Math.max(0, snapshot.experienceNeeded() - snapshot.experience()),
                        snapshot.experience(),
                        snapshot.experienceNeeded()
                );
        graphics.drawCenteredString(font, experience, centerX, top + 102, 0xFFB8D8FF);

        int pollutionColor = pollutionColor(snapshot.pollution());
        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.mathmaster.self_insight.pollution",
                        snapshot.pollution()
                ),
                centerX,
                top + 126,
                pollutionColor
        );
        int barLeft = left + 38;
        int barRight = right - 38;
        int barTop = top + 141;
        graphics.fill(barLeft - 1, barTop - 1, barRight + 1, barTop + 7, 0xFF08080A);
        graphics.fill(barLeft, barTop, barRight, barTop + 6, 0xFF302C32);
        int filled = (barRight - barLeft) * Math.max(0, Math.min(100, snapshot.pollution())) / 100;
        if (filled > 0) {
            graphics.fill(barLeft, barTop, barLeft + filled, barTop + 6, pollutionColor);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static int pollutionColor(int pollution) {
        if (pollution >= 75) {
            return 0xFFFF5555;
        }
        if (pollution >= 50) {
            return 0xFFFF9F32;
        }
        if (pollution >= 25) {
            return 0xFFFFFF55;
        }
        return 0xFF55FF88;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
