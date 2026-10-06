package com.freshfish.mathmaster.compat.mcphone.client;

import com.freshfish.mathmaster.MathMaster;
import com.november.mcphone.api.client.app.IPhoneApp;
import com.november.mcphone.api.client.ui.IPhonePage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Optional MCphone application. Discovered only through MCphone's client-side SPI. */
public final class XuexitongApp implements IPhoneApp {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "xuexitong");
    private static final ResourceLocation ICON =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "textures/app/xuexitong.png");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("app.mathmaster.xuexitong");
    }

    @Override
    public ResourceLocation getIconTexture() {
        return ICON;
    }

    @Override
    public void onPress() {
        // openPage() is supported by the MCphone API version declared by MathMaster.
    }

    @Override
    public IPhonePage openPage() {
        return new XuexitongPage();
    }

    @Override
    public String getAuthor() {
        return "freshfish6666";
    }

    @Override
    public String getDescription() {
        return Component.translatable("app.mathmaster.xuexitong.description").getString();
    }
}
