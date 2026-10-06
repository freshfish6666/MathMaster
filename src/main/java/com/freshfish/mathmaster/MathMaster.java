package com.freshfish.mathmaster;

import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.tree.CollatzTreeGrowth;
import com.freshfish.mathmaster.axiom.AdditionCommutativitySkill;
import com.freshfish.mathmaster.axiom.AxiomCooldownHandler;
import com.freshfish.mathmaster.axiom.AdditiveInverseSkill;
import com.freshfish.mathmaster.axiom.EuclidPrimeInfinitySkill;
import com.freshfish.mathmaster.axiom.PlayfairAxiomSkill;
import com.freshfish.mathmaster.axiom.InvolutionSkill;
import com.freshfish.mathmaster.axiom.GeodesicSkill;
import com.freshfish.mathmaster.axiom.ReturnSkill;
import com.freshfish.mathmaster.network.ReturnControlPayload;
import com.freshfish.mathmaster.network.ReturnAnchorsPayload;
import com.freshfish.mathmaster.axiom.ShannonEntropySkill;
import com.freshfish.mathmaster.event.BookshelfInteractionHandler;
import com.freshfish.mathmaster.event.AxiomPassiveHandler;
import com.freshfish.mathmaster.event.FoodIntelligenceHandler;
import com.freshfish.mathmaster.event.FlowEffectHandler;
import com.freshfish.mathmaster.event.FivePlatformHandler;
import com.freshfish.mathmaster.event.DigitalPollutionHandler;
import com.freshfish.mathmaster.event.DigitallyCorruptedBlockEffectHandler;
import com.freshfish.mathmaster.event.InsightMirrorInteractionHandler;
import com.freshfish.mathmaster.intellect.EntityIntellectCombatHandler;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.intellect.InsightQuestionManager;
import com.freshfish.mathmaster.integration.IntegrationManager;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModArmorMaterials;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModBlockEntities;
import com.freshfish.mathmaster.init.ModCreativeTabs;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModMenuTypes;
import com.freshfish.mathmaster.init.ModMobEffects;
import com.freshfish.mathmaster.init.ModPotions;
import com.freshfish.mathmaster.init.ModSounds;
import com.freshfish.mathmaster.init.ModStructures;
import com.freshfish.mathmaster.network.InsightFailurePayload;
import com.freshfish.mathmaster.network.InvolutionStatePayload;
import com.freshfish.mathmaster.network.GeodesicControlPayload;
import com.freshfish.mathmaster.network.GeodesicStatePayload;
import com.freshfish.mathmaster.network.DigitalPollutionMeterPayload;
import com.freshfish.mathmaster.network.LearningAppNetworking;
import com.freshfish.mathmaster.network.ClaimStudyNotePayload;
import com.freshfish.mathmaster.network.SelfInsightPayload;
import com.freshfish.mathmaster.network.SelectAxiomSkillPayload;
import com.freshfish.mathmaster.network.UseAxiomSkillPayload;
import com.freshfish.mathmaster.quiz.QuizQuestionManager;
import com.freshfish.mathmaster.oracle.OracleManager;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import com.freshfish.mathmaster.init.ModFluids;

