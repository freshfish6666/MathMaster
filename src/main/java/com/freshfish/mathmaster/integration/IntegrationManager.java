package com.freshfish.mathmaster.integration;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.util.OptionalInt;

public final class IntegrationManager {
    private static final String MADNESS_CORE_MOD_ID = "madnesscore";
    private static final String MADNESS_CORE_BRIDGE =
            "com.freshfish.mathmaster.compat.madnesscore.MadnessCoreIntegration";
    private static MathMasterIntegration integration = new NoopIntegration();

    private IntegrationManager() {
    }

    public static void initialize() {
        if (!ModList.get().isLoaded(MADNESS_CORE_MOD_ID)) {
            MathMaster.LOGGER.info("MadnessCore not installed; optional integration is inactive");
            return;
        }

        try {
            integration = (MathMasterIntegration) Class.forName(MADNESS_CORE_BRIDGE)
                    .getDeclaredConstructor()
                    .newInstance();
            MathMaster.LOGGER.info("MadnessCore optional integration enabled");
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                 | IllegalAccessException | InvocationTargetException | LinkageError exception) {
            throw new IllegalStateException("MadnessCore is installed, but MathMaster could not load its compatibility bridge", exception);
        }
    }

    public static boolean tryStartQuiz(ServerPlayer player, ResourceLocation bookId, int difficulty) {
        return integration.tryStartQuiz(player, bookId, difficulty);
    }

    public static boolean isActive() {
        return !(integration instanceof NoopIntegration);
    }

    public static void syncAuthority(ServerPlayer player, OptionalInt rank) {
        integration.syncAuthority(player, rank);
    }
}
