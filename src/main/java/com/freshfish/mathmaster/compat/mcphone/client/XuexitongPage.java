package com.freshfish.mathmaster.compat.mcphone.client;

import com.freshfish.mathmaster.api.MathMasterApi;
import com.freshfish.mathmaster.client.learning.LearningCatalogClientState;
import com.freshfish.mathmaster.network.StartBookQuizPayload;
import com.november.mcphone.api.client.ui.IPhonePage;
import com.november.mcphone.api.client.ui.PhoneCanvas;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Two-level in-phone page: learning provider, then server-provided book catalog. */
public final class XuexitongPage implements IPhonePage {
    private static final int MARGIN = 6;
    private static final int TITLE_HEIGHT = 18;
    private static final int ROW_HEIGHT = 23;
    private static final int ROW_GAP = 3;

    private boolean showingBooks;
    private ButtonArea providerButton;
    private List<BookButtonArea> bookButtons = List.of();

    @Override
    public void onOpen() {
        LearningCatalogClientState.request();
    }

    @Override
    public void render(PhoneCanvas canvas) {
        GuiGraphics graphics = canvas.graphics();
        Font font = canvas.font();
        graphics.fill(
                canvas.x(),
                canvas.y(),
                canvas.x() + canvas.width(),
                canvas.y() + canvas.height(),
                canvas.style().screenBackground()
        );

        String title = Component.translatable("app.mathmaster.xuexitong").getString();
        graphics.drawCenteredString(
                font,
                title,
                canvas.x() + canvas.width() / 2,
                canvas.y() + 5,
                canvas.style().titleColor()
        );

        if (showingBooks) {
            renderBooks(canvas);
        } else {
            renderProviders(canvas);
        }
    }

    private void renderProviders(PhoneCanvas canvas) {
        int x = canvas.x() + MARGIN;
        int y = canvas.y() + TITLE_HEIGHT + 10;
        int width = canvas.width() - MARGIN * 2;
        int height = 30;
        providerButton = new ButtonArea(x, y, width, height);
        drawButton(
                canvas,
                providerButton,
                Component.translatable("app.mathmaster.xuexitong.provider.mathmaster").getString()
        );
        bookButtons = List.of();
    }

    private void renderBooks(PhoneCanvas canvas) {
        providerButton = null;
        List<MathMasterApi.QuizBook> books = LearningCatalogClientState.books();
        if (LearningCatalogClientState.isLoading()) {
            drawCenteredMessage(canvas, "app.mathmaster.xuexitong.loading");
            bookButtons = List.of();
            return;
        }
        if (books.isEmpty()) {
            drawCenteredMessage(canvas, "app.mathmaster.xuexitong.empty");
            bookButtons = List.of();
            return;
        }

        int x = canvas.x() + MARGIN;
        int y = canvas.y() + TITLE_HEIGHT + 2;
        int width = canvas.width() - MARGIN * 2;
        List<BookButtonArea> rendered = new ArrayList<>(books.size());
        for (MathMasterApi.QuizBook book : books) {
            ButtonArea area = new ButtonArea(x, y, width, ROW_HEIGHT);
            drawBookButton(canvas, area, book);
            rendered.add(new BookButtonArea(area, book));
            y += ROW_HEIGHT + ROW_GAP;
        }
        bookButtons = List.copyOf(rendered);
    }

    private static void drawBookButton(
            PhoneCanvas canvas,
            ButtonArea area,
            MathMasterApi.QuizBook book
    ) {
        GuiGraphics graphics = canvas.graphics();
        Font font = canvas.font();
        boolean hovered = area.contains(canvas.mouseX(), canvas.mouseY());
        graphics.fill(
                area.x(), area.y(), area.x() + area.width(), area.y() + area.height(),
                hovered ? canvas.style().buttonHoverColor() : canvas.style().buttonColor()
        );

        String difficulty = Component.translatable(
                "app.mathmaster.xuexitong.difficulty",
                book.difficulty()
        ).getString();
        int difficultyWidth = font.width(difficulty);
        int nameWidth = Math.max(8, area.width() - difficultyWidth - 12);
        String name = Component.translatable(book.translationKey()).getString();
        if (font.width(name) > nameWidth) {
            name = font.plainSubstrByWidth(name, Math.max(0, nameWidth - font.width("…"))) + "…";
        }
        int textY = area.y() + (area.height() - font.lineHeight) / 2;
        graphics.drawString(font, name, area.x() + 5, textY, canvas.style().bodyColor(), false);
        graphics.drawString(
                font,
                difficulty,
                area.x() + area.width() - difficultyWidth - 5,
                textY,
                canvas.style().accentColor(),
                false
        );
    }

    private static void drawButton(PhoneCanvas canvas, ButtonArea area, String label) {
        boolean hovered = area.contains(canvas.mouseX(), canvas.mouseY());
        canvas.graphics().fill(
                area.x(), area.y(), area.x() + area.width(), area.y() + area.height(),
                hovered ? canvas.style().buttonHoverColor() : canvas.style().buttonColor()
        );
        canvas.graphics().drawCenteredString(
                canvas.font(),
                label,
                area.x() + area.width() / 2,
                area.y() + (area.height() - canvas.font().lineHeight) / 2,
                canvas.style().bodyColor()
        );
    }

    private static void drawCenteredMessage(PhoneCanvas canvas, String translationKey) {
        canvas.graphics().drawCenteredString(
                canvas.font(),
                Component.translatable(translationKey),
                canvas.x() + canvas.width() / 2,
                canvas.y() + canvas.height() / 2 - canvas.font().lineHeight / 2,
                canvas.style().subtleColor()
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        if (!showingBooks && providerButton != null && providerButton.contains(mouseX, mouseY)) {
            showingBooks = true;
            return true;
        }
        if (showingBooks) {
            for (BookButtonArea entry : bookButtons) {
                if (entry.area().contains(mouseX, mouseY)) {
                    PacketDistributor.sendToServer(new StartBookQuizPayload(entry.book().id()));
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean onBack() {
        if (showingBooks) {
            showingBooks = false;
            return true;
        }
        return false;
    }

    private record ButtonArea(int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record BookButtonArea(ButtonArea area, MathMasterApi.QuizBook book) {
    }
}
