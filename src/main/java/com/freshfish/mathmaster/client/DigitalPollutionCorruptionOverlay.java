package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.init.ModSounds;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MathMaster.MODID, value = Dist.CLIENT)
public final class DigitalPollutionCorruptionOverlay {
    private static final ResourceLocation FLESH_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MathMaster.MODID,
            "textures/gui/digital_pollution_flesh.png"
    );
    private static final String[] GLYPHS = {
            "0", "1", "6", "7", "8", "9", "∑", "∫", "∞", "√", "π", "∆", "≠", "∅",
            "x²", "f(x)", "lim", "1/0", "Σn", "{?}", "[ ]", "?=?", "NULL", "NaN"
    };
    private static final String[] SENTENCES = {
            "0 零 NULL の 문 ∅",
            "Σ(nombre) = 影 kein 수",
            "f(∅) // 8 8 8 // NaN",
            "1/0 se souvient 기억 1/0",
            "lim x→影 [du] = 零",
            "{ 九, acht, nana, 영, ∅ }",
            "ROOT(ROOT(ROOT(NULL)))",
            "la somme は kein 항",
            "0x0 ≠ 0x0 ≠ 0x0",
            "compte zurück ∞부터",
            "∫ flesh d(number) = ?",
            "suite 受理 // Körper 거부",
            "π π π // END OF INPUT",
            "[1][1][0][?][9][∅]",
            "index 你的 wurde 지움",
            "√-1 IS LOOKING THROUGH",
            "aucun 軸 kein 출구",
            "CARDINALITY: UNDEFINED",
            "∅ n'est 空 nicht 비어",
            "REMAINDER = OBSERVER",
            "0 parle 基数 ∅ 로",
            "THE LAST DIGIT BLINKED",
            "∀x ∃? : x = NOT x",
            "ne finis 証明 nicht 완성"
    };

    private static int pollution;
    private static long clientTicks;
    private static long nextWhisperTick;
    private static long nextSentenceTick;
    private static long sentenceEndsAt;
    private static String sentence = "";
    private static GlyphLayout glyphLayout;

    private DigitalPollutionCorruptionOverlay() {
    }

    public static void accept(int value) {
        int previous = pollution;
        pollution = Math.max(0, Math.min(99, value));
        if (previous <= 80 && pollution > 80) {
            nextWhisperTick = clientTicks + 40L;
        }
        if (previous <= 95 && pollution > 95) {
            nextWhisperTick = Math.min(nextWhisperTick, clientTicks + 20L);
            nextSentenceTick = clientTicks + 40L;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !minecraft.player.isAlive()) {
            resetTransientState();
            return;
        }

        clientTicks++;
        if (pollution <= 80) {
            nextWhisperTick = clientTicks + 40L;
        } else if (clientTicks >= nextWhisperTick) {
            boolean critical = pollution > 95;
            float volume = critical ? 0.52F : 0.18F;
            float pitch = 0.88F + minecraft.player.getRandom().nextFloat() * 0.2F;
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(ModSounds.DIGITAL_POLLUTION_WHISPER.get(), pitch, volume)
            );
            nextWhisperTick = clientTicks + randomBetween(minecraft, critical ? 95 : 260, critical ? 150 : 420);
        }

        if (pollution <= 95) {
            sentence = "";
            sentenceEndsAt = 0L;
            nextSentenceTick = clientTicks + 40L;
        } else if (clientTicks >= nextSentenceTick) {
            sentence = SENTENCES[minecraft.player.getRandom().nextInt(SENTENCES.length)];
            sentenceEndsAt = clientTicks + randomBetween(minecraft, 32, 62);
            nextSentenceTick = sentenceEndsAt + randomBetween(minecraft, 70, 170);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (pollution <= 50 || minecraft.player == null || !minecraft.player.isAlive()) {
            return;
        }

        int tier = pollution > 95 ? 3 : pollution > 80 ? 2 : 1;
        GuiGraphics graphics = event.getGuiGraphics();
        if (tier == 1 && clientTicks % 130L >= 42L) {
            return;
        }

        drawEdgeGlyphs(graphics, minecraft, tier);
        if (tier == 3 && clientTicks < sentenceEndsAt && !sentence.isEmpty()) {
            drawSentence(graphics, minecraft);
        }
    }

    /** Retained for a future screen or effect; digital pollution does not call it. */
    public static void renderFleshOverlay(GuiGraphics graphics) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.setColor(1.0F, 1.0F, 1.0F, 0.18F);
        blitFlesh(graphics, width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void blitFlesh(GuiGraphics graphics, int width, int height) {
        graphics.blit(FLESH_TEXTURE, 0, 0, width, height,
                0.0F, 0.0F, 256, 256, 256, 256);
    }

    private static void drawEdgeGlyphs(GuiGraphics graphics, Minecraft minecraft, int tier) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        long frame = clientTicks / (tier == 3 ? 2L : tier == 2 ? 4L : 7L);
        if (glyphLayout == null || glyphLayout.width() != width || glyphLayout.height() != height
                || glyphLayout.tier() != tier || glyphLayout.pollution() != pollution || glyphLayout.frame() != frame) {
            glyphLayout = createGlyphLayout(width, height, tier, pollution, frame);
        }
        for (EdgeGlyph glyph : glyphLayout.glyphs()) {
            graphics.pose().pushPose();
            graphics.pose().translate(glyph.x(), glyph.y(), 0.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(glyph.angle()));
            graphics.pose().scale(glyph.scale(), glyph.scale(), 1.0F);
            graphics.drawString(minecraft.font, glyph.text(), 0, 0, glyph.color(), false);
            if (glyph.echoOffset() != 0) {
                graphics.drawString(minecraft.font, glyph.text(), glyph.echoOffset(), 0, 0x5000B7B0, false);
            }
            graphics.pose().popPose();
        }
    }

    private static GlyphLayout createGlyphLayout(int width, int height, int tier, int pollution, long frame) {
        int shortSide = Math.min(width, height);
        int depth = tier == 3 ? Math.max(32, shortSide / 4)
                : tier == 2 ? Math.max(22, shortSide / 8)
                : Math.max(12, shortSide / 16);
        int count = tier == 3 ? clamp((width + height) / 7, 64, 150)
                : tier == 2 ? clamp((width + height) / 16, 28, 80)
                : clamp((width + height) / 55, 7, 18);
        Random random = new Random(frame * 0x9E3779B97F4A7C15L + pollution * 7919L);
        List<EdgeGlyph> glyphs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String glyph = GLYPHS[random.nextInt(GLYPHS.length)];
            int edge = random.nextInt(4);
            int x;
            int y;
            if (edge == 0 || edge == 1) {
                x = random.nextInt(Math.max(1, width));
                y = edge == 0 ? random.nextInt(depth) : height - 1 - random.nextInt(depth);
            } else {
                x = edge == 2 ? random.nextInt(depth) : width - 1 - random.nextInt(depth);
                y = random.nextInt(Math.max(1, height));
            }

            float scale = (tier == 3 ? 0.8F : 0.65F) + random.nextFloat() * (tier == 3 ? 1.15F : 0.7F);
            float angle = (random.nextFloat() - 0.5F) * (tier == 3 ? 64.0F : 34.0F);
            int alpha = tier == 3 ? 105 + random.nextInt(86)
                    : tier == 2 ? 58 + random.nextInt(72)
                    : 32 + random.nextInt(38);
            int rgb = switch (random.nextInt(5)) {
                case 0 -> 0xFF4B58;
                case 1 -> 0x7CE8DE;
                case 2 -> 0xD7C9A4;
                default -> 0xE6E0DA;
            };
            int color = alpha << 24 | rgb;

            int echoOffset = 0;
            if (tier == 3 && random.nextBoolean()) {
                echoOffset = random.nextBoolean() ? 1 : -1;
            }
            glyphs.add(new EdgeGlyph(glyph, x, y, scale, angle, color, echoOffset));
        }
        return new GlyphLayout(width, height, tier, pollution, frame, List.copyOf(glyphs));
    }

    private record EdgeGlyph(String text, int x, int y, float scale, float angle, int color, int echoOffset) {
    }

    private record GlyphLayout(int width, int height, int tier, int pollution, long frame, List<EdgeGlyph> glyphs) {
    }

    private static void drawSentence(GuiGraphics graphics, Minecraft minecraft) {
        long age = sentenceEndsAt - clientTicks;
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int jitterX = (int) Math.floorMod(clientTicks * 17L, 5L) - 2;
        int jitterY = (int) Math.floorMod(clientTicks * 11L, 3L) - 1;
        int alpha = (int) Math.min(220L, Math.max(55L, age * 8L));
        int textWidth = minecraft.font.width(sentence);

        graphics.fill(centerX - textWidth / 2 - 7, centerY - 7,
                centerX + textWidth / 2 + 7, centerY + 13, 0x45000000);
        graphics.drawCenteredString(minecraft.font, sentence, centerX - 2, centerY + 1,
                0x6500C7C0);
        graphics.drawCenteredString(minecraft.font, sentence, centerX + jitterX, centerY + jitterY,
                alpha << 24 | 0xFFE9E4);
        if (clientTicks % 5L == 0L) {
            graphics.fill(centerX - textWidth / 2, centerY + 10,
                    centerX + textWidth / 2, centerY + 11, 0x75FF1018);
        }
    }

    private static int randomBetween(Minecraft minecraft, int minimum, int maximum) {
        return minimum + minecraft.player.getRandom().nextInt(maximum - minimum + 1);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void resetTransientState() {
        pollution = 0;
        clientTicks = 0L;
        nextWhisperTick = 0L;
        nextSentenceTick = 0L;
        sentenceEndsAt = 0L;
        sentence = "";
        glyphLayout = null;
    }
}
