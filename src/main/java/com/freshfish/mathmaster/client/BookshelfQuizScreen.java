package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import com.mojang.blaze3d.systems.RenderSystem;
import com.freshfish.mathmaster.menu.BookshelfQuizMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class BookshelfQuizScreen extends AbstractContainerScreen<BookshelfQuizMenu> {
    private static final int MAX_PANEL_WIDTH = 360;
    private static final int MAX_PANEL_HEIGHT = 236;
    private static final int PANEL_PADDING = 12;
    private static final int BUTTON_GAP = 4;
    private static final int MIN_BUTTON_HEIGHT = 18;
    private static final int MAX_BUTTON_HEIGHT = 30;
    private static final ResourceLocation EXPERIENCE_BAR_BACKGROUND =
            ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final ResourceLocation EXPERIENCE_BAR_PROGRESS =
            ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");

    private List<FormattedCharSequence> questionLines = List.of();
    private int questionLabelY;
    private int questionStartY;
    private boolean questionTruncated;
    private Component fontWarning = Component.empty();
    private int fontWarningY = -1;
    private String lastLoggedMissingSymbols = "";

    public BookshelfQuizScreen(BookshelfQuizMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = Math.max(1, Math.min(MAX_PANEL_WIDTH, this.width - 16));
        this.imageHeight = Math.max(1, Math.min(MAX_PANEL_HEIGHT, this.height - 16));
        super.init();

        String[] options = buildOptions();
        updateFontWarning(options);

        this.questionLabelY = this.fontWarningY >= 0 ? 50 : 40;
        this.questionStartY = this.questionLabelY + 12;

        int contentWidth = Math.max(1, this.imageWidth - PANEL_PADDING * 2);
        List<FormattedCharSequence> allQuestionLines = this.font.split(
                Component.literal(this.menu.getQuestionText()),
                contentWidth
        );
        int reservedButtonHeight = MIN_BUTTON_HEIGHT * 4 + BUTTON_GAP * 3;
        int questionSpace = this.imageHeight
                - this.questionStartY
                - 8
                - reservedButtonHeight
                - 6;
        int maxQuestionLines = Math.max(1, Math.min(5, questionSpace / this.font.lineHeight));
        int visibleQuestionLines = Math.min(allQuestionLines.size(), maxQuestionLines);
        this.questionLines = List.copyOf(allQuestionLines.subList(0, visibleQuestionLines));
        this.questionTruncated = allQuestionLines.size() > visibleQuestionLines;

        int buttonStartY = this.questionStartY + visibleQuestionLines * this.font.lineHeight + 6;
        int buttonSpace = this.imageHeight - buttonStartY - 8 - BUTTON_GAP * 3;
        int buttonHeight = Math.max(
                12,
                Math.min(MAX_BUTTON_HEIGHT, buttonSpace / 4)
        );
        int buttonWidth = contentWidth;
        int startX = this.leftPos + PANEL_PADDING;
        int absoluteButtonY = this.topPos + buttonStartY;

        for (int index = 0; index < options.length; index++) {
            char label = (char) ('A' + index);
            Component answer = Component.literal(label + ". " + options[index]);
            int optionIndex = index;
            this.addRenderableWidget(new WrappedAnswerButton(
                    startX,
                    absoluteButtonY + index * (buttonHeight + BUTTON_GAP),
                    buttonWidth,
                    buttonHeight,
                    answer,
                    button -> this.selectOption(optionIndex)
            ));
        }
    }

    private void selectOption(int index) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = this.imageWidth / 2;

        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.mathmaster.iq", this.menu.getIntelligenceLevel()),
                centerX,
                6,
                0xFFFFFF
        );

        drawExperienceBar(graphics, centerX, 18, Math.min(160, Math.max(20, this.imageWidth - 40)));

        graphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.mathmaster.experience",
                        this.menu.getIntelligenceExperience(),
                        this.menu.getIntelligenceRequiredXp()
                ),
                centerX,
                26,
                0xFFFFFF
        );

        if (this.fontWarningY >= 0) {
            graphics.drawCenteredString(this.font, this.fontWarning, centerX, this.fontWarningY, 0xFFCC55);
        }

        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.mathmaster.question", this.menu.getQuestionNumber()),
                centerX,
                this.questionLabelY,
                0xFFFFFF
        );

        for (int index = 0; index < this.questionLines.size(); index++) {
            graphics.drawCenteredString(
                    this.font,
                    this.questionLines.get(index),
                    centerX,
                    this.questionStartY + index * this.font.lineHeight,
                    0xFFFFFF
            );
        }
        if (this.questionTruncated) {
            graphics.drawString(
                    this.font,
                    "…",
                    this.imageWidth - PANEL_PADDING - 6,
                    this.questionStartY + (this.questionLines.size() - 1) * this.font.lineHeight,
                    0xFFFFFF,
                    false
            );
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int right = this.leftPos + this.imageWidth;
        int bottom = this.topPos + this.imageHeight;
        graphics.fill(this.leftPos, this.topPos, right, bottom, 0xD0101010);
        graphics.fill(this.leftPos, this.topPos, right, this.topPos + 1, 0xFF8B8B8B);
        graphics.fill(this.leftPos, bottom - 1, right, bottom, 0xFF373737);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, bottom, 0xFF8B8B8B);
        graphics.fill(right - 1, this.topPos, right, bottom, 0xFF373737);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String[] buildOptions() {
        String[] options = new String[4];
        String[] wrongAnswers = this.menu.getWrongAnswers();
        int correctIndex = this.menu.getCorrectOptionIndex();
        int wrongIndex = 0;

        for (int i = 0; i < options.length; i++) {
            if (i == correctIndex) {
                options[i] = this.menu.getCorrectAnswer();
            } else {
                options[i] = wrongAnswers[wrongIndex++];
            }
        }

        return options;
    }

    private void updateFontWarning(String[] options) {
        List<String> displayedTexts = new ArrayList<>(options.length + 1);
        displayedTexts.add(this.menu.getQuestionText());
        displayedTexts.addAll(List.of(options));
        String missingSymbols = MathSymbolFontChecker.findMissingSymbols(this.font, displayedTexts);

        if (missingSymbols.isEmpty()) {
            this.fontWarning = Component.empty();
            this.fontWarningY = -1;
            return;
        }

        this.fontWarning = Component.translatable("screen.mathmaster.font_missing", missingSymbols);
        this.fontWarningY = 38;
        if (!missingSymbols.equals(this.lastLoggedMissingSymbols)) {
            MathMaster.LOGGER.warn("The active font is missing math symbols used by this quiz: {}", missingSymbols);
            this.lastLoggedMissingSymbols = missingSymbols;
        }
    }

    private void drawExperienceBar(GuiGraphics graphics, int centerX, int y, int width) {
        int x = centerX - width / 2;
        graphics.blitSprite(EXPERIENCE_BAR_BACKGROUND, x, y, width, 5);

        int required = this.menu.getIntelligenceRequiredXp();
        int progress = 0;
        if (required > 0) {
            progress = Math.min(width, (int) ((long) this.menu.getIntelligenceExperience() * width / required));
        }

        if (progress > 0) {
            RenderSystem.enableBlend();
            graphics.blitSprite(EXPERIENCE_BAR_PROGRESS, 182, 5, 0, 0, x, y, progress, 5);
            RenderSystem.disableBlend();
        }
    }
}
