package com.freshfish.mathmaster.ritual;

import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class NAltarBloodDifficulty {
    public static final double PLAYER_RADIUS = 12.0;
    private static final List<Holder<MobEffect>> LOW = List.of(MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED);
    private static final List<Holder<MobEffect>> NORMAL = List.of(MobEffects.MOVEMENT_SPEED);
    private static final List<Holder<MobEffect>> HIGH = List.of(MobEffects.MOVEMENT_SLOWDOWN);
    private static final List<Holder<MobEffect>> HIGHEST = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS);

    private NAltarBloodDifficulty() {}

    public static double averageIq(ServerLevel level, Vec3 center) {
        long total = 0;
        int count = 0;
        for (var player : level.players()) {
            if (player.isAlive() && !player.isCreative() && !player.isSpectator()
                    && player.position().distanceToSqr(center) <= PLAYER_RADIUS * PLAYER_RADIUS) {
                total += IntelligenceManager.getEffectiveIq(player);
                count++;
            }
        }
        return count == 0 ? 0 : (double) total / count;
    }

    public static List<Holder<MobEffect>> effectsFor(double averageIq) {
        if (averageIq < 50) return LOW;
        if (averageIq <= 100) return NORMAL;
        if (averageIq < 160) return HIGH;
        return HIGHEST;
    }
}
