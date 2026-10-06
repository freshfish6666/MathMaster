package com.freshfish.mathmaster.effect;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class MentalRampageMobEffect extends MobEffect {
    public MentalRampageMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF05A24);
        addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                id("attack_damage"),
                0.20D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        addAttributeModifier(
                Attributes.ATTACK_SPEED,
                id("attack_speed"),
                0.10D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                id("movement_speed"),
                -0.10D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    private static ResourceLocation id(String attribute) {
        return ResourceLocation.fromNamespaceAndPath(
                MathMaster.MODID,
                "mental_rampage." + attribute
        );
    }
}
