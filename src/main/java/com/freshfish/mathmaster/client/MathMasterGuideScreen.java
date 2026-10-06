package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.menu.MathMasterGuideMenu;
import com.freshfish.mathmaster.network.ClaimStudyNotePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MathMasterGuideScreen extends BookViewScreen implements MenuAccess<MathMasterGuideMenu> {
    private static final int DIRECTORY_PAGE = 1;
    private static final int QUESTION_INDEX_PAGE = 10;

    private final MathMasterGuideMenu menu;
    private Button claimNoteButton;

    public MathMasterGuideScreen(MathMasterGuideMenu menu, Inventory inventory, Component title) {
        super(new BookAccess(buildPages(menu)));
        this.menu = menu;
    }

    @Override
    public MathMasterGuideMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void createMenuControls() {
        int buttonY = Math.min(this.height - 48, 196);
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.mathmaster.guide.directory"),
                button -> this.forcePage(DIRECTORY_PAGE)
        ).bounds(this.width / 2 - 100, buttonY, 98, 20).build());
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                button -> this.onClose()
        ).bounds(this.width / 2 + 2, buttonY, 98, 20).build());
        this.claimNoteButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.mathmaster.guide.claim_note"),
                button -> {
                    button.active = false;
                    PacketDistributor.sendToServer(new ClaimStudyNotePayload(this.menu.getQuizBank().dataId()));
                }
        ).bounds(this.width / 2 - 100, buttonY + 24, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateClaimNoteButton(partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int bookX = (this.width - IMAGE_WIDTH) / 2;
        graphics.drawString(
                this.font,
                Component.translatable(
                        "screen.mathmaster.guide.progress_header",
                        this.menu.getCorrectCount(),
                        this.menu.getTotalCount()
                ),
                bookX + PAGE_TEXT_X_OFFSET,
                18,
                0x5A4632,
                false
        );
    }

    private void updateClaimNoteButton(float partialTick) {
        if (this.claimNoteButton == null) {
            return;
        }
        if (!this.menu.isComplete()) {
            this.claimNoteButton.active = false;
            this.claimNoteButton.setMessage(Component.translatable("screen.mathmaster.guide.claim_note.incomplete"));
            return;
        }

        float cooldown = this.minecraft == null || this.minecraft.player == null
                ? 0.0F
                : this.minecraft.player.getCooldowns().getCooldownPercent(
                        this.menu.getQuizBank().studyNoteItem(),
                        partialTick
                );
        if (cooldown > 0.0F) {
            int seconds = Math.max(1, (int) Math.ceil(cooldown * this.menu.getQuizBank().difficulty()));
            this.claimNoteButton.active = false;
            this.claimNoteButton.setMessage(Component.translatable(
                    "screen.mathmaster.guide.claim_note.cooldown",
                    seconds
            ));
            return;
        }

        this.claimNoteButton.active = true;
        this.claimNoteButton.setMessage(Component.translatable("screen.mathmaster.guide.claim_note"));
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        super.onClose();
    }

    @Override
    public void removed() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.menu.removed(this.minecraft.player);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static List<Component> buildPages(MathMasterGuideMenu menu) {
        List<Component> pages = new ArrayList<>();
        pages.add(coverPage(menu));
        pages.add(directoryPage());
        pages.add(chapterPage("screen.mathmaster.guide.chapter.introduction", "screen.mathmaster.guide.introduction.body"));
        pages.add(chapterPage("screen.mathmaster.guide.chapter.structure", "screen.mathmaster.guide.structure.body"));
        pages.add(chapterPage("screen.mathmaster.guide.chapter.books", "screen.mathmaster.guide.books.body"));
        pages.add(chapterPage("screen.mathmaster.guide.chapter.iq", "screen.mathmaster.guide.iq.body"));
        pages.add(chapterPage("screen.mathmaster.guide.chapter.rewards", "screen.mathmaster.guide.rewards.body"));
        pages.add(chapterPage("screen.mathmaster.guide.chapter.progress", "screen.mathmaster.guide.progress.body"));
        pages.add(chapterPage("screen.mathmaster.guide.chapter.lingxu", "screen.mathmaster.guide.lingxu.body"));
        pages.add(chapterPage("screen.mathmaster.guide.chapter.further_study", "screen.mathmaster.guide.further_study.body"));
        pages.add(questionIndexPage(menu));

        for (MathMasterGuideMenu.QuestionEntry question : menu.getQuestions()) {
            pages.addAll(questionPages(question, menu.getTotalCount()));
        }
        return List.copyOf(pages);
    }

    private static Component coverPage(MathMasterGuideMenu menu) {
        return Component.empty()
                .append(Component.translatable("screen.mathmaster.guide.title")
                        .withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD))
                .append("\n\n")
                .append(Component.translatable("screen.mathmaster.guide.current_book")
                        .withStyle(ChatFormatting.DARK_GRAY))
                .append("\n")
                .append(menu.getQuizBank().bookItem().getDefaultInstance().getHoverName().copy()
                        .withStyle(ChatFormatting.BOLD))
                .append("\n\n")
                .append(Component.translatable(
                        "screen.mathmaster.guide.cover_progress",
                        menu.getCorrectCount(),
                        menu.getTotalCount()
                ).withStyle(ChatFormatting.DARK_GREEN))
                .append("\n\n")
                .append(pageLink("screen.mathmaster.guide.open_directory", DIRECTORY_PAGE))
                .append("\n")
                .append(pageLink("screen.mathmaster.guide.open_questions", QUESTION_INDEX_PAGE));
    }

    private static Component directoryPage() {
        MutableComponent page = Component.empty()
                .append(Component.translatable("screen.mathmaster.guide.directory")
                        .withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD))
                .append("\n\n");
        String[] chapterKeys = {
                "screen.mathmaster.guide.chapter.introduction",
                "screen.mathmaster.guide.chapter.structure",
                "screen.mathmaster.guide.chapter.books",
                "screen.mathmaster.guide.chapter.iq",
                "screen.mathmaster.guide.chapter.rewards",
                "screen.mathmaster.guide.chapter.progress",
                "screen.mathmaster.guide.chapter.lingxu",
                "screen.mathmaster.guide.chapter.further_study"
        };
        for (int index = 0; index < chapterKeys.length; index++) {
            page.append(pageLink(chapterKeys[index], index + 2)).append("\n");
        }
        return page.append(pageLink("screen.mathmaster.guide.questions", QUESTION_INDEX_PAGE));
    }

    private static Component chapterPage(String titleKey, String bodyKey) {
        return Component.empty()
                .append(Component.translatable(titleKey)
                        .withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD))
                .append("\n\n")
                .append(Component.translatable(bodyKey).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component questionIndexPage(MathMasterGuideMenu menu) {
        return Component.empty()
                .append(Component.translatable("screen.mathmaster.guide.questions")
                        .withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD))
                .append("\n\n")
                .append(menu.getQuizBank().bookItem().getDefaultInstance().getHoverName().copy()
                        .withStyle(ChatFormatting.BOLD))
                .append("\n")
                .append(Component.translatable(
                        "screen.mathmaster.guide.cover_progress",
                        menu.getCorrectCount(),
                        menu.getTotalCount()
                ).withStyle(ChatFormatting.DARK_GREEN))
                .append("\n\n")
                .append(Component.translatable("screen.mathmaster.guide.questions.explanation")
                        .withStyle(ChatFormatting.DARK_GRAY))
                .append("\n\n")
                .append(menu.getTotalCount() > 0
                        ? pageLink("screen.mathmaster.guide.questions.start", QUESTION_INDEX_PAGE + 1)
                        : Component.translatable("screen.mathmaster.guide.questions.empty")
                                .withStyle(ChatFormatting.RED));
    }

    private static List<Component> questionPages(MathMasterGuideMenu.QuestionEntry question, int totalCount) {
        MutableComponent page = Component.empty()
                .append(Component.translatable(
                        "screen.mathmaster.guide.question_number",
                        question.number(),
                        totalCount
                ).withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD))
                .append("\n\n");

        if (!question.correct()) {
            return List.of(page.append(Component.literal("???")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        }

        boolean english = QuizLanguagePreference.useEnglish(Minecraft.getInstance());
        Component content = page.append(Component.literal(question.question(english)).withStyle(ChatFormatting.DARK_GRAY))
                .append("\n\n")
                .append(Component.translatable("screen.mathmaster.guide.correct_answer")
                        .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD))
                .append("\n")
                .append(Component.literal(question.correctAnswer(english)).withStyle(ChatFormatting.DARK_GREEN));
        return paginate(content);
    }

    private static List<Component> paginate(Component content) {
        List<FormattedText> lines = Minecraft.getInstance().font.getSplitter()
                .splitLines(content, TEXT_WIDTH, Style.EMPTY);
        int maxLinesPerPage = TEXT_HEIGHT / Minecraft.getInstance().font.lineHeight;
        List<Component> pages = new ArrayList<>((lines.size() + maxLinesPerPage - 1) / maxLinesPerPage);

        for (int start = 0; start < lines.size(); start += maxLinesPerPage) {
            int end = Math.min(lines.size(), start + maxLinesPerPage);
            MutableComponent page = Component.empty();
            for (int index = start; index < end; index++) {
                if (index > start) {
                    page.append("\n");
                }
                page.append(copyFormattedText(lines.get(index)));
            }
            pages.add(page);
        }
        return pages.isEmpty() ? List.of(Component.empty()) : List.copyOf(pages);
    }

    private static Component copyFormattedText(FormattedText text) {
        MutableComponent copy = Component.empty();
        text.visit((style, value) -> {
            copy.append(Component.literal(value).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return copy;
    }

    private static Component pageLink(String translationKey, int zeroBasedPage) {
        return Component.translatable(translationKey)
                .withStyle(style -> style
                        .withColor(ChatFormatting.BLUE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.CHANGE_PAGE,
                                Integer.toString(zeroBasedPage + 1)
                        )));
    }
}
