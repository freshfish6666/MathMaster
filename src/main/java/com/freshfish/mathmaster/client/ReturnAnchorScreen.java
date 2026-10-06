package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.axiom.ReturnAnchorData.Anchor;
import com.freshfish.mathmaster.network.ReturnAnchorsPayload;
import com.freshfish.mathmaster.network.ReturnControlPayload;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** A small owner-only anchor list; coordinates are always supplied by the server. */
public final class ReturnAnchorScreen extends Screen {
    private int level, selected, confirmation = -1;
    private List<Optional<Anchor>> anchors;
    private Button record, delete;
    private int left, top, panelWidth, panelHeight, rowHeight;

    public ReturnAnchorScreen(ReturnAnchorsPayload payload) {
        super(Component.translatable("screen.mathmaster.return.title"));
        level = payload.level(); selected = payload.selected(); anchors = payload.anchors();
    }

    public static void accept(ReturnAnchorsPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isAlive()) return;
        if (minecraft.screen instanceof ReturnAnchorScreen screen) {
            if (payload.level() < 2) { screen.onClose(); return; }
            if (screen.selected != payload.selected() || screen.level != payload.level() || !screen.anchors.equals(payload.anchors())) {
                screen.confirmation = -1;
            }
            screen.level = payload.level(); screen.selected = payload.selected(); screen.anchors = payload.anchors();
            screen.rebuildWidgets();
        } else if (payload.open() && payload.level() >= 2 && minecraft.screen == null && minecraft.isWindowActive()) {
            minecraft.setScreen(new ReturnAnchorScreen(payload));
        } else if (payload.open() && minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new ReturnControlPayload(ReturnControlPayload.CLOSE, -1));
        }
    }

    @Override protected void init() {
        panelWidth = Math.min(340, width - 16);
        rowHeight = Math.min(34, Math.max(22, (height - 96) / Math.max(1, level)));
        panelHeight = 82 + level * rowHeight;
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        for (int i = 0; i < level; i++) {
            final int slot = i;
            addRenderableWidget(new AnchorButton(left + 12, top + 34 + i * rowHeight, panelWidth - 24, rowHeight - 2, slot));
        }
        int buttonWidth = (panelWidth - 30) / 2;
        record = addRenderableWidget(Button.builder(Component.empty(), button -> recordOrConfirm())
                .bounds(left + 12, top + panelHeight - 30, buttonWidth, 20).build());
        delete = addRenderableWidget(Button.builder(Component.empty(), button -> deleteOrCancel())
                .bounds(left + 18 + buttonWidth, top + panelHeight - 30, buttonWidth, 20).build());
        refreshButtons();
    }

    private void refreshButtons() {
        boolean available = selected >= 0 && selected < level;
        record.setMessage(Component.translatable(confirmation >= 0 ? "screen.mathmaster.return.confirm" : "screen.mathmaster.return.record"));
        delete.setMessage(Component.translatable(confirmation >= 0 ? "screen.mathmaster.return.cancel" : "screen.mathmaster.return.delete"));
        record.active = available;
        delete.active = confirmation >= 0 || available && anchors.get(selected).isPresent();
    }

    private void recordOrConfirm() {
        if (confirmation >= 0) {
            send(confirmation, selected); confirmation = -1;
        } else if (anchors.get(selected).isPresent()) {
            confirmation = ReturnControlPayload.RECORD;
        } else send(ReturnControlPayload.RECORD, selected);
        refreshButtons();
    }

    private void deleteOrCancel() {
        confirmation = confirmation >= 0 ? -1 : ReturnControlPayload.DELETE;
        refreshButtons();
    }

    private void select(int slot) {
        selected = slot; confirmation = -1;
        send(ReturnControlPayload.SELECT, slot);
        refreshButtons();
    }

    private static void send(int action, int slot) { PacketDistributor.sendToServer(new ReturnControlPayload(action, slot)); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() {
        if (Minecraft.getInstance().getConnection() != null) send(ReturnControlPayload.CLOSE, -1);
    }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0161B25);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFF8DA8D9);
        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFFE6ECFF);
        if (confirmation >= 0) graphics.drawCenteredString(font,
                Component.translatable(confirmation == ReturnControlPayload.RECORD ? "screen.mathmaster.return.overwrite" : "screen.mathmaster.return.delete_confirm"),
                width / 2, top + panelHeight - 44, 0xFFFFD7A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component dimension(Anchor anchor) {
        String key = "dimension." + anchor.dimension().getNamespace() + "." + anchor.dimension().getPath().replace('/', '.');
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(anchor.dimension().toString());
    }

    private final class AnchorButton extends Button {
        private final int slot;
        private AnchorButton(int x, int y, int width, int height, int slot) {
            super(x, y, width, height, Component.translatable("screen.mathmaster.return.slot", slot + 1),
                    button -> select(slot), DEFAULT_NARRATION);
            this.slot = slot;
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean chosen = selected == slot;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), chosen ? 0xFF344766 : isHoveredOrFocused() ? 0xFF29354B : 0xFF202836);
            if (chosen) graphics.fill(getX(), getY(), getX() + 2, getY() + getHeight(), 0xFFB5CCFF);
            var anchor = anchors.get(slot);
            Component label = Component.translatable("screen.mathmaster.return.slot_name", slot + 1,
                    anchor.map(ReturnAnchorScreen.this::dimension).orElse(Component.translatable("screen.mathmaster.return.empty")));
            graphics.drawString(font, font.plainSubstrByWidth(label.getString(), getWidth() - 16), getX() + 8, getY() + 3, 0xFFE6ECFF, false);
            if (anchor.isPresent()) {
                Anchor value = anchor.get();
                String coordinates = String.format(Locale.ROOT, "X %.2f   Y %.2f   Z %.2f", value.x(), value.y(), value.z());
                graphics.drawString(font, font.plainSubstrByWidth(coordinates, getWidth() - 16), getX() + 8, getY() + getHeight() - 11, 0xFFBAC5D8, false);
            }
        }
    }
}
