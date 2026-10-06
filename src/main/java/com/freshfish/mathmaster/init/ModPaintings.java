package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Preset vanilla paintings; variants are supplied by the data pack registry. */
public final class ModPaintings {
    public static final ResourceKey<PaintingVariant> DIGITAL_FIVE = ResourceKey.create(
            Registries.PAINTING_VARIANT, ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "digital_five"));

    private ModPaintings() {}

    public static ItemStack createDigitalFive() {
        var stack = new ItemStack(Items.PAINTING);
        var entityData = new CompoundTag();
        entityData.putString("id", "minecraft:painting");
        entityData.putString("variant", DIGITAL_FIVE.location().toString());
        stack.set(DataComponents.ENTITY_DATA, CustomData.of(entityData));
        return stack;
    }
}
