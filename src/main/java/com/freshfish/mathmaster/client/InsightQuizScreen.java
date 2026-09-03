package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.menu.InsightQuizMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class InsightQuizScreen extends AbstractContainerScreen<InsightQuizMenu> {
    private static final int MAX_WIDTH = 350;
    private static final int MAX_HEIGHT = 250;
    private static final int PADDING = 12;
    private static final int MAX_BUTTON_HEIGHT = 27;
    private static final int MIN_BUTTON_HEIGHT = 17;
    private static final int BUTTON_GAP = 4;

    private final List<WrappedAnswerButton> answerButtons = new ArrayList<>(InsightQuizMenu.ANSWER_COUNT);
    private List<FormattedCharSequence> questionLines = List.of();
    private Button nextButton;
    private Button closeButton;
    private boolean answerPending;
    private int displayedQuestionNumber = -1;

    public InsightQuizScreen(InsightQuizMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = Math.max(1, Math.min(MAX_WIDTH, this.width - 16));
        this.imageHeight = Math.max(1, Math.min(MAX_HEIGHT, this.height - 16));
        super.init();
        this.answerButtons.clear();
        this.answerPending = false;
        this.displayedQuestionNumber = this.menu.getCurrentQuestionNumber();

        int contentWidth = this.imageWidth - PADDING * 2;
        updateQuestionContent(contentWidth);

        int startX = this.leftPos + PADDING;
        int buttonStartY = this.topPos + 116;
        int closeY = this.topPos + this.imageHeight - 28;
        int availableButtonHeight = closeY
                - buttonStartY
                - 6
                - BUTTON_GAP * (InsightQuizMenu.ANSWER_COUNT - 1);
        int buttonHeight = Math.max(
                MIN_BUTTON_HEIGHT,
                Math.min(MAX_BUTTON_HEIGHT, availableButtonHeight / InsightQuizMenu.ANSWER_COUNT)
        );
        List<String> answers = this.menu.getAnswers();
        for (int index = 0; index < InsightQuizMenu.ANSWER_COUNT; index++) {
            int answerIndex = index;
            char label = (char) ('A' + index);
            WrappedAnswerButton button = this.addRenderableWidget(new WrappedAnswerButton(
                    startX,
                    buttonStartY + index * (buttonHeight + BUTTON_GAP),
                    contentWidth,
                    buttonHeight,
                    Component.literal(label + ". " + answers.get(index)),
                    ignored -> selectAnswer(answerIndex)
            ));
            this.answerButtons.add(button);
        }

        this.nextButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.mathmaster.insight.next"),
                ignored -> clickMenuButton(InsightQuizMenu.NEXT_BUTTON_ID)
        ).bounds(this.leftPos + this.imageWidth / 2 - 104, closeY, 100, 20).build());
        this.closeButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.mathmaster.insight.close"),
                ignored -> clickMenuButton(InsightQuizMenu.CLOSE_BUTTON_ID)
        ).bounds(this.leftPos + this.imageWidth / 2 + 4, closeY, 100, 20).build());
        updateControls();
    }

    private void updateQuestionContent(int contentWidth) {
        List<FormattedCharSequence> allLines = this.font.split(
                Component.literal(this.menu.getQuestion()),
                contentWidth
        );
        this.questionLines = List.copyOf(allLines.subList(0, Math.min(6, allLines.size())));

        List<String> answers = this.menu.getAnswers();
        for (int index = 0; index < this.answerButtons.size(); index++) {
            char label = (char) ('A' + index);
            this.answerButtons.get(index).setMessage(
                    Component.literal(label + ". " + answers.get(index))
            );
        }
    }

    private void selectAnswer(int index) {
        if (this.menu.isAnswered() || this.answerPending) {
            return;
        }
        this.answerPending = true;
        updateControls();
        clickMenuButton(index);
    }

    private void clickMenuButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.displayedQuestionNumber != this.menu.getCurrentQuestionNumber()) {
            this.displayedQuestionNumber = this.menu.getCurrentQuestionNumber();
            this.answerPending = false;
            updateQuestionContent(this.imageWidth - PADDING * 2);
        }
        updateControls();
    }

    private void updateControls() {
        if (this.closeButton == null || this.nextButton == null) {
            return;
        }
        if (this.menu.isAnswered()) {
            this.answerPending = false;
        }
        for (WrappedAnswerButton button : this.answerButtons) {
            button.active = !this.menu.isAnswered() && !this.answerPending;
        }
        this.nextButton.active = this.menu.isAnswered();
        this.nextButton.setMessage(Component.translatable(this.menu.isLastQuestion()
                ? "screen.mathmaster.insight.finish"
                : "screen.mathmaster.insight.next"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderAnswerMarker(graphics);
    }

    private void renderAnswerMarker(GuiGraphics graphics) {
        int selected = this.menu.getSelectedAnswerIndex();
        if (selected < 0 || selected >= this.answerButtons.size()) {
            return;
        }
        WrappedAnswerButton button = this.answerButtons.get(selected);
        int x = button.getX() + button.getWidth() - 15;
        int y = button.getY() + (button.getHeight() - 7) / 2;
        int color = this.menu.wasCorrect() ? 0xFF55FF55 : 0xFFFF5555;
        if (this.menu.wasCorrect()) {
            graphics.fill(x, y + 3, x + 2, y + 5, color);
            graphics.fill(x + 2, y + 5, x + 4, y + 7, color);
            graphics.fill(x + 4, y + 3, x + 6, y + 5, color);
            graphics.fill(x + 6, y + 1, x + 8, y + 3, color);
        } else {
            for (int offset = 0; offset < 7; offset++) {
                graphics.fill(x + offset, y + offset, x + offset + 1, y + offset + 1, color);
                graphics.fill(x + 6 - offset, y + offset, x + 7 - offset, y + offset + 1, color);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = this.imageWidth / 2;
        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.mathmaster.insight.title", this.menu.getTargetName()),
                centerX,
                8,
                0xF6DDFF
        );
        graphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.mathmaster.insight.comparison",
                        this.menu.getPlayerIq(),
                        this.menu.getTargetIntellect()
                ),
                centerX,
                22,
                0xDDB7E8
        );
        graphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.mathmaster.insight.question_counter",
                        this.menu.getCurrentQuestionNumber(),
                        this.menu.getTotalQuestions(),
                        this.menu.getCorrectAnswers()
                ),
                centerX,
                35,
                0xE8D6A7
        );

        if (this.menu.isAnswered()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable(this.menu.wasCorrect()
                            ? "screen.mathmaster.insight.correct"
                            : "screen.mathmaster.insight.incorrect"),
                    centerX,
                    48,
                    this.menu.wasCorrect() ? 0x55FF55 : 0xFF5555
            );
        }

        int questionY = 60;
        for (int index = 0; index < this.questionLines.size(); index++) {
            graphics.drawCenteredString(
                    this.font,
                    this.questionLines.get(index),
                    centerX,
                    questionY + index * this.font.lineHeight,
                    0xFFFFFF
            );
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int right = this.leftPos + this.imageWidth;
        int bottom = this.topPos + this.imageHeight;
        graphics.fill(this.leftPos, this.topPos, right, bottom, 0xE0181020);
        graphics.fill(this.leftPos, this.topPos, right, this.topPos + 2, 0xFFD86EE8);
        graphics.fill(this.leftPos, bottom - 2, right, bottom, 0xFF6E3478);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 2, bottom, 0xFFD86EE8);
        graphics.fill(right - 2, this.topPos, right, bottom, 0xFF6E3478);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
