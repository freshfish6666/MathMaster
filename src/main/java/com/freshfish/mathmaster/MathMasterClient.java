package com.freshfish.mathmaster;

import com.freshfish.mathmaster.client.BookshelfQuizScreen;
import com.freshfish.mathmaster.client.AxiomDeductionScreen;
import com.freshfish.mathmaster.client.AxiomCaseScreen;
import com.freshfish.mathmaster.client.AxiomSkillKeyHandler;
import com.freshfish.mathmaster.client.GraduationCapModel;
import com.freshfish.mathmaster.client.PrimeMarkRenderer;
import com.freshfish.mathmaster.client.entity.EightModel;
import com.freshfish.mathmaster.client.entity.FiveModel;
import com.freshfish.mathmaster.client.entity.FiveRenderer;
import com.freshfish.mathmaster.client.entity.SixModel;
import com.freshfish.mathmaster.client.entity.SixRenderer;
import com.freshfish.mathmaster.client.entity.SevenModel;
import com.freshfish.mathmaster.client.entity.SevenRenderer;
import com.freshfish.mathmaster.client.entity.EightRenderer;
import com.freshfish.mathmaster.client.entity.NineModel;
import com.freshfish.mathmaster.client.entity.NineRenderer;
import com.freshfish.mathmaster.init.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.freshfish.mathmaster.client.InsightMirrorHud;
import com.freshfish.mathmaster.client.DigitalPollutionMeterHud;
import com.freshfish.mathmaster.client.DigitalPollutionCorruptionOverlay;
import com.freshfish.mathmaster.client.MathMasterGuideScreen;
import com.freshfish.mathmaster.client.InsightQuizScreen;
import com.freshfish.mathmaster.client.InvolutionClientController;
import com.freshfish.mathmaster.client.GeodesicClientController;
import com.freshfish.mathmaster.client.ReturnAnchorScreen;
import com.freshfish.mathmaster.network.ReturnAnchorsPayload;
import com.freshfish.mathmaster.client.SelfInsightScreen;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMenuTypes;
import com.freshfish.mathmaster.network.InsightFailurePayload;
import com.freshfish.mathmaster.network.InvolutionStatePayload;
import com.freshfish.mathmaster.network.GeodesicStatePayload;
import com.freshfish.mathmaster.network.DigitalPollutionMeterPayload;
import com.freshfish.mathmaster.network.LearningAppNetworking;
import com.freshfish.mathmaster.network.SelfInsightPayload;
import com.freshfish.mathmaster.client.learning.LearningCatalogClientState;
import com.freshfish.mathmaster.quiz.QuizBank;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import com.freshfish.mathmaster.client.WrappedAnswerButton;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import com.freshfish.mathmaster.init.ModFluids;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

