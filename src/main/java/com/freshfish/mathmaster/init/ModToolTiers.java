package com.freshfish.mathmaster.init;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public enum ModToolTiers implements Tier {
    LINGXU;

    private static final int BASE_USES = 2031;

    @Override
    public int getUses() {
        return Math.round(BASE_USES * 1.5F);
    }

    @Override
    public float getSpeed() {
        return 9.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0F;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.LINGXU_INGOT.get());
    }
}
