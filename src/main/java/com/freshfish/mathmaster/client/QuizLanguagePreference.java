package com.freshfish.mathmaster.client;

import net.minecraft.client.Minecraft;

public final class QuizLanguagePreference {
    private static Boolean english;

    private QuizLanguagePreference() {
    }

    public static boolean useEnglish(Minecraft minecraft) {
        if (english == null) {
            english = !"zh_cn".equalsIgnoreCase(
                    minecraft.getLanguageManager().getSelected()
            );
        }
        return english;
    }

    public static boolean toggle(Minecraft minecraft) {
        english = !useEnglish(minecraft);
        return english;
    }
}
