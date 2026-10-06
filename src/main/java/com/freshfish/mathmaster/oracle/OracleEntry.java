package com.freshfish.mathmaster.oracle;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Stable identity and fallback text allow new entries without a language file. */
public record OracleEntry(ResourceLocation id, String text) {
    public Component message() {
        return Component.translatableWithFallback(
                "oracle." + this.id.getNamespace() + "." + this.id.getPath().replace('/', '.'), this.text);
    }
}
