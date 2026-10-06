package com.freshfish.mathmaster.integration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.OptionalInt;

final class NoopIntegration implements MathMasterIntegration {
    @Override
    public boolean tryStartQuiz(ServerPlayer player, ResourceLocation bookId, int difficulty) {
        return true;
    }

    @Override
    public void syncAuthority(ServerPlayer player, OptionalInt rank) {
    }
}
