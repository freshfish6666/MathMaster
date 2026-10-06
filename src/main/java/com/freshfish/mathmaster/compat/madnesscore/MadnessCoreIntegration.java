package com.freshfish.mathmaster.compat.madnesscore;

import com.freshfish.madnesscore.api.CoreApi;
import com.freshfish.madnesscore.api.MutationContext;
import com.freshfish.madnesscore.api.authority.AuthorityApi;
import com.freshfish.madnesscore.api.metric.BuiltinMetrics;
import com.freshfish.madnesscore.api.metric.MetricApi;
import com.freshfish.mathmaster.integration.MathMasterIntegration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.OptionalInt;

public final class MadnessCoreIntegration implements MathMasterIntegration {
    private static final int SUPPORTED_API_MAJOR = 1;
    private static final double MINIMUM_QUIZ_ENERGY = 10.0;
    private static final ResourceLocation METRIC_SOURCE = ResourceLocation.parse("mathmaster:quiz_system");
    private static final ResourceLocation QUIZ_START = ResourceLocation.parse("mathmaster:quiz_start");
    private static final ResourceLocation AUTHORITY_SOURCE = ResourceLocation.parse("mathmaster:progression");
    private static final ResourceLocation PROGRESSION_SYNC = ResourceLocation.parse("mathmaster:progression_sync");

    public MadnessCoreIntegration() {
        if (CoreApi.API_MAJOR != SUPPORTED_API_MAJOR) {
            throw new IllegalStateException("Unsupported MadnessCore API major: " + CoreApi.API_MAJOR);
        }
    }

    @Override
    public boolean tryStartQuiz(ServerPlayer player, ResourceLocation bookId, int difficulty) {
        double cost = difficulty / 2.0;
        double required = Math.max(MINIMUM_QUIZ_ENERGY, cost);
        double energy = MetricApi.get(player, BuiltinMetrics.MENTAL_ENERGY);
        if (energy < required) {
            player.sendSystemMessage(Component.translatable(
                    "message.mathmaster.madnesscore.insufficient_mental_energy",
                    formatAmount(required),
                    formatAmount(energy)
            ));
            return false;
        }

        boolean consumed = MetricApi.decrease(
                player,
                BuiltinMetrics.MENTAL_ENERGY,
                cost,
                context(player, METRIC_SOURCE, QUIZ_START, true)
        );
        if (!consumed) {
            player.sendSystemMessage(Component.translatable("message.mathmaster.madnesscore.mental_energy_change_blocked"));
        }
        return consumed;
    }

    @Override
    public void syncAuthority(ServerPlayer player, OptionalInt rank) {
        OptionalInt current = AuthorityApi.getContribution(player, AUTHORITY_SOURCE);
        if (current.equals(rank)) {
            return;
        }

        MutationContext context = context(player, AUTHORITY_SOURCE, PROGRESSION_SYNC, false);
        if (rank.isPresent()) {
            AuthorityApi.reportRank(player, AUTHORITY_SOURCE, rank.getAsInt(), context);
        } else if (current.isPresent()) {
            AuthorityApi.clearRank(player, AUTHORITY_SOURCE, context);
        }
    }

    private static MutationContext context(
            ServerPlayer player,
            ResourceLocation source,
            ResourceLocation reason,
            boolean allowNotifications
    ) {
        return new MutationContext(source, reason, Optional.of(player.getUUID()), allowNotifications);
    }

    private static String formatAmount(double amount) {
        return amount == Math.rint(amount) ? Integer.toString((int) amount) : Double.toString(amount);
    }
}
