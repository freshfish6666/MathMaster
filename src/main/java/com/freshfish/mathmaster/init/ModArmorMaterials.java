package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MathMaster.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GRADUATION_CAP =
            ARMOR_MATERIALS.register("graduation_cap", () -> new ArmorMaterial(
                    Map.of(ArmorItem.Type.HELMET, 2),
                    15,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(Items.BLACK_WOOL),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                            MathMaster.MODID,
                            "graduation_cap"
                    ))),
                    0.0F,
                    0.0F
            ));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LINGXU =
            ARMOR_MATERIALS.register("lingxu", () -> new ArmorMaterial(
                    // ArmorMaterial only accepts integers; the item attribute component retains the half points.
                    Map.of(
                            ArmorItem.Type.HELMET, 4,
                            ArmorItem.Type.CHESTPLATE, 12,
                            ArmorItem.Type.LEGGINGS, 9,
                            ArmorItem.Type.BOOTS, 4
                    ),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.LINGXU_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                            MathMaster.MODID,
                            "lingxu"
                    ))),
                    4.5F,
                    0.0F
            ));

    private ModArmorMaterials() {
    }
}
