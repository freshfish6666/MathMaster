package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import com.mojang.blaze3d.systems.RenderSystem;
import com.freshfish.mathmaster.menu.BookshelfQuizMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BookshelfQuizScreen extends AbstractContainerScreen<BookshelfQuizMenu> {
    private static final int MAX_PANEL_WIDTH = 360;
    private static final int MAX_PANEL_HEIGHT = 262;
    private static final int PANEL_PADDING = 12;
    private static final int BUTTON_GAP = 4;
    private static final int MIN_BUTTON_HEIGHT = 18;
    private static final int MAX_BUTTON_HEIGHT = 30;
    private static final int ACTION_ROW_GAP = 6;
    private static final int ACTION_BUTTON_GAP = 6;
    private static final int ACTION_BUTTON_HEIGHT = 20;
    private static final float FEEDBACK_SOUND_VOLUME = 1.0F;
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
    private final List<WrappedAnswerButton> answerButtons = new ArrayList<>(BookshelfQuizMenu.ANSWER_BUTTON_COUNT);
    private Button nextQuestionButton;
    private Button exitButton;
    private boolean answerPending;

    public BookshelfQuizScreen(BookshelfQuizMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = Math.max(1, Math.min(MAX_PANEL_WIDTH, this.width - 16));
        this.imageHeight = Math.max(1, Math.min(MAX_PANEL_HEIGHT, this.height - 16));
        super.init();
        this.answerButtons.clear();
        this.answerPending = false;

        String[] options = buildOptions();
        updateFontWarning(options);

        this.questionLabelY = this.fontWarningY >= 0 ? 50 : 40;
        this.questionStartY = this.questionLabelY + 12;

        int contentWidth = Math.max(1, this.imageWidth - PANEL_PADDING * 2);
        List<FormattedCharSequence> allQuestionLines = this.font.split(
                Component.literal(this.menu.getQuestionText()),
                contentWidth
        );
        int reservedButtonHeight = MIN_BUTTON_HEIGHT * BookshelfQuizMenu.ANSWER_BUTTON_COUNT
                + BUTTON_GAP * (BookshelfQuizMenu.ANSWER_BUTTON_COUNT - 1)
                + ACTION_ROW_GAP
                + ACTION_BUTTON_HEIGHT;
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
        int buttonSpace = this.imageHeight
                - buttonStartY
                - 8
                - BUTTON_GAP * (BookshelfQuizMenu.ANSWER_BUTTON_COUNT - 1)
                - ACTION_ROW_GAP
                - ACTION_BUTTON_HEIGHT;
        int buttonHeight = Math.max(
                12,
                Math.min(MAX_BUTTON_HEIGHT, buttonSpace / BookshelfQuizMenu.ANSWER_BUTTON_COUNT)
        );
        int buttonWidth = contentWidth;
        int startX = this.leftPos + PANEL_PADDING;
        int absoluteButtonY = this.topPos + buttonStartY;

        for (int index = 0; index < options.length; index++) {
            char label = (char) ('A' + index);
            Component answer = Component.literal(label + ". " + options[index]);
            int optionIndex = index;
            WrappedAnswerButton answerButton = this.addRenderableWidget(new WrappedAnswerButton(
                    startX,
                    absoluteButtonY + index * (buttonHeight + BUTTON_GAP),
                    buttonWidth,
                    buttonHeight,
                    answer,
                    button -> this.selectOption(optionIndex)
            ));
            this.answerButtons.add(answerButton);
        }

        int actionY = absoluteButtonY
                + BookshelfQuizMenu.ANSWER_BUTTON_COUNT * buttonHeight
                + (BookshelfQuizMenu.ANSWER_BUTTON_COUNT - 1) * BUTTON_GAP
                + ACTION_ROW_GAP;
        int actionButtonWidth = Math.max(1, (contentWidth - ACTION_BUTTON_GAP) / 2);
        this.nextQuestionButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.mathmaster.next_question"),
                button -> this.clickMenuButton(BookshelfQuizMenu.NEXT_QUESTION_BUTTON_ID)
        ).bounds(startX, actionY, actionButtonWidth, ACTION_BUTTON_HEIGHT).build());
        this.exitButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.mathmaster.exit"),
                button -> this.clickMenuButton(BookshelfQuizMenu.EXIT_BUTTON_ID)
        ).bounds(
                startX + actionButtonWidth + ACTION_BUTTON_GAP,
                actionY,
                contentWidth - actionButtonWidth - ACTION_BUTTON_GAP,
                ACTION_BUTTON_HEIGHT
        ).build());
        updateAnswerControls();
    }

    private void selectOption(int index) {
        if (this.menu.isAnswered()) {
            if (index != this.menu.getSelectedOptionIndex()) {
                if (this.menu.wasAnsweredCorrectly()) {
                    playCatMeow();
                } else {
                    playWrongAnswerWarning();
                }
            }
            return;
        }

        if (this.answerPending) {
            return;
        }

        this.answerPending = true;
        updateAnswerControls();
        clickMenuButton(index);
    }

    private void clickMenuButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void playWrongAnswerWarning() {
        SoundEvent sound = switch (ThreadLocalRandom.current().nextInt(5)) {
            case 0 -> SoundEvents.CREEPER_PRIMED;
            case 1 -> SoundEvents.GHAST_SCREAM;
            case 2 -> SoundEvents.SPIDER_AMBIENT;
            case 3 -> SoundEvents.ZOMBIE_AMBIENT;
            default -> SoundEvents.WARDEN_ROAR;
        };
        playFeedbackSound(sound, 1.0F);
    }

    private void playCatMeow() {
        boolean kitten = ThreadLocalRandom.current().nextBoolean();
        SoundEvent sound = kitten ? SoundEvents.CAT_AMBIENT : SoundEvents.CAT_PURREOW;
        float pitch = kitten ? 1.5F : 1.0F;
        playFeedbackSound(sound, pitch);
    }

    private void playFeedbackSound(SoundEvent sound, float pitch) {
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(sound, pitch, FEEDBACK_SOUND_VOLUME)
        );
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateAnswerControls();
    }

    private void updateAnswerControls() {
        if (this.nextQuestionButton == null || this.exitButton == null) {
            return;
        }

        boolean answered = this.menu.isAnswered();
        int selectedIndex = this.menu.getSelectedOptionIndex();
        if (answered) {
            this.answerPending = false;
        }

        for (int index = 0; index < this.answerButtons.size(); index++) {
            WrappedAnswerButton button = this.answerButtons.get(index);
            if (!answered) {
                button.active = !this.answerPending;
            } else {
                button.active = index != selectedIndex;
            }
        }

        this.nextQuestionButton.visible = answered;
        this.nextQuestionButton.active = answered;
        this.exitButton.visible = answered;
        this.exitButton.active = answered;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderAnswerMarker(graphics);
    }

    private void renderAnswerMarker(GuiGraphics graphics) {
        int selectedIndex = this.menu.getSelectedOptionIndex();
        if (selectedIndex < 0 || selectedIndex >= this.answerButtons.size()) {
            return;
        }

        WrappedAnswerButton button = this.answerButtons.get(selectedIndex);
        int markerX = button.getX() + button.getWidth() - 14;
        int markerY = button.getY() + (button.getHeight() - 7) / 2;
        if (this.menu.wasAnsweredCorrectly()) {
            drawCheckMark(graphics, markerX, markerY, 0xFF55FF55);
        } else {
            drawCrossMark(graphics, markerX, markerY, 0xFFFF5555);
        }
    }

    private static void drawCrossMark(GuiGraphics graphics, int x, int y, int color) {
        for (int offset = 0; offset < 7; offset++) {
            graphics.fill(x + offset, y + offset, x + offset + 1, y + offset + 1, color);
            graphics.fill(x + 6 - offset, y + offset, x + 7 - offset, y + offset + 1, color);
        }
    }

    private static void drawCheckMark(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y + 3, x + 2, y + 5, color);
        graphics.fill(x + 2, y + 5, x + 4, y + 7, color);
        graphics.fill(x + 4, y + 3, x + 6, y + 5, color);
        graphics.fill(x + 6, y + 1, x + 8, y + 3, color);
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
