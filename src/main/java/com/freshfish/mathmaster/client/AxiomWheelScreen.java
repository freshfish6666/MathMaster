package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.EquippedAxiomCase;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.network.SelectAxiomSkillPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AxiomWheelScreen extends Screen {
    private static final int INNER_RADIUS = 25;
    private static final int OUTER_RADIUS = 82;
    private static final int CATEGORY_COUNT = 5;
    private static final AxiomDefinition.AxiomCategory[] CATEGORIES = AxiomDefinition.AxiomCategory.values();
    private static final List<List<Offset>> SECTOR_OFFSETS = createSectorOffsets();
    private static final List<Offset> LABEL_OFFSETS = createLabelOffsets();
    private final Map<AxiomDefinition.AxiomCategory, AxiomDefinition> activeAxioms =
            new EnumMap<>(AxiomDefinition.AxiomCategory.class);
    private AxiomDefinition.AxiomCategory hoveredCategory;
    private boolean cancelled;

    public AxiomWheelScreen() {
        super(Component.translatable("screen.mathmaster.axiom_wheel.title"));
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            EquippedAxiomCase.find(minecraft.player).ifPresent(caseStack -> {
                for (AxiomDefinition axiom : AxiomCaseItem.getAxioms(caseStack)) {
                    if (axiom.isActive()) {
                        this.activeAxioms.putIfAbsent(axiom.category(), axiom);
                    }
                }
            });
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The wheel draws its own translucent overlay. Calling the vanilla
        // implementation would blur both the world and the wheel itself.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x66000000);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.hoveredCategory = categoryAt(mouseX - centerX, mouseY - centerY);

        AxiomDefinition.AxiomCategory[] categories = CATEGORIES;
        for (int index = 0; index < categories.length; index++) {
            AxiomDefinition.AxiomCategory category = categories[index];
            int color = this.activeAxioms.containsKey(category) ? category.color() : 0xFF3B3B3B;
            if (category == this.hoveredCategory) {
                color = brighten(color);
            }
            drawSector(graphics, centerX, centerY, index, color);
        }
        graphics.fill(centerX - INNER_RADIUS, centerY - 9, centerX + INNER_RADIUS, centerY + 9, 0xEE171717);
        graphics.drawCenteredString(this.font, Component.translatable("screen.mathmaster.axiom_wheel.release"),
                centerX, centerY - 4, 0xFFE6E6E6);

        for (int index = 0; index < categories.length; index++) {
            AxiomDefinition.AxiomCategory category = categories[index];
            Offset label = LABEL_OFFSETS.get(index);
            int labelX = centerX + label.x();
            int labelY = centerY + label.y();
            graphics.drawCenteredString(this.font, Component.translatable(category.translationKey()),
                    labelX, labelY, category.color());
        }

        Component detail;
        if (this.hoveredCategory == null) {
            detail = this.title;
        } else {
            AxiomDefinition axiom = this.activeAxioms.get(this.hoveredCategory);
            detail = axiom == null
                    ? Component.translatable("screen.mathmaster.axiom_wheel.empty",
                            Component.translatable(this.hoveredCategory.translationKey()))
                    : Component.translatable(axiom.translationKey()).withStyle(this.hoveredCategory.formatting());
        }
        graphics.drawCenteredString(this.font, detail, centerX, centerY + OUTER_RADIUS + 12, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawSector(GuiGraphics graphics, int centerX, int centerY, int index, int color) {
        for (Offset offset : SECTOR_OFFSETS.get(index)) {
            int x = centerX + offset.x();
            int y = centerY + offset.y();
            graphics.fill(x - 2, y - 2, x + 2, y + 2, color);
        }
    }

    private static List<List<Offset>> createSectorOffsets() {
        List<List<Offset>> sectors = new ArrayList<>();
        for (int index = 0; index < CATEGORY_COUNT; index++) {
            List<Offset> offsets = new ArrayList<>();
            double centerAngle = -90.0D + index * 72.0D;
            for (double degrees = centerAngle - 35.0D; degrees <= centerAngle + 35.0D; degrees += 2.5D) {
                double radians = Math.toRadians(degrees);
                double cosine = Math.cos(radians);
                double sine = Math.sin(radians);
                for (int radius = INNER_RADIUS; radius <= OUTER_RADIUS; radius += 3) {
                    offsets.add(new Offset((int) Math.round(cosine * radius), (int) Math.round(sine * radius)));
                }
            }
            sectors.add(List.copyOf(offsets));
        }
        return List.copyOf(sectors);
    }

    private static List<Offset> createLabelOffsets() {
        List<Offset> labels = new ArrayList<>();
        for (int index = 0; index < CATEGORY_COUNT; index++) {
            double angle = Math.toRadians(-90.0D + index * 72.0D);
            labels.add(new Offset((int) Math.round(Math.cos(angle) * 57.0D),
                    (int) Math.round(Math.sin(angle) * 57.0D) - 4));
        }
        return List.copyOf(labels);
    }

    private record Offset(int x, int y) {
    }

    private static int brighten(int color) {
        int red = Math.min(255, ((color >> 16) & 255) + 38);
        int green = Math.min(255, ((color >> 8) & 255) + 38);
        int blue = Math.min(255, (color & 255) + 38);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static AxiomDefinition.AxiomCategory categoryAt(double offsetX, double offsetY) {
        double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
        if (distance < INNER_RADIUS || distance > OUTER_RADIUS + 10) {
            return null;
        }
        double degrees = Math.toDegrees(Math.atan2(offsetY, offsetX));
        double normalized = (degrees + 90.0D + 36.0D + 360.0D) % 360.0D;
        int index = (int) Math.floor(normalized / (360.0D / CATEGORY_COUNT));
        return CATEGORIES[index];
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (AxiomSkillKeyHandler.OPEN_WHEEL.matches(keyCode, scanCode)) {
            confirmSelection();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            this.cancelled = true;
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void confirmSelection() {
        if (!this.cancelled && this.hoveredCategory != null) {
            AxiomDefinition axiom = this.activeAxioms.get(this.hoveredCategory);
            if (axiom != null) {
                PacketDistributor.sendToServer(new SelectAxiomSkillPayload(axiom.id().toString()));
            }
        }
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
