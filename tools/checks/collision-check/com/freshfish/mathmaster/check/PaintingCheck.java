package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.init.ModCreativeTabs;
import com.freshfish.mathmaster.init.ModPaintings;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Vanilla preset placement, random-pool exclusion, creative availability and saved variant. */
public final class PaintingCheck {
    private static int checks;
    public static int run(ServerLevel level) {
        checks = 0;
        var wall = new BlockPos(12, 250, 12);
        var area = new AABB(wall).inflate(8);
        var player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "PaintingCheck"), ClientInformation.createDefault());
        try {
            var variant = level.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT).getHolderOrThrow(ModPaintings.DIGITAL_FIVE);
            require(variant.value().width() == 4 && variant.value().height() == 4, "four by four blocks");
            require(!variant.is(PaintingVariantTags.PLACEABLE), "excluded from random tag");
            var tab = ModCreativeTabs.MATHMASTER_BLOCKS_TAB.get();
            tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), false, level.registryAccess()));
            var stack = ModPaintings.createDigitalFive();
            require(stack.is(Items.PAINTING), "vanilla painting item");
            require(tab.getDisplayItems().stream().anyMatch(s -> net.minecraft.world.item.ItemStack.isSameItemSameComponents(s, stack)), "available in creative without OP");
            require(tab.getSearchTabDisplayItems().stream().anyMatch(s -> net.minecraft.world.item.ItemStack.isSameItemSameComponents(s, stack)), "available in creative search");
            for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) level.setBlockAndUpdate(wall.offset(x, y, 0), Blocks.STONE.defaultBlockState());
            for (int i = 0; i < 64; i++) {
                var random = Painting.create(level, wall.north(), Direction.NORTH).orElseThrow();
                require(!random.getVariant().is(ModPaintings.DIGITAL_FIVE), "ordinary random painting cannot select custom variant");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atCenterOf(wall), Direction.NORTH, wall, false);
            stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            require(stack.isEmpty(), "successful vanilla placement consumes item");
            var paintings = level.getEntitiesOfClass(Painting.class, area);
            require(paintings.size() == 1, "one actual vanilla painting placed");
            var painting = paintings.getFirst();
            require(painting.getVariant().is(ModPaintings.DIGITAL_FIVE) && painting.survives(), "preset overrides random choice and fits wall");
            require(stack.isEmpty(), "one item consumed");
            var tag = painting.saveWithoutId(new CompoundTag());
            var restored = EntityType.PAINTING.create(level); restored.load(tag);
            require(restored.getVariant().is(ModPaintings.DIGITAL_FIVE) && restored.getDirection() == Direction.NORTH, "variant and facing survive entity save/load");
            painting.discard();
            for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) level.setBlockAndUpdate(wall.offset(x, y, 0), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            var smallStack = ModPaintings.createDigitalFive(); player.setItemInHand(InteractionHand.MAIN_HAND, smallStack);
            smallStack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            require(level.getEntitiesOfClass(Painting.class, area).isEmpty() && smallStack.getCount() == 1, "insufficient wall does not place or consume preset");
            return checks;
        } finally {
            for (var painting : level.getEntitiesOfClass(Painting.class, area)) painting.discard();
            for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) level.setBlockAndUpdate(wall.offset(x, y, 0), Blocks.AIR.defaultBlockState());
            player.discard();
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("Digital five painting: " + message);
        checks++;
    }
}
