package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Server-side activation and cleanup checks for the visual-only Involution skill. */
public final class InvolutionSkillCheck {
    private InvolutionSkillCheck() {
    }

    public static int run(net.minecraft.server.level.ServerLevel level) throws Exception {
        var player = FakePlayerFactory.getMinecraft(level);
        player.getData(ModAttachments.DIGITAL_POLLUTION).reset();
        player.getData(ModAttachments.AXIOM_SKILL_DATA).setInvolutionCooldownTicks(0);
        IntelligenceManager.setIq(player, 100);
        player.setPos(120.0D, 200.0D, 120.0D);

        var cow = EntityType.COW.create(level);
        if (cow == null) throw new AssertionError("Involution target failed to instantiate");
        cow.setPos(120.0D, 200.0D, 123.0D);
        level.addFreshEntity(cow);

        if (!AxiomDefinition.INVOLUTION.isActive()
                || AxiomDefinition.INVOLUTION.requiredNoteLevel() != 5
                || AxiomDefinition.INVOLUTION.maximumNoteLevel() != 5) {
            throw new AssertionError("Involution axiom registration was incorrect");
        }
        InvolutionSkill.useSelectedTarget(player, cow);
        if (!InvolutionSkill.isActive(player)) {
            throw new AssertionError("Involution did not activate on a lower-intellect cow");
        }
        if (cow.isNoAi()) {
            throw new AssertionError("Visual-only Involution unexpectedly suspended creature AI");
        }
        if (player.getData(ModAttachments.DIGITAL_POLLUTION).getValue() != 20) {
            throw new AssertionError("Involution did not add 20 pollution");
        }
        if (player.getData(ModAttachments.AXIOM_SKILL_DATA).getInvolutionCooldownTicks()
                != InvolutionSkill.COOLDOWN_TICKS) {
            throw new AssertionError("Involution did not start its 100 second cooldown");
        }

        cow.discard();
        InvolutionSkill.finish(player, null);
        if (InvolutionSkill.isActive(player)) {
            throw new AssertionError("Involution session survived target removal");
        }
        player.getData(ModAttachments.DIGITAL_POLLUTION).reset();
        player.getData(ModAttachments.AXIOM_SKILL_DATA).setInvolutionCooldownTicks(0);
        return 6 + checkPollutionActivation(level);
    }

    private static int checkPollutionActivation(net.minecraft.server.level.ServerLevel level) throws Exception {
        var cow = EntityType.COW.create(level);
        if (cow == null) throw new AssertionError("Involution pollution target missing");
        cow.setNoAi(true);
        cow.setPos(4, 240, 9);
        level.addFreshEntity(cow);
        try {
            for (int startingPollution : new int[]{79, 80, 99}) {
                try (var probe = new SkillCheckPlayer(level, "InvolutionCheck")) {
                    var owner = probe.player;
                    IntelligenceManager.setIq(owner, 100);
                    owner.getData(ModAttachments.DIGITAL_POLLUTION).add(startingPollution);
                    InvolutionSkill.useSelectedTarget(owner, cow);
                    boolean survives = startingPollution == 79;
                    if (owner.isAlive() != survives || InvolutionSkill.isActive(owner) != survives) {
                        throw new AssertionError("Involution pollution/death session mismatch at " + startingPollution);
                    }
                    if (owner.getData(ModAttachments.AXIOM_SKILL_DATA).getInvolutionCooldownTicks()
                            != InvolutionSkill.COOLDOWN_TICKS) {
                        throw new AssertionError("Involution lethal activation changed cooldown");
                    }
                    if (owner.getData(ModAttachments.DIGITAL_POLLUTION).getValue() != (survives ? 99 : 0)) {
                        throw new AssertionError("Involution pollution charge/reset changed");
                    }
                    if (probe.involutionPackets.size() != 1
                            || probe.involutionPackets.getFirst().active() != survives) {
                        throw new AssertionError("Involution broadcast activation after lethal pollution: "
                                + probe.involutionPackets);
                    }
                    InvolutionSkill.finish(owner, null);
                }
            }
            return 12;
        } finally {
            cow.discard();
        }
    }
}
