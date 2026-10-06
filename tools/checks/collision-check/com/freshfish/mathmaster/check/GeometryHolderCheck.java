package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Actual egg interaction and persistence on a dedicated server (no client classes). */
public final class GeometryHolderCheck {
    private static int checks;
    public static int run(ServerLevel level) {
        checks = 0;
        BlockPos floor = new BlockPos(210, 240, 210);
        var previous = level.getBlockState(floor);
        var player = FakePlayerFactory.getMinecraft(level);
        ItemStack oldHand = player.getMainHandItem();
        var spawned = new java.util.ArrayList<GeometryHolderEntity>();
        java.util.function.Consumer<EntityJoinLevelEvent> observer = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof GeometryHolderEntity entity) spawned.add(entity);
        };
        NeoForge.EVENT_BUS.addListener(observer);
        GeometryHolderEntity holder = null;
        try {
            level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            ItemStack eggStack = new ItemStack(ModItems.GEOMETRY_HOLDER_SPAWN_EGG.get(), 2);
            var egg = (SpawnEggItem) eggStack.getItem();
            require(egg.getType(eggStack) == ModEntities.GEOMETRY_HOLDER.get(), "egg maps to correct entity");
            player.setItemInHand(InteractionHand.MAIN_HAND, eggStack);
            var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0), Direction.UP, floor, false);
            require(egg.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, eggStack, hit)).consumesAction(), "actual egg use accepted");
            // Join events include entities in chunks that are not player-tracked yet.
            require(spawned.size() == 1, "egg creates exactly one entity");
            holder = spawned.getFirst();
            require(eggStack.getCount() == 1, "egg consumes one");
            require(!holder.isNoAi() && holder.isNoGravity(), "movement AI and no gravity");
            require(holder.isPersistenceRequired() && !holder.isPushable(), "persistent stationary preview");
            require(holder.getBbWidth() == GeometryHolderEntity.WIDTH && holder.getBbHeight() == GeometryHolderEntity.HEIGHT,
                    "registered preview dimensions");
            require(holder.getMaxHealth() == 200 && holder.getHealth() == 200 && holder.getArmorValue() == 20,
                    "spawn egg creates two-hundred-health twenty-armor boss");
            var position = holder.position();
            for (int tick = 0; tick < 60; tick++) holder.tick();
            require(holder.position().distanceToSqr(position) <= 17*17 && holder.getTarget() == null, "wandering remains near home without player target");
            holder.checkDespawn();
            require(!holder.isRemoved(), "preview not naturally discarded");
            CompoundTag saved = new CompoundTag();
            require(holder.save(saved), "entity serialization");
            var restored = ModEntities.GEOMETRY_HOLDER.get().create(level);
            require(restored != null, "registered type can recreate entity");
            restored.load(saved);
            require(!restored.isNoAi() && restored.isNoGravity() && restored.isPersistenceRequired(), "movement flags survive save reload");
            require(restored.position().distanceToSqr(holder.position()) < 1e-8 && restored.getHealth() == holder.getHealth(),
                    "position and health survive save reload");
            restored.discard();
        } finally {
            NeoForge.EVENT_BUS.unregister(observer);
            spawned.forEach(GeometryHolderEntity::discard);
            if (holder != null) holder.discard();
            player.setItemInHand(InteractionHand.MAIN_HAND, oldHand);
            level.setBlockAndUpdate(floor, previous);
        }
        return checks;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("Geometry Holder: " + message);
        checks++;
    }
}
