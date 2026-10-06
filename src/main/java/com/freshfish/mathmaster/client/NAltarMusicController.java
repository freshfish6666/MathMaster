package com.freshfish.mathmaster.client;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.block.NAltarBlock;
import com.freshfish.mathmaster.block.entity.NAltarBlockEntity;
import com.freshfish.mathmaster.init.ModBlocks;
import com.freshfish.mathmaster.init.ModSounds;
import com.freshfish.mathmaster.ritual.NAltarMusicSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MathMaster.MODID, value = Dist.CLIENT)
public final class NAltarMusicController {
    private static final double RANGE_SQUARED = NAltarBlockEntity.RADIUS * NAltarBlockEntity.RADIUS;
    private static final List<BlockPos> nearby = new ArrayList<>();
    private static final NAltarMusicSequence sequence = new NAltarMusicSequence(outro -> {
        var sound = new RitualSound(outro);
        Minecraft.getInstance().getSoundManager().play(sound);
        return sound;
    });
    private static ClientLevel watchedLevel;
    private static BlockPos origin;
    private static int scanTicks;
    private static long lastNanos;

    private NAltarMusicController() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        long now = System.nanoTime();
        double seconds = lastNanos == 0 ? 0 : (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        var level = minecraft.level;
        if (level != watchedLevel || minecraft.player == null || !minecraft.player.isAlive()
                || minecraft.options.getSoundSourceVolume(SoundSource.MUSIC) == 0
                || minecraft.options.getSoundSourceVolume(SoundSource.MASTER) == 0) {
            sequence.reset();
            nearby.clear();
            origin = null;
            scanTicks = 0;
            watchedLevel = level;
            return;
        }
        if (minecraft.isPaused()) return;
        // Boss music takes focus; preserve the accepted altar loop/outro behavior otherwise.
        if (GeometryHolderMusicController.ownsMusic()) {
            sequence.tick(false, false, false, seconds);
            origin = null;
            return;
        }
        Vec3 position = minecraft.player.position();
        if (scanTicks-- <= 0) {
            scanTicks = 9;
            scan(level, position);
        }

        // A chunk disappearing is not proof of ritual completion. Loaded block changes are.
        boolean ended = origin != null && loaded(level, origin) && !blood(level, origin);
        BlockPos active = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (BlockPos candidate : nearby) {
            if (!loaded(level, candidate) || !blood(level, candidate)) continue;
            double distance = candidate.getCenter().distanceToSqr(position);
            if (distance <= RANGE_SQUARED && (active == null || distance < nearest
                    || (distance == nearest && candidate.asLong() < active.asLong()))) {
                active = candidate;
                nearest = distance;
            }
        }
        if (active != null) origin = active;
        boolean inRange = origin != null && origin.getCenter().distanceToSqr(position) <= RANGE_SQUARED;
        sequence.tick(active != null, ended, inRange, seconds);
        if (active == null && !sequence.running()) origin = null;
    }

    private static void scan(ClientLevel level, Vec3 position) {
        nearby.clear();
        int minX = ((int) Math.floor(position.x - NAltarBlockEntity.RADIUS)) >> 4;
        int maxX = ((int) Math.floor(position.x + NAltarBlockEntity.RADIUS)) >> 4;
        int minZ = ((int) Math.floor(position.z - NAltarBlockEntity.RADIUS)) >> 4;
        int maxZ = ((int) Math.floor(position.z + NAltarBlockEntity.RADIUS)) >> 4;
        // At most nine loaded chunks every half second; no cubic block scan or forced load.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof NAltarBlockEntity && !entity.isRemoved()) {
                        nearby.add(entity.getBlockPos().immutable());
                    }
                }
            }
        }
    }

    private static boolean loaded(ClientLevel level, BlockPos pos) {
        return level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false) != null;
    }

    private static boolean blood(ClientLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(ModBlocks.N_ALTAR.get()) && state.getValue(NAltarBlock.HALF) == DoubleBlockHalf.LOWER
                && state.getValue(NAltarBlock.BLOOD_SACRIFICE);
    }

    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        if (sequence.running()) event.setMusic(null);
    }

    @SubscribeEvent
    public static void onStreamStarted(PlayStreamingSourceEvent event) {
        if (event.getSound() instanceof RitualSound sound) sound.started = true;
    }

    private static final class RitualSound extends AbstractTickableSoundInstance implements NAltarMusicSequence.Audio {
        private volatile boolean started;
        private double elapsed;

        private RitualSound(boolean outro) {
            super(outro ? ModSounds.N_ALTAR_OUTRO.get() : ModSounds.N_ALTAR_BLOOD_SACRIFICE.get(),
                    SoundSource.MUSIC, RandomSource.create());
            relative = true;
            attenuation = Attenuation.NONE;
            volume = 0;
            // One complete streamed play; the sequence restarts only while a blood altar is active.
            looping = false;
        }

        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {}
        @Override public boolean playing() {
            return !isStopped() && Minecraft.getInstance().getSoundManager().isActive(this);
        }
        @Override public boolean started() { return started; }
        @Override public double seconds() { return elapsed; }
        @Override public void advance(double seconds) { if (started) elapsed += seconds; }
        @Override public void volume(float value) { volume = value; }
        @Override public void cancel() {
            stop();
            Minecraft.getInstance().getSoundManager().stop(this);
        }
    }
}
