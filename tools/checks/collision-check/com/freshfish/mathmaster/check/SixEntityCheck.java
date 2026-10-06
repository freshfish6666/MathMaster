package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Focused server assertions for Six; compiled only by the opt-in check run. */
final class SixEntityCheck {
    private SixEntityCheck() {}

    static int run(net.minecraft.server.level.ServerLevel level) {
        var server = level.getServer();
        var six = ModEntities.SIX.get().create(level);
        if (six == null) {
            throw new AssertionError("Six failed to instantiate");
        }
        if (Math.abs(six.getMaxHealth() - 100.0F) > 1.0E-5F) {
            throw new AssertionError("Six max health was " + six.getMaxHealth());
        }
        if (!six.canDisableShield()) {
            throw new AssertionError("Six did not expose vanilla shield disabling");
        }

        int checks = 2;
        var target = EntityType.IRON_GOLEM.create(level);
        if (target == null) {
            throw new AssertionError("Damage target failed to instantiate");
        }
        Difficulty[] difficulties = {Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD};
        float[] damages = {12.0F, 25.0F, 39.0F};
        for (int index = 0; index < difficulties.length; index++) {
            server.setDifficulty(difficulties[index], true);
            target.setHealth(target.getMaxHealth());
            target.invulnerableTime = 0;
            if (!six.doHurtTarget(target)) {
                throw new AssertionError("Six attack failed on " + difficulties[index]);
            }
            float actual = target.getMaxHealth() - target.getHealth();
            if (Math.abs(actual - damages[index]) > 1.0E-4F) {
                throw new AssertionError(difficulties[index] + " damage was " + actual);
            }
            checks++;
        }

        server.setDifficulty(Difficulty.NORMAL, true);
        var player = FakePlayerFactory.getMinecraft(level);
        player.setHealth(player.getMaxHealth());
        player.setPos(0.0D, 200.0D, 0.0D);
        player.setYRot(0.0F);
        player.setYHeadRot(0.0F);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        player.startUsingItem(InteractionHand.OFF_HAND);
        for (int tick = 0; tick < 10; tick++) {
            player.tick();
        }
        six.setPos(0.0D, 200.0D, 1.0D);
        player.invulnerableTime = 0;
        six.doHurtTarget(player);
        if (!player.getCooldowns().isOnCooldown(Items.SHIELD)) {
            throw new AssertionError("Blocked Six attack did not disable shield");
        }
        checks++;
        player.stopUsingItem();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        target.discard();
        six.discard();
        return checks;
    }
}
