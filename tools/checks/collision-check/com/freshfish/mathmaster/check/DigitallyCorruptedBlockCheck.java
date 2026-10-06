package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.event.DigitallyCorruptedBlockEffectHandler;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

final class DigitallyCorruptedBlockCheck {
    private DigitallyCorruptedBlockCheck() {
    }

    static int run(net.minecraft.server.level.ServerLevel level) {
        BlockPos corruptedPos = new BlockPos(20, 200, 20);
        BlockPos safePos = new BlockPos(22, 200, 20);
        level.setBlockAndUpdate(corruptedPos, ModBlocks.DIGITALLY_CORRUPTED_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(safePos, Blocks.STONE.defaultBlockState());

        var cow = EntityType.COW.create(level);
        if (cow == null) {
            throw new AssertionError("Cow failed to instantiate");
        }
        DigitallyCorruptedBlockEffectHandler handler = new DigitallyCorruptedBlockEffectHandler();

        cow.setPos(20.5D, 201.0D, 20.5D);
        handler.onEntityTick(new EntityTickEvent.Post(cow));
        MobEffectInstance blockEffect = cow.getEffect(ModMobEffects.DIGITAL_POLLUTION);
        require(blockEffect != null && blockEffect.isAmbient() && !blockEffect.isVisible(),
                "standing creature did not receive the block pollution effect");

        cow.setPos(22.5D, 201.0D, 20.5D);
        handler.onEntityTick(new EntityTickEvent.Post(cow));
        require(!cow.hasEffect(ModMobEffects.DIGITAL_POLLUTION),
                "block pollution effect remained after leaving");

        cow.addEffect(new MobEffectInstance(ModMobEffects.DIGITAL_POLLUTION, 1200));
        handler.onEntityTick(new EntityTickEvent.Post(cow));
        require(cow.hasEffect(ModMobEffects.DIGITAL_POLLUTION),
                "leaving the block removed a longer external pollution effect");
        cow.discard();

        var armorStand = EntityType.ARMOR_STAND.create(level);
        if (armorStand == null) {
            throw new AssertionError("Armor stand failed to instantiate");
        }
        armorStand.setPos(20.5D, 201.0D, 20.5D);
        handler.onEntityTick(new EntityTickEvent.Post(armorStand));
        require(!armorStand.hasEffect(ModMobEffects.DIGITAL_POLLUTION),
                "entity without intellect received the block pollution effect");
        armorStand.discard();
        return 4;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
