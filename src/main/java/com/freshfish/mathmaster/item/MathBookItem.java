package com.freshfish.mathmaster.item;

import com.freshfish.mathmaster.menu.MathMasterGuideMenu;
import com.freshfish.mathmaster.quiz.QuizBank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MathBookItem extends Item {
    private final int difficulty;
    private final int titleColor;
    private final String descriptionKey;
    private final String verseKey;
    private final String usageHintKey;

    public MathBookItem(
            Properties properties,
            int difficulty,
            int titleColor,
            String descriptionKey,
            String verseKey
    ) {
        this(properties, difficulty, titleColor, descriptionKey, verseKey, null);
    }

    public MathBookItem(
            Properties properties,
            int difficulty,
            int titleColor,
            String descriptionKey,
            String verseKey,
            String usageHintKey
    ) {
        super(properties);
        this.difficulty = difficulty;
        this.titleColor = titleColor;
        this.descriptionKey = descriptionKey;
        this.verseKey = verseKey;
        this.usageHintKey = usageHintKey;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack))
                .withStyle(style -> style.withColor(this.titleColor));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        QuizBank bank = QuizBank.byItem(stack.getItem());
        if (bank == null) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MathMasterGuideMenu.open(serverPlayer, bank);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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
                Component.translatable(this.verseKey)
                        .withStyle(ChatFormatting.GOLD)
        );
        if (this.usageHintKey != null) {
            tooltipComponents.add(
                    Component.translatable("tooltip.mathmaster.chiseled_bookshelf")
                            .withStyle(ChatFormatting.GREEN)
            );
        }
        tooltipComponents.add(
                Component.translatable("tooltip.mathmaster.open_guide")
                        .withStyle(ChatFormatting.AQUA)
        );
        if (this.usageHintKey != null) {
            tooltipComponents.add(
                    Component.translatable(this.usageHintKey)
                            .withStyle(style -> style.withColor(0xFF9800))
            );
        }
    }
}
