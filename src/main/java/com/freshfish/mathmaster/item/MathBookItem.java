package com.freshfish.mathmaster.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MathBookItem extends Item {
    private final int difficulty;
    private final int titleColor;
    private final String descriptionKey;
    private final String usageHintKey;

    public MathBookItem(Properties properties, int difficulty, int titleColor, String descriptionKey) {
        this(properties, difficulty, titleColor, descriptionKey, null);
    }

    public MathBookItem(
            Properties properties,
            int difficulty,
            int titleColor,
            String descriptionKey,
            String usageHintKey
    ) {
        super(properties);
        this.difficulty = difficulty;
        this.titleColor = titleColor;
        this.descriptionKey = descriptionKey;
        this.usageHintKey = usageHintKey;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack))
                .withStyle(style -> style.withColor(this.titleColor));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        tooltipComponents.add(
                Component.translatable("tooltip.mathmaster.difficulty", this.difficulty)
                        .withStyle(ChatFormatting.YELLOW)
        );
        tooltipComponents.add(
                Component.translatable(this.descriptionKey)
                        .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
                Component.translatable("tooltip.mathmaster.chiseled_bookshelf")
                        .withStyle(ChatFormatting.GREEN)
        );
        if (this.usageHintKey != null) {
            tooltipComponents.add(
                    Component.translatable(this.usageHintKey)
                            .withStyle(ChatFormatting.GOLD)
            );
        }
    }
}
