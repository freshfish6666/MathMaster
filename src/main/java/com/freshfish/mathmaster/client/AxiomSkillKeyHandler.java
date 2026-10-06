package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.network.UseAxiomSkillPayload;
import com.freshfish.mathmaster.network.GeodesicControlPayload;
import com.freshfish.mathmaster.network.ReturnControlPayload;
import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.EquippedAxiomCase;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class AxiomSkillKeyHandler {
    public static final String CATEGORY = "key.categories.mathmaster";
    public static final KeyMapping USE_SKILL = new KeyMapping(
            "key.mathmaster.use_axiom_skill", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY
    );
    public static final KeyMapping OPEN_WHEEL = new KeyMapping(
            "key.mathmaster.open_axiom_wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY
    );
    private static boolean skillPressed;
    private static boolean geodesicPressed;
    private static boolean returnPressed;
    private static int heldTicks;

    private AxiomSkillKeyHandler() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(USE_SKILL);
        event.register(OPEN_WHEEL);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        InvolutionClientController.tick(minecraft);
        GeodesicClientController.tick(minecraft);
        if (minecraft.player == null || minecraft.screen != null || !minecraft.isWindowActive()) {
            if (skillPressed && minecraft.getConnection() != null) {
                PacketDistributor.sendToServer(new GeodesicControlPayload(false));
                PacketDistributor.sendToServer(new ReturnControlPayload(ReturnControlPayload.CANCEL, -1));
            }
            skillPressed = false;
            geodesicPressed = false;
            returnPressed = false;
            heldTicks = 0;
            while (USE_SKILL.consumeClick()) { /* Discard clicks made inside a screen. */ }
            return;
        }
        while (USE_SKILL.consumeClick()) {
            if (!skillPressed) {
                PacketDistributor.sendToServer(new UseAxiomSkillPayload(true));
                skillPressed = true;
                var pressedAxiom = EquippedAxiomCase.find(minecraft.player)
                        .map(caseStack -> {
                            var active = AxiomCaseItem.getAxioms(caseStack).stream()
                                    .filter(AxiomDefinition::isActive).toList();
                            return AxiomCaseItem.getSelectedAxiom(caseStack).filter(active::contains)
                                    .orElse(active.isEmpty() ? null : active.getFirst());
                        })
                        .orElse(null);
                geodesicPressed = pressedAxiom == AxiomDefinition.GEODESIC;
                returnPressed = pressedAxiom == AxiomDefinition.RETURN;
                heldTicks = 0;
            }
        }
        if (skillPressed && !USE_SKILL.isDown()) {
            GeodesicClientController.stop();
            PacketDistributor.sendToServer(new UseAxiomSkillPayload(false));
            skillPressed = false;
            geodesicPressed = false;
            returnPressed = false;
        } else if (skillPressed && (geodesicPressed || returnPressed) && ++heldTicks % 5 == 0) {
            if (geodesicPressed) PacketDistributor.sendToServer(new GeodesicControlPayload(true));
            if (returnPressed) PacketDistributor.sendToServer(new ReturnControlPayload(ReturnControlPayload.HEARTBEAT, -1));
        }
        if (OPEN_WHEEL.consumeClick()) {
            minecraft.setScreen(new AxiomWheelScreen());
        }
    }
}
