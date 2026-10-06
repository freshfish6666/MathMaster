package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.item.StudyNoteItem;
import com.freshfish.mathmaster.menu.AxiomDeductionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AxiomDeductionScreen extends AbstractContainerScreen<AxiomDeductionMenu> {
    private static final int LIST_X = 13;
    private static final int LIST_Y = 68;
    private static final int LIST_WIDTH = 192;
    private static final int LIST_HEIGHT = 61;
    private static final int VISIBLE_ROWS = 3;
    private static final int ROW_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 7;

    private final Map<AxiomDefinition, Button> axiomButtons = new EnumMap<>(AxiomDefinition.class);
    private final List<AxiomDefinition> matchingAxioms = new ArrayList<>();
    private int scrollOffset;

    public AxiomDeductionScreen(AxiomDeductionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 218;
        this.imageHeight = 230;
        this.inventoryLabelX = 28;
        this.inventoryLabelY = 139;
    }

    @Override
    protected void init() {
        super.init();
        this.axiomButtons.clear();
        for (AxiomDefinition axiom : AxiomDefinition.values()) {
            Button button = this.addRenderableWidget(Button.builder(
                    Component.translatable(axiom.translationKey()).withStyle(axiom.category().formatting()),
                    ignored -> selectAxiom(axiom)
            ).bounds(this.leftPos + LIST_X + 2, this.topPos + LIST_Y, LIST_WIDTH - SCROLLBAR_WIDTH - 7, 18).build());
            this.axiomButtons.put(axiom, button);
        }
        updateButtons();
    }

    private void selectAxiom(AxiomDefinition axiom) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, axiom.ordinal());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        this.matchingAxioms.clear();
        for (AxiomDefinition axiom : AxiomDefinition.values()) {
            Button button = this.axiomButtons.get(axiom);
            button.visible = false;
            button.setTooltip(Tooltip.create(axiom.skillTooltip(Math.max(1, this.menu.getNoteLevel()))));
            if (axiom.acceptsMaterial(this.menu.getMaterial())) {
                this.matchingAxioms.add(axiom);
            }
        }
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScrollOffset());
        for (int row = 0; row < VISIBLE_ROWS && row + this.scrollOffset < this.matchingAxioms.size(); row++) {
            AxiomDefinition axiom = this.matchingAxioms.get(row + this.scrollOffset);
            Button button = this.axiomButtons.get(axiom);
            button.visible = true;
            button.active = this.menu.canApply(axiom);
            button.setY(this.topPos + LIST_Y + 1 + row * ROW_HEIGHT);
        }
    }

    private int maxScrollOffset() {
        return Math.max(0, this.matchingAxioms.size() - VISIBLE_ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D
                && this.isHovering(LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT, mouseX, mouseY)
                && maxScrollOffset() > 0) {
            this.scrollOffset = Mth.clamp(this.scrollOffset - (int) Math.signum(scrollY), 0, maxScrollOffset());
            updateButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        graphics.fill(this.leftPos, this.topPos, right, bottom, 0xFF4A3725);
        graphics.fill(this.leftPos + 3, this.topPos + 3, right - 3, this.topPos + 134, 0xF0201B18);
        graphics.fill(this.leftPos, this.topPos, right, this.topPos + 3, 0xFFCFB66F);
        graphics.fill(this.leftPos, bottom - 3, right, bottom, 0xFF6D5330);

        drawSlotFrame(graphics, this.leftPos + 58, this.topPos + 36, 0xFF8DB5D4);
        drawSlotFrame(graphics, this.leftPos + 142, this.topPos + 36, 0xFFD5A951);
        graphics.fill(this.leftPos + 78, this.topPos + 44, this.leftPos + 134, this.topPos + 46, 0xFF765B34);
        graphics.fill(this.leftPos + 104, this.topPos + 40, this.leftPos + 108, this.topPos + 50, 0xFFE4D080);

        graphics.fill(this.leftPos + LIST_X, this.topPos + LIST_Y,
                this.leftPos + LIST_X + LIST_WIDTH, this.topPos + LIST_Y + LIST_HEIGHT, 0xFF100E0C);
        graphics.fill(this.leftPos + LIST_X + LIST_WIDTH - SCROLLBAR_WIDTH, this.topPos + LIST_Y + 1,
                this.leftPos + LIST_X + LIST_WIDTH - 2, this.topPos + LIST_Y + LIST_HEIGHT - 1, 0xFF352C24);
        int maxOffset = maxScrollOffset();
        int thumbHeight = maxOffset == 0 ? LIST_HEIGHT - 4 : 18;
        int thumbTravel = LIST_HEIGHT - 4 - thumbHeight;
        int thumbY = this.topPos + LIST_Y + 2
                + (maxOffset == 0 ? 0 : Math.round((float) this.scrollOffset / maxOffset * thumbTravel));
        graphics.fill(this.leftPos + LIST_X + LIST_WIDTH - SCROLLBAR_WIDTH + 1, thumbY,
                this.leftPos + LIST_X + LIST_WIDTH - 3, thumbY + thumbHeight, 0xFFC9A85D);

        drawInventoryBackground(graphics);
    }

    private void drawInventoryBackground(GuiGraphics graphics) {
        int panelLeft = this.leftPos + 6;
        int panelTop = this.topPos + 134;
        int panelRight = this.leftPos + this.imageWidth - 6;
        int panelBottom = this.topPos + this.imageHeight - 4;
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFFC6C0B2);
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 2, 0xFFF2EBD9);
        graphics.fill(panelLeft, panelBottom - 2, panelRight, panelBottom, 0xFF675F54);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawInventorySlot(graphics, this.leftPos + 28 + column * 18, this.topPos + 151 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawInventorySlot(graphics, this.leftPos + 28 + column * 18, this.topPos + 209);
        }
    }

    private static void drawInventorySlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF6F695E);
        graphics.fill(x, y, x + 17, y + 17, 0xFFF0E9D8);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8579);
    }

    private static void drawSlotFrame(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 1, y - 1, x + 19, y + 19, 0xFF080706);
        graphics.fill(x, y, x + 18, y + 18, color);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF302923);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, 8, 0xFFF1DFA7);
        graphics.drawString(this.font, Component.translatable("screen.mathmaster.axiom.note_slot"), 39, 57, 0xFF9FCBE8, false);
        graphics.drawString(this.font, Component.translatable("screen.mathmaster.axiom.material_slot"), 123, 57, 0xFFE6C66E, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFD8CBAE, false);

        Component status = statusMessage();
        graphics.drawCenteredString(this.font, status, this.imageWidth / 2, 22, 0xFFC8BFAF);
    }

    private Component statusMessage() {
        if (this.menu.getNote().isEmpty()) {
            return Component.translatable("screen.mathmaster.axiom.status.insert_note");
        }
        var learned = StudyNoteItem.getAxiom(this.menu.getNote());
        if (learned.isPresent()) {
            return Component.translatable("screen.mathmaster.axiom.status.learned",
                    Component.translatable(learned.get().translationKey())
                            .withStyle(learned.get().category().formatting()));
        }
        if (this.menu.getMaterial().isEmpty()) {
            return Component.translatable("screen.mathmaster.axiom.status.insert_material",
                    this.menu.getNoteLevel());
        }
        boolean hasMatching = this.axiomButtons.entrySet().stream()
                .anyMatch(entry -> entry.getValue().visible && entry.getValue().active);
        var category = java.util.Arrays.stream(AxiomDefinition.values())
                .filter(axiom -> axiom.acceptsMaterial(this.menu.getMaterial()))
                .map(AxiomDefinition::category)
                .findFirst()
                .orElse(null);
        if (category == null) {
            return Component.translatable("screen.mathmaster.axiom.status.invalid_material");
        }
        Component categoryName = Component.translatable(category.translationKey())
                .withStyle(category.formatting());
        return hasMatching
                ? Component.translatable("screen.mathmaster.axiom.status.choose", categoryName)
                : Component.translatable("screen.mathmaster.axiom.status.locked", this.menu.getNoteLevel(), categoryName);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