@Mod(value = MathMaster.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MathMaster.MODID, value = Dist.CLIENT)
public class MathMasterClient {
    public MathMasterClient() {
        InsightFailurePayload.setClientHandler(InsightMirrorHud::showTooDifficult);
        DigitalPollutionMeterPayload.setClientHandler(payload -> {
            DigitalPollutionMeterHud.accept(payload);
            DigitalPollutionCorruptionOverlay.accept(payload.pollution());
        });
        LearningAppNetworking.setCatalogClientHandler(LearningCatalogClientState::accept);
        SelfInsightPayload.setClientHandler(payload ->
                Minecraft.getInstance().setScreen(new SelfInsightScreen(payload)));
        InvolutionStatePayload.setClientHandler(InvolutionClientController::accept);
        GeodesicStatePayload.setClientHandler(GeodesicClientController::accept);
        ReturnAnchorsPayload.setClientHandler(ReturnAnchorScreen::accept);
        com.freshfish.mathmaster.network.PrimeComboStatePayload.setClientHandler(
                com.freshfish.mathmaster.client.PrimeComboOverlay::accept);
        NeoForge.EVENT_BUS.addListener(AxiomSkillKeyHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(InvolutionClientController::onRenderLiving);
        NeoForge.EVENT_BUS.addListener(PrimeMarkRenderer::onRenderNameTag);
    }

    @SubscribeEvent
    static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> WrappedAnswerButton.invalidateLayouts());
    }

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        AxiomSkillKeyHandler.registerKeys(event);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.BOOKSHELF_QUIZ.get(), BookshelfQuizScreen::new);
        event.register(ModMenuTypes.MATHMASTER_GUIDE.get(), MathMasterGuideScreen::new);
        event.register(ModMenuTypes.INSIGHT_QUIZ.get(), InsightQuizScreen::new);
        event.register(ModMenuTypes.AXIOM_DEDUCTION_TABLE.get(), AxiomDeductionScreen::new);
        event.register(ModMenuTypes.AXIOM_CASE.get(), AxiomCaseScreen::new);
    }

    @SubscribeEvent
    static void onRegisterEntityLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FiveModel.LAYER, FiveModel::createBodyLayer);
        event.registerLayerDefinition(EightModel.LAYER, EightModel::createBodyLayer);
        event.registerLayerDefinition(SixModel.LAYER, SixModel::createBodyLayer);
        event.registerLayerDefinition(com.freshfish.mathmaster.client.entity.GeometryHolderModel.LAYER,
                com.freshfish.mathmaster.client.entity.GeometryHolderModel::createBodyLayer);
        event.registerLayerDefinition(com.freshfish.mathmaster.client.entity.GeometryConstructModel.LAYER,
                com.freshfish.mathmaster.client.entity.GeometryConstructModel::createBodyLayer);
        event.registerLayerDefinition(SevenModel.LAYER, SevenModel::createBodyLayer);
        event.registerLayerDefinition(NineModel.LAYER, NineModel::createBodyLayer);
        event.registerLayerDefinition(GraduationCapModel.LAYER, GraduationCapModel::createBodyLayer);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(
                    ModFluids.DIGITALLY_POLLUTED_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    ModFluids.FLOWING_DIGITALLY_POLLUTED_WATER.get(), RenderType.translucent());
        });
    }

    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "block/digitally_polluted_water_still");
            }
            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "block/digitally_polluted_water_flow");
            }
        }, ModFluids.DIGITALLY_POLLUTED_WATER_TYPE.get());
        event.registerItem(new IClientItemExtensions() {
            private GraduationCapModel model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<?> original
            ) {
                if (this.model == null) {
                    this.model = new GraduationCapModel(
                            Minecraft.getInstance().getEntityModels().bakeLayer(GraduationCapModel.LAYER)
                    );
                }
                return this.model;
            }
        }, ModItems.GRADUATION_CAP.get());
    }

    @SubscribeEvent
    static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FIVE.get(), FiveRenderer::new);
        event.registerEntityRenderer(ModEntities.EIGHT.get(), EightRenderer::new);
        event.registerEntityRenderer(ModEntities.SIX.get(), SixRenderer::new);
        event.registerEntityRenderer(ModEntities.GEOMETRY_HOLDER.get(),
                com.freshfish.mathmaster.client.entity.GeometryHolderRenderer::new);
        event.registerEntityRenderer(ModEntities.GEOMETRY_CONSTRUCT.get(),
                com.freshfish.mathmaster.client.entity.GeometryConstructRenderer::new);
        event.registerEntityRenderer(ModEntities.SEVEN.get(), SevenRenderer::new);
        event.registerEntityRenderer(ModEntities.NINE.get(), NineRenderer::new);
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

        if (isLingxuArmor(event)) {
            event.getToolTip().add(
                    Component.translatable("tooltip.mathmaster.lingxu_armor.flow")
                            .withStyle(ChatFormatting.GRAY)
            );
            event.getToolTip().add(
                    Component.translatable("tooltip.mathmaster.lingxu_armor.set_bonus")
                            .withStyle(ChatFormatting.DARK_PURPLE)
            );
        }

        if (event.getItemStack().is(ModItems.FIVE_SPAWN_EGG.get())) {
            event.getToolTip().add(
                    Component.translatable("tooltip.mathmaster.five_spawn_egg.experimental")
                            .withStyle(ChatFormatting.YELLOW)
            );
        }

        if (event.getItemStack().is(ModItems.MATHEMATICAL_RING.get())) {
            event.getToolTip().add(Component.translatable("tooltip.mathmaster.mathematical_ring")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            event.getToolTip().add(Component.translatable("tooltip.mathmaster.mathematical_ring.creative_only")
                    .withStyle(ChatFormatting.GRAY));
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

    private static boolean isLingxuArmor(ItemTooltipEvent event) {
        return event.getItemStack().is(ModItems.LINGXU_HELMET.get())
                || event.getItemStack().is(ModItems.LINGXU_CHESTPLATE.get())
                || event.getItemStack().is(ModItems.LINGXU_LEGGINGS.get())
                || event.getItemStack().is(ModItems.LINGXU_BOOTS.get());
    }
}
