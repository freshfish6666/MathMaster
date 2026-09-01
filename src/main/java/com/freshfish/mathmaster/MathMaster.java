package com.freshfish.mathmaster;

import com.freshfish.mathmaster.event.BookshelfInteractionHandler;
import com.freshfish.mathmaster.event.FoodIntelligenceHandler;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModCreativeTabs;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMenuTypes;
import com.freshfish.mathmaster.quiz.QuizQuestionManager;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Mod(MathMaster.MODID)
public class MathMaster {
    public static final String MODID = "mathmaster";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MathMaster(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        NeoForge.EVENT_BUS.register(new BookshelfInteractionHandler());
        NeoForge.EVENT_BUS.register(new FoodIntelligenceHandler());
        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("MathMaster common setup");
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(QuizQuestionManager.INSTANCE);
    }
}
