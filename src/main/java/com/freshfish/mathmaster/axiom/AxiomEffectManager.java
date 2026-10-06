package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class AxiomEffectManager {
    private AxiomEffectManager() {
    }

    public static int getEquippedLevel(LivingEntity entity, AxiomDefinition definition) {
        return EquippedAxiomCase.find(entity)
                .map(caseStack -> getEffectiveLevel(caseStack, definition))
                .orElse(0);
    }

    public static int getEffectiveLevel(ItemStack caseStack, AxiomDefinition definition) {
        return getEffectiveLevel(AxiomCaseItem.getEquippedAxioms(caseStack), definition);
    }

    public static int getEffectiveLevel(
            List<AxiomCaseItem.EquippedAxiom> equippedAxioms, AxiomDefinition definition
    ) {
        int baseLevel = getRawLevel(equippedAxioms, definition);
        if (baseLevel <= 0) {
            return 0;
        }
        int inductionLevel = getRawLevel(equippedAxioms, AxiomDefinition.MATHEMATICAL_INDUCTION);
        return applyMathematicalInduction(definition, baseLevel, inductionLevel);
    }

    static int applyMathematicalInduction(
            AxiomDefinition definition,
            int baseLevel,
            int inductionLevel
    ) {
        if (baseLevel <= 0
                || definition == AxiomDefinition.MATHEMATICAL_INDUCTION
                || definition.category() != AxiomDefinition.AxiomCategory.LOGIC
                || !AxiomDefinition.MATHEMATICAL_INDUCTION.acceptsNoteLevel(inductionLevel)) {
            return baseLevel;
        }
        return Math.min(definition.maximumNoteLevel(), Math.max(baseLevel, inductionLevel + 1));
    }

    private static int getRawLevel(
            List<AxiomCaseItem.EquippedAxiom> equippedAxioms,
            AxiomDefinition definition
    ) {
        return equippedAxioms.stream()
                .filter(equipped -> equipped.definition() == definition)
                .mapToInt(AxiomCaseItem.EquippedAxiom::level)
                .filter(definition::acceptsNoteLevel)
                .max()
                .orElse(0);
    }

    public static double getPeanoSpawnMultiplier(ServerLevel level, BlockPos position) {
        double multiplier = 0.0D;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.distanceToSqr(
                    position.getX() + 0.5D,
                    position.getY() + 0.5D,
                    position.getZ() + 0.5D
            ) > 128.0D * 128.0D) {
                continue;
            }
            int axiomLevel = getEquippedLevel(player, AxiomDefinition.PEANO_AXIOMS);
            if (axiomLevel <= 0) {
                continue;
            }
            int pollution = player.getData(ModAttachments.DIGITAL_POLLUTION).getValue();
            multiplier = Math.max(multiplier, (1.0D + pollution / 100.0D) * axiomLevel);
        }
        return multiplier;
    }
}
