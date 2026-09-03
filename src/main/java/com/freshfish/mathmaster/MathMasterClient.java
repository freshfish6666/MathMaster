package com.freshfish.mathmaster;

import com.freshfish.mathmaster.client.BookshelfQuizScreen;
import com.freshfish.mathmaster.client.InsightMirrorHud;
import com.freshfish.mathmaster.client.MathMasterGuideScreen;
import com.freshfish.mathmaster.client.InsightQuizScreen;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMenuTypes;
import com.freshfish.mathmaster.network.InsightFailurePayload;
import com.freshfish.mathmaster.quiz.QuizBank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod(value = MathMaster.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MathMaster.MODID, value = Dist.CLIENT)
public class MathMasterClient {
    public MathMasterClient() {
        InsightFailurePayload.setClientHandler(InsightMirrorHud::showTooDifficult);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.BOOKSHELF_QUIZ.get(), BookshelfQuizScreen::new);
        event.register(ModMenuTypes.MATHMASTER_GUIDE.get(), MathMasterGuideScreen::new);
        event.register(ModMenuTypes.INSIGHT_QUIZ.get(), InsightQuizScreen::new);
    }

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getToolTip().isEmpty()) {
            return;
        }

        if (isLingxuTool(event)) {
            event.getToolTip().add(
                    Component.translatable("tooltip.mathmaster.lingxu_tool.flow")
                            .withStyle(ChatFormatting.GRAY)
            );
        }

        if (event.getEntity() == null) {
            return;
        }

        QuizBank bank = QuizBank.byItem(event.getItemStack().getItem());
        if (bank == null) {
            return;
        }

        var progress = event.getEntity().getData(ModAttachments.QUIZ_PROGRESS);
        int total = progress.getDisplayTotalCount(bank);
        if (total < 0) {
            return;
        }

        Component suffix = Component.literal(
                " (" + progress.getDisplayCorrectCount(bank) + "/" + total + ")"
        ).withStyle(ChatFormatting.WHITE);
        event.getToolTip().set(0, event.getToolTip().get(0).copy().append(suffix));
    }

    private static boolean isLingxuTool(ItemTooltipEvent event) {
        return event.getItemStack().is(ModItems.LINGXU_SWORD.get())
                || event.getItemStack().is(ModItems.LINGXU_PICKAXE.get())
                || event.getItemStack().is(ModItems.LINGXU_AXE.get())
                || event.getItemStack().is(ModItems.LINGXU_SHOVEL.get())
                || event.getItemStack().is(ModItems.LINGXU_HOE.get());
    }
}
