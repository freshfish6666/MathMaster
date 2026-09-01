package com.freshfish.mathmaster;

import com.freshfish.mathmaster.client.BookshelfQuizScreen;
import com.freshfish.mathmaster.init.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = MathMaster.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MathMaster.MODID, value = Dist.CLIENT)
public class MathMasterClient {
    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.BOOKSHELF_QUIZ.get(), BookshelfQuizScreen::new);
    }
}