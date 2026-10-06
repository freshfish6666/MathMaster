package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.item.StudyNoteItem;
import com.freshfish.mathmaster.menu.AxiomCaseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AxiomCaseScreen extends AbstractContainerScreen<AxiomCaseMenu> {
    public AxiomCaseScreen(AxiomCaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 154;
        this.inventoryLabelY = 60;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int right = this.leftPos + this.imageWidth;
        int bottom = this.topPos + this.imageHeight;
        graphics.fill(this.leftPos, this.topPos, right, bottom, 0xFF4B3828);
        graphics.fill(this.leftPos + 3, this.topPos + 3, right - 3, this.topPos + 56, 0xF0211B18);
        graphics.fill(this.leftPos + 3, this.topPos + 56, right - 3, bottom - 3, 0xFFC6C0B2);
        graphics.fill(this.leftPos, this.topPos, right, this.topPos + 2, 0xFFD3B568);
        graphics.fill(this.leftPos, bottom - 2, right, bottom, 0xFF6A4D2D);

        int caseStartX = 8 + (9 - this.menu.getCapacity()) * 9;
        for (int index = 0; index < this.menu.getCapacity(); index++) {
            int slotColor = StudyNoteItem.getAxiom(this.menu.getCaseItem(index))
                    .map(axiom -> darken(axiom.category().color()))
                    .orElse(0xFF76684B);
            drawSlot(graphics, this.leftPos + caseStartX + index * 18, this.topPos + 27, slotColor);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 72 + row * 18, 0xFF8B8579);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 130, 0xFF8B8579);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int innerColor) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF514A40);
        graphics.fill(x, y, x + 17, y + 17, 0xFFE9DFC9);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, innerColor);
    }

    private static int darken(int color) {
        int red = ((color >> 16) & 255) * 2 / 3;
        int green = ((color >> 8) & 255) * 2 / 3;
        int blue = (color & 255) * 2 / 3;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, 8, 0xFFF0DFA9);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.mathmaster.axiom_case.slots", this.menu.getCapacity()),
                this.imageWidth / 2,
                17,
                0xFFBDB4A5
        );
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                0xFF403A32, false);
        var conflict = this.menu.carriedConflictCategory();
        if (conflict != null) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable(
                            "screen.mathmaster.axiom_case.active_conflict",
                            Component.translatable(conflict.translationKey()).withStyle(conflict.formatting())
                    ),
                    this.imageWidth / 2,
                    48,
                    0xFFFF7777
            );
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
