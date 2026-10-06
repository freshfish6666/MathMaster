package com.freshfish.mathmaster.integration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.OptionalInt;

/** Internal boundary for optional integrations; it must not expose provider types. */
public interface MathMasterIntegration {
    boolean tryStartQuiz(ServerPlayer player, ResourceLocation bookId, int difficulty);

    void syncAuthority(ServerPlayer player, OptionalInt rank);
}
