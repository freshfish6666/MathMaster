package com.freshfish.mathmaster.block.entity;

import com.freshfish.mathmaster.block.GeometryAltarBlock;
import com.freshfish.mathmaster.entity.GeometryHolderEntity;
import com.freshfish.mathmaster.init.ModBlockEntities;
import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.UUID;

/** The consumed core is held in escrow until completion, cancellation, or block removal. */
public final class GeometryAltarBlockEntity extends BlockEntity {
    public static final int RITUAL_TICKS = 80;
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(.18F, .85F, 1F), 1F);
    private ItemStack offering = ItemStack.EMPTY;
    private int remainingTicks;
    private UUID summoner;

    public GeometryAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEOMETRY_ALTAR.get(), pos, state);
    }
    public boolean isSummoning() { return !offering.isEmpty(); }
    public int remainingTicks() { return remainingTicks; }

    public boolean begin(Player player, ItemStack stack) {
        if (!(level instanceof ServerLevel server) || isSummoning() || !stack.is(ModItems.GEOMETRY_CORE.get())) return false;
        if (!canSummon(server)) {
            player.displayClientMessage(Component.translatable(server.getDifficulty() == Difficulty.PEACEFUL
                    ? "message.mathmaster.geometry_altar.peaceful" : "message.mathmaster.geometry_altar.blocked"), true);
            return false;
        }
        // Consume in creative too: every escrowed item has actually been removed, so refunds cannot duplicate it.
        offering = stack.split(1);
        summoner = player.getUUID();
        remainingTicks = RITUAL_TICKS;
        updateActive(true);
        setChanged();
        return true;
    }

    /** Any player may remove the deposited core; return it to the player cancelling. */
    public void cancel(Player player) {
        if (!(level instanceof ServerLevel) || !isSummoning()) return;
        player.getInventory().placeItemBackInInventory(takeOffering());
    }

    public void dropOffering() {
        if (level instanceof ServerLevel && isSummoning()) Block.popResource(level, worldPosition, takeOffering());
    }

    private ItemStack takeOffering() {
        ItemStack result = offering;
        offering = ItemStack.EMPTY;
        remainingTicks = 0;
        summoner = null;
        updateActive(false);
        setChanged();
        return result;
    }

    private void updateActive(boolean active) {
        if (level != null && level.getBlockState(worldPosition).getBlock() instanceof GeometryAltarBlock)
            level.setBlockAndUpdate(worldPosition, level.getBlockState(worldPosition).setValue(GeometryAltarBlock.SUMMONING, active));
    }

    private boolean canSummon(ServerLevel server) {
        AABB bounds = AABB.ofSize(worldPosition.getCenter().add(0, .5 + GeometryHolderEntity.HEIGHT / 2D, 0),
                GeometryHolderEntity.WIDTH, GeometryHolderEntity.HEIGHT, GeometryHolderEntity.WIDTH);
        // Do not force neighboring chunks, spawn in walls, cross the border, or restrict other living bosses.
        return server.getDifficulty() != Difficulty.PEACEFUL && bounds.maxY <= server.getMaxBuildHeight()
                && server.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                        BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))
                && server.getWorldBorder().isWithinBounds(bounds) && server.noBlockCollision(null, bounds.deflate(1.e-6));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeometryAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel server)) return;
        // Reconcile loaded/command-placed block states from the authoritative saved escrow.
        if (state.getValue(GeometryAltarBlock.SUMMONING) != altar.isSummoning()) altar.updateActive(altar.isSummoning());
        if (!altar.isSummoning()) return;
        altar.remainingTicks--;
        altar.setChanged();
        if (altar.remainingTicks <= 0) altar.finish(server);
        else if (altar.remainingTicks % 4 == 0) altar.particles(server);
    }

    private void particles(ServerLevel server) {
        double progress = 1 - remainingTicks / (double) RITUAL_TICKS;
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double radius = .42 / Math.max(Math.abs(Math.cos(angle)), Math.abs(Math.sin(angle)));
            server.sendParticles(CYAN, worldPosition.getX() + .5 + Math.cos(angle) * radius,
                    worldPosition.getY() + .87 + progress * .6, worldPosition.getZ() + .5 + Math.sin(angle) * radius,
                    1 + (int) (progress * 2), .025, .08 + progress * .12, .025, 0);
        }
    }

    private void finish(ServerLevel server) {
        Player owner = summoner == null ? null : server.getPlayerByUUID(summoner);
        boolean allowed = canSummon(server);
        // Clear escrow before spawn hooks: even a hook removing the block cannot also refund the core.
        ItemStack spent = takeOffering();
        GeometryHolderEntity boss = allowed ? ModEntities.GEOMETRY_HOLDER.get().create(server, null,
                worldPosition.above(), MobSpawnType.TRIGGERED, false, false) : null;
        if (boss != null && owner != null) {
            boss.setYRot((float) Math.toDegrees(Math.atan2(owner.getZ() - boss.getZ(), owner.getX() - boss.getX())) - 90F);
            boss.yBodyRot = boss.yHeadRot = boss.getYRot();
        }
        if (boss != null && server.addFreshEntity(boss)) {
            server.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + .5, worldPosition.getY() + 1.5,
                    worldPosition.getZ() + .5, 36, .6, .7, .6, .06);
        } else {
            if (boss != null) boss.discard();
            if (owner != null) {
                owner.getInventory().placeItemBackInInventory(spent);
                owner.displayClientMessage(Component.translatable("message.mathmaster.geometry_altar.failed"), true);
            } else Block.popResource(server, worldPosition, spent);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Offering", offering.saveOptional(registries));
        tag.putInt("RemainingTicks", remainingTicks);
        if (summoner != null) tag.putUUID("Summoner", summoner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        offering = ItemStack.parseOptional(registries, tag.getCompound("Offering"));
        if (!offering.isEmpty()) offering.setCount(1);
        remainingTicks = offering.isEmpty() ? 0 : Math.clamp(tag.getInt("RemainingTicks"), 1, RITUAL_TICKS);
        summoner = tag.hasUUID("Summoner") ? tag.getUUID("Summoner") : null;
    }
}
