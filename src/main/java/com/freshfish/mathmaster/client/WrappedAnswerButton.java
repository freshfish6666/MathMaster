package com.freshfish.mathmaster.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class WrappedAnswerButton extends Button {
    public WrappedAnswerButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress
    ) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.setTooltip(Tooltip.create(message));
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        List<FormattedCharSequence> lines = font.split(this.getMessage(), Math.max(1, this.getWidth() - 10));
        int maxLines = Math.max(1, (this.getHeight() - 4) / font.lineHeight);
        int visibleLines = Math.min(lines.size(), maxLines);
        int startY = this.getY() + (this.getHeight() - visibleLines * font.lineHeight) / 2 + 1;

        for (int index = 0; index < visibleLines; index++) {
            graphics.drawCenteredString(
                    font,
                    lines.get(index),
                    this.getX() + this.getWidth() / 2,
                    startY + index * font.lineHeight,
                    color
            );
        }

        if (lines.size() > visibleLines) {
            graphics.drawString(
                    font,
                    "…",
                    this.getX() + this.getWidth() - 9,
                    startY + (visibleLines - 1) * font.lineHeight,
                    color,
                    false
            );
        }
    }
}