@Mod(MathMaster.MODID)
public class MathMaster {
    public static final String MODID = "mathmaster";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MathMaster(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                MathMasterConfig.SPEC,
                "mathmaster-server.toml"
        );
        IntegrationManager.initialize();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(InsightFailurePayload::register);
        modEventBus.addListener(DigitalPollutionMeterPayload::register);
        modEventBus.addListener(LearningAppNetworking::register);
        modEventBus.addListener(ClaimStudyNotePayload::register);
        modEventBus.addListener(SelfInsightPayload::register);
        modEventBus.addListener(SelectAxiomSkillPayload::register);
        modEventBus.addListener(UseAxiomSkillPayload::register);
        modEventBus.addListener(InvolutionStatePayload::register);
        modEventBus.addListener(GeodesicControlPayload::register);
        modEventBus.addListener(GeodesicStatePayload::register);
        modEventBus.addListener(ReturnControlPayload::register);
        modEventBus.addListener(ReturnAnchorsPayload::register);
        modEventBus.addListener(com.freshfish.mathmaster.network.PrimeComboStatePayload::register);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
        ModMenuTypes.MENUS.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModPotions.POTIONS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModStructures.STRUCTURE_TYPES.register(modEventBus);
        ModStructures.POOL_ELEMENT_TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(ModPotions::registerBrewingRecipes);
        NeoForge.EVENT_BUS.register(new CollatzTreeGrowth());
        NeoForge.EVENT_BUS.register(new com.freshfish.mathmaster.ritual.NAltarRitualHandler());
        NeoForge.EVENT_BUS.register(com.freshfish.mathmaster.ritual.NAltarOfferings.INSTANCE);
        NeoForge.EVENT_BUS.register(new com.freshfish.mathmaster.ritual.NAltarAccess());
        NeoForge.EVENT_BUS.register(new BookshelfInteractionHandler());
        NeoForge.EVENT_BUS.register(new FoodIntelligenceHandler());
        NeoForge.EVENT_BUS.register(new FlowEffectHandler());
        NeoForge.EVENT_BUS.register(new FivePlatformHandler());
        NeoForge.EVENT_BUS.register(new DigitallyCorruptedBlockEffectHandler());
        NeoForge.EVENT_BUS.register(new DigitalPollutionHandler());
        NeoForge.EVENT_BUS.register(new AxiomPassiveHandler());
        // Combo's final additive term runs before entropy at the same LOWEST event priority.
        NeoForge.EVENT_BUS.register(new com.freshfish.mathmaster.axiom.PrimeComboSkill());
        NeoForge.EVENT_BUS.register(new ShannonEntropySkill());
        NeoForge.EVENT_BUS.register(new AxiomCooldownHandler());
        NeoForge.EVENT_BUS.register(new AdditionCommutativitySkill());
        NeoForge.EVENT_BUS.register(new AdditiveInverseSkill());
        NeoForge.EVENT_BUS.register(new EuclidPrimeInfinitySkill());
        NeoForge.EVENT_BUS.register(new PlayfairAxiomSkill());
        NeoForge.EVENT_BUS.register(new InvolutionSkill());
        NeoForge.EVENT_BUS.register(new GeodesicSkill());
        NeoForge.EVENT_BUS.register(new ReturnSkill());
        NeoForge.EVENT_BUS.register(new EntityIntellectCombatHandler());
        NeoForge.EVENT_BUS.register(new InsightMirrorInteractionHandler());
        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("MathMaster common setup");
        event.enqueueWork(() -> {
            var fire = (net.minecraft.world.level.block.FireBlock) net.minecraft.world.level.block.Blocks.FIRE;
            fire.setFlammable(ModBlocks.COLLATZ_LOG.get(), 5, 5);
            for (var block : new net.minecraft.world.level.block.Block[]{ModBlocks.COLLATZ_PLANKS.get(),
                    ModBlocks.COLLATZ_SLAB.get(), ModBlocks.COLLATZ_STAIRS.get(),
                    ModBlocks.COLLATZ_FENCE.get(), ModBlocks.COLLATZ_FENCE_GATE.get()}) {
                fire.setFlammable(block, 5, 20);
            }
            fire.setFlammable(ModBlocks.PURPLE_COLLATZ_LOG.get(), 5, 5);
            for (var block : new net.minecraft.world.level.block.Block[]{ModBlocks.PURPLE_COLLATZ_PLANKS.get(),
                    ModBlocks.PURPLE_COLLATZ_SLAB.get(), ModBlocks.PURPLE_COLLATZ_STAIRS.get(),
                    ModBlocks.PURPLE_COLLATZ_FENCE.get(), ModBlocks.PURPLE_COLLATZ_FENCE_GATE.get()}) {
                fire.setFlammable(block, 5, 20);
            }
            fire.setFlammable(ModBlocks.BLUE_COLLATZ_LOG.get(), 5, 5);
            for (var block : new net.minecraft.world.level.block.Block[]{ModBlocks.BLUE_COLLATZ_PLANKS.get(),
                    ModBlocks.BLUE_COLLATZ_SLAB.get(), ModBlocks.BLUE_COLLATZ_STAIRS.get(),
                    ModBlocks.BLUE_COLLATZ_FENCE.get(), ModBlocks.BLUE_COLLATZ_FENCE_GATE.get()}) {
                fire.setFlammable(block, 5, 20);
            }
        });
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(QuizQuestionManager.INSTANCE);
        event.addListener(EntityIntellectManager.INSTANCE);
        event.addListener(InsightQuestionManager.INSTANCE);
        event.addListener(OracleManager.INSTANCE);
    }
}
