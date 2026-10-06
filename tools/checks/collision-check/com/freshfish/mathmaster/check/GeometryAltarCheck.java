package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.block.GeometryAltarBlock;
import com.freshfish.mathmaster.block.entity.GeometryAltarBlockEntity;
import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModCreativeTabs;
import com.freshfish.mathmaster.init.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.UUID;

/** Real block/item interactions, escrow persistence and runtime spawning, outside the user's run worlds. */
public final class GeometryAltarCheck {
    private static int checks;
    public static int run(ServerLevel level) {
        checks = 0;
        BlockPos pos = new BlockPos(280, 240, 280);
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "GeometryAltarCheck"));
        player.setPos(282, 240, 282);
        var difficulty = level.getDifficulty();
        var bosses = new ArrayList<GeometryHolderEntity>();
        var drops = new ArrayList<ItemEntity>();
        boolean[] cancelJoin = {false};
        boolean[] removeOnJoin = {false};
        java.util.function.Consumer<EntityJoinLevelEvent> observer = event -> {
            if (event.getLevel() != level) return;
            if (event.getEntity() instanceof GeometryHolderEntity boss) {
                bosses.add(boss);
                if (removeOnJoin[0]) level.destroyBlock(pos, true);
                if (cancelJoin[0]) event.setCanceled(true);
            }
            if (event.getEntity() instanceof ItemEntity item) drops.add(item);
        };
        NeoForge.EVENT_BUS.addListener(observer);
        try {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = 0; y <= 5; y++)
                level.setBlockAndUpdate(pos.offset(x, y, z), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            var altarStack = new ItemStack(ModItems.GEOMETRY_ALTAR.get(), 2);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, altarStack, hit);
            require(((BlockItem) altarStack.getItem()).place(context).consumesAction() && altarStack.getCount() == 1, "actual block placement consumes one");
            require(level.getBlockState(pos).getBlock() == ModBlocks.GEOMETRY_ALTAR.get(), "single-block placement");
            require(level.getBlockState(pos).getPistonPushReaction() == PushReaction.BLOCK, "pistons cannot move escrow");
            require(level.getBlockState(pos).getCollisionShape(level, pos).max(Direction.Axis.Y) == .875, "model corner height matches shape");
            var frame = Blocks.END_PORTAL_FRAME.defaultBlockState();
            require(level.getBlockState(pos).getDestroySpeed(level,pos) == frame.getDestroySpeed(level,pos)
                    && ModBlocks.GEOMETRY_ALTAR.get().getExplosionResistance() == Blocks.END_PORTAL_FRAME.getExplosionResistance(),
                    "same unbreakable hardness and blast resistance as End portal frame");
            require(level.getBlockState(pos).is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)
                    && level.getBlockState(pos).is(net.minecraft.tags.BlockTags.DRAGON_IMMUNE), "same boss destruction immunity tags");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            for (boolean active : new boolean[]{false,true}) {
                if (active) altar(level,pos).begin(player,core(1));
                var original = altar(level,pos);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.NETHERITE_PICKAXE));
                player.gameMode.handleBlockBreakAction(pos,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                        Direction.UP,level.getMaxBuildHeight(),0);
                for (int i=0;i<100;i++) player.gameMode.tick();
                player.gameMode.handleBlockBreakAction(pos,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        Direction.UP,level.getMaxBuildHeight(),1);
                require(altar(level,pos)==original && original.isSummoning()==active
                        && level.getBlockState(pos).getDestroyProgress(player,level,pos)==0,
                        "survival mining cannot destroy idle/active altar or disturb escrow");
                level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,6F,net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
                require(altar(level,pos)==original && original.isSummoning()==active, "real explosion leaves idle/active altar intact");
                if (active) original.cancel(player);
            }
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.gameMode.handleBlockBreakAction(pos,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    Direction.UP,level.getMaxBuildHeight(),2);
            require(level.getBlockState(pos).isAir(), "creative still removes altar normally");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            level.setBlockAndUpdate(pos,ModBlocks.GEOMETRY_ALTAR.get().defaultBlockState());
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            var parameters = new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), false, level.registryAccess());
            ModCreativeTabs.MATHMASTER_TAB.get().buildContents(parameters);
            ModCreativeTabs.MATHMASTER_BLOCKS_TAB.get().buildContents(parameters);
            require(ModCreativeTabs.MATHMASTER_TAB.get().getDisplayItems().stream().anyMatch(s -> s.is(ModItems.GEOMETRY_CORE.get())), "core creative entry");
            require(ModCreativeTabs.MATHMASTER_BLOCKS_TAB.get().getSearchTabDisplayItems().stream().anyMatch(s -> s.is(ModItems.GEOMETRY_ALTAR.get())), "altar creative/search entry");
            var wrong = new ItemStack(Items.DIAMOND, 3);
            require(interact(level, pos, player, wrong, InteractionHand.MAIN_HAND) == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                    && wrong.getCount() == 3 && !altar(level, pos).isSummoning(), "unrelated offerings do not activate");

            for (var hand : InteractionHand.values()) {
                player.getInventory().clearContent();
                var stack = core(3);
                var expected = stack.copyWithCount(1);
                require(interact(level, pos, player, stack, hand).consumesAction(), "core handled in " + hand);
                require(stack.getCount() == 2 && altar(level, pos).remainingTicks() == 80, "one core escrowed immediately");
                require(level.getBlockState(pos).getValue(GeometryAltarBlock.SUMMONING), "active block state");
                // Cancelling with another item must neither spend that item nor start a second ritual.
                var cancelItem = new ItemStack(Items.STICK, 4);
                interact(level, pos, player, cancelItem, hand);
                require(cancelItem.getCount() == 4 && !altar(level, pos).isSummoning(), "any held item cancels");
                require(player.getInventory().items.stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, expected)), "original components refunded");
            }
            player.getInventory().clearContent();
            var stack = core(1);
            interact(level, pos, player, stack, InteractionHand.MAIN_HAND);
            require(stack.isEmpty(), "single stack completely removed");
            require(level.getBlockState(pos).useWithoutItem(level, player, hit).consumesAction() && !altar(level, pos).isSummoning(), "empty-hand cancellation");
            require(coreCount(player) == 1, "empty hand returns one");

            player.getInventory().clearContent();
            interact(level, pos, player, core(1), InteractionHand.MAIN_HAND);
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            int dropStart = drops.size();
            level.getBlockState(pos).useWithoutItem(level, player, hit);
            require(coreDrops(drops, dropStart) == 1 && !altar(level, pos).isSummoning(), "full inventory refunds as one dropped core");

            player.getInventory().clearContent();
            var named = core(2);
            interact(level, pos, player, named, InteractionHand.MAIN_HAND);
            for (int i = 0; i < 20; i++) tick(level, pos);
            var tag = altar(level, pos).saveWithFullMetadata(level.registryAccess());
            var restored = new GeometryAltarBlockEntity(pos, level.getBlockState(pos));
            restored.loadWithComponents(tag, level.registryAccess());
            level.removeBlockEntity(pos);
            level.setBlockEntity(restored);
            require(restored.isSummoning() && restored.remainingTicks() == 60, "escrow/countdown survive reload");
            for (int i = 0; i < 59; i++) tick(level, pos);
            require(bosses.isEmpty() && restored.remainingTicks() == 1, "not before four seconds total");
            tick(level, pos);
            require(bosses.size() == 1 && !bosses.getFirst().isRemoved() && !restored.isSummoning(), "exactly one boss at completion");
            var boss = bosses.getFirst();
            require(boss.position().distanceTo(Vec3.atBottomCenterOf(pos.above())) < 1.e-6 && level.noBlockCollision(boss, boss.getBoundingBox()), "spawn above socket without block intersection");
            require(boss.getHealth() == 200 && boss.getArmorValue() == 20 && !boss.isNoAi(), "existing combat attributes/AI unchanged");
            for (int i = 0; i < 100; i++) tick(level, pos);
            require(bosses.size() == 1, "finished ritual cannot spawn twice");
            var next = core(1);
            require(altar(level, pos).begin(player, next), "living nearby boss does not prohibit another ritual");
            for (int i = 0; i < 80; i++) tick(level, pos);
            require(bosses.size() == 2 && !bosses.getLast().isRemoved(), "repeat summon without proximity restriction");

            level.setBlockAndUpdate(pos.above(2), Blocks.STONE.defaultBlockState());
            var blocked = core(2);
            interact(level, pos, player, blocked, InteractionHand.MAIN_HAND);
            require(blocked.getCount() == 2 && !altar(level, pos).isSummoning(), "obstructed start consumes nothing");
            level.setBlockAndUpdate(pos.above(2), Blocks.AIR.defaultBlockState());
            interact(level, pos, player, core(1), InteractionHand.MAIN_HAND);
            level.setBlockAndUpdate(pos.above(2), Blocks.STONE.defaultBlockState());
            dropStart = drops.size();
            for (int i = 0; i < 80; i++) tick(level, pos);
            require(bosses.size() == 2 && coreDrops(drops, dropStart) == 1 && !altar(level, pos).isSummoning(), "late obstruction refunds offline owner at altar");
            level.setBlockAndUpdate(pos.above(2), Blocks.AIR.defaultBlockState());

            level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
            var peaceful = core(2);
            interact(level, pos, player, peaceful, InteractionHand.MAIN_HAND);
            require(peaceful.getCount() == 2 && !altar(level, pos).isSummoning(), "peaceful cannot waste core");
            level.getServer().setDifficulty(difficulty, true);

            interact(level, pos, player, core(1), InteractionHand.MAIN_HAND);
            cancelJoin[0] = true;
            dropStart = drops.size();
            for (int i = 0; i < 80; i++) tick(level, pos);
            require(bosses.size() == 3 && bosses.getLast().isRemoved() && coreDrops(drops, dropStart) == 1, "cancelled entity join refunds exactly once");
            cancelJoin[0] = false;

            interact(level, pos, player, core(1), InteractionHand.MAIN_HAND);
            dropStart = drops.size();
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            player.gameMode.handleBlockBreakAction(pos,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    Direction.UP,level.getMaxBuildHeight(),3);
            require(coreDrops(drops, dropStart) == 1 && drops.subList(dropStart, drops.size()).stream()
                    .filter(e -> e.getItem().is(ModItems.GEOMETRY_ALTAR.get())).mapToInt(e -> e.getItem().getCount()).sum() == 0,
                    "creative removal of active altar returns escrow once without dropping altar");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            level.setBlockAndUpdate(pos, ModBlocks.GEOMETRY_ALTAR.get().defaultBlockState());
            interact(level, pos, player, core(1), InteractionHand.MAIN_HAND);
            cancelJoin[0] = removeOnJoin[0] = true;
            dropStart = drops.size();
            for (int i = 0; i < 80; i++) tick(level, pos);
            require(coreDrops(drops, dropStart) == 1 && level.getBlockState(pos).isAir(), "spawn hook removing altar cannot duplicate escrow refund");
            return checks;
        } finally {
            NeoForge.EVENT_BUS.unregister(observer);
            cancelJoin[0] = removeOnJoin[0] = false;
            for (var boss : bosses) boss.discard();
            level.getServer().setDifficulty(difficulty, true);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.above(2), Blocks.AIR.defaultBlockState());
            for (var item : drops) item.discard();
            player.discard();
        }
    }
    private static ItemStack core(int count) {
        var stack = new ItemStack(ModItems.GEOMETRY_CORE.get(), count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Escrow core"));
        return stack;
    }
    private static int coreCount(FakePlayer player) {
        return player.getInventory().items.stream().filter(s -> s.is(ModItems.GEOMETRY_CORE.get())).mapToInt(ItemStack::getCount).sum();
    }
    private static int coreDrops(ArrayList<ItemEntity> drops, int start) {
        return drops.subList(start, drops.size()).stream().filter(e -> e.getItem().is(ModItems.GEOMETRY_CORE.get()))
                .mapToInt(e -> e.getItem().getCount()).sum();
    }
    private static GeometryAltarBlockEntity altar(ServerLevel level, BlockPos pos) {
        return (GeometryAltarBlockEntity) level.getBlockEntity(pos);
    }
    private static void tick(ServerLevel level, BlockPos pos) {
        if (altar(level, pos) != null) GeometryAltarBlockEntity.serverTick(level, pos, level.getBlockState(pos), altar(level, pos));
    }
    private static ItemInteractionResult interact(ServerLevel level, BlockPos pos, FakePlayer player, ItemStack stack, InteractionHand hand) {
        player.setItemInHand(hand, stack);
        return level.getBlockState(pos).useItemOn(stack, level, player, hand,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("Geometry altar: " + message);
        checks++;
    }
}
