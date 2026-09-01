package com.freshfish.mathmaster.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MathSymbolFontChecker {
    private static final int UNSUPPORTED_SENTINEL = 0x10FFFF;

    private MathSymbolFontChecker() {
    }

    public static String findMissingSymbols(Font font, List<String> texts) {
        FontSet fontSet = font.getFontSet(Style.DEFAULT_FONT);
        BakedGlyph missingGlyph = fontSet.getGlyph(UNSUPPORTED_SENTINEL);
        Set<Integer> missing = new LinkedHashSet<>();

        for (String text : texts) {
            text.codePoints()
                    .filter(MathSymbolFontChecker::isMathematicalSymbol)
                    .filter(codePoint -> fontSet.getGlyph(codePoint) == missingGlyph)
                    .forEach(missing::add);
        }

        StringBuilder result = new StringBuilder();
        for (int codePoint : missing) {
            result.appendCodePoint(codePoint);
        }
        return result.toString();
    }

    private static boolean isMathematicalSymbol(int codePoint) {
        if (Character.getType(codePoint) == Character.MATH_SYMBOL) {
            return true;
        }

        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.GREEK
                || block == Character.UnicodeBlock.GREEK_EXTENDED
                || block == Character.UnicodeBlock.MATHEMATICAL_OPERATORS
                || block == Character.UnicodeBlock.SUPPLEMENTAL_MATHEMATICAL_OPERATORS
                || block == Character.UnicodeBlock.MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A
                || block == Character.UnicodeBlock.MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B
                || block == Character.UnicodeBlock.SUPERSCRIPTS_AND_SUBSCRIPTS
                || block == Character.UnicodeBlock.LETTERLIKE_SYMBOLS
                || block == Character.UnicodeBlock.NUMBER_FORMS;
    }
}
