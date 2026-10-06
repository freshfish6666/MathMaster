package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.AxiomSkillData;
import com.freshfish.mathmaster.axiom.EquippedAxiomCase;
import com.freshfish.mathmaster.axiom.GeodesicMovement;
import com.freshfish.mathmaster.axiom.GeodesicSkill;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.item.StudyNoteItem;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Uses real players, movement packets and Curios in an isolated disposable world. */
final class GeodesicSkillCheck {
    private static final GeodesicSkill HANDLER = new GeodesicSkill();

    static int run(ServerLevel level) throws Exception {
        long originalTime = level.getGameTime();
        var player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.fromString("b361c034-f501-4475-a8d7-93fbe24dd008"), "GeodesicCheck"),
                ClientInformation.createDefault());
        var listener = new ProbeListener(level, player);
        player.connection = listener;
        player.moveTo(4, 200, 4, 0, 0);
        level.addNewPlayer(player);
        var curios = CuriosApi.getCuriosInventory(player).orElseThrow();
        int checks = 0;
        try {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 3; z++) level.getChunk(x, z);
            for (int x = -2; x < 22; x++) for (int z = -2; z < 22; z++) {
                level.setBlockAndUpdate(new BlockPos(x, 199, z), Blocks.STONE.defaultBlockState());
                for (int y = 200; y < 207; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            }
            require(AxiomDefinition.GEODESIC.isActive()
                    && AxiomDefinition.GEODESIC.category() == AxiomDefinition.AxiomCategory.GEOMETRY
                    && !AxiomDefinition.GEODESIC.acceptsNoteLevel(1)
                    && AxiomDefinition.GEODESIC.acceptsMaterial(new ItemStack(Items.ENDER_PEARL)), "registration/material");
            checks++;
            for (int grade = 2; grade <= 5; grade++) {
                equip(player, grade);
                reset(player, level, listener);
                Vec3 origin = player.position();
                GeodesicSkill.press(player, grade);
                tick(player, level, 2);
                require(player.position().equals(origin) && pollution(player) == 0, "pending press moved/charged early");
                GeodesicSkill.release(player);
                require(Math.abs(player.getZ() - origin.z - 2 * grade) < 1.e-5, "dash distance at level " + grade);
                require(pollution(player) == grade && cooldown(player) == (32 - 6 * grade) * 20, "dash cost/cooldown");
                require(player.fallDistance == 0 && !player.isNoGravity(), "dash gravity/fall cleanup");
                GeodesicSkill.press(player, grade);
                tick(player, level, 6);
                GeodesicSkill.release(player);
                require(pollution(player) == grade, "cooldown allowed another use");
                checks += 3;
            }
            equip(player, 2);
            reset(player, level, listener);
            player.moveTo(4.5, 200, 4.5, 0, 0);
            aimAt(player, new Vec3(4.5, 200, 8.5));
            GeodesicSkill.press(player, 2); GeodesicSkill.release(player);
            require(player.position().distanceTo(new Vec3(4.5, 200, 8.5)) < .002
                    && pollution(player) == 2, "aiming four blocks away on a flat floor failed");
            checks++;
            reset(player, level, listener); player.moveTo(4.5, 200, 4.5, 0, 0);
            level.setBlockAndUpdate(new BlockPos(4, 200, 6), Blocks.STONE_SLAB.defaultBlockState());
            aimAt(player, new Vec3(4.5, 200, 8.5));
            GeodesicSkill.press(player, 2); GeodesicSkill.release(player);
            require(player.getZ() > 8.49 && Math.abs(player.getY() - 200) < .002, "visible floor beyond a low slab failed");
            level.setBlockAndUpdate(new BlockPos(4, 200, 6), Blocks.AIR.defaultBlockState()); checks++;
            equip(player, 5); reset(player, level, listener); player.moveTo(4.5, 200, 4.5, 0, 0);
            level.setBlockAndUpdate(new BlockPos(4, 200, 8), Blocks.STONE.defaultBlockState());
            aimAt(player, new Vec3(4.5, 200.5, 8));
            GeodesicSkill.press(player, 5); GeodesicSkill.release(player);
            require(player.getZ() > 8.49 && Math.abs(player.getY() - 201) < .002, "one-block ledge was treated as a wall");
            level.setBlockAndUpdate(new BlockPos(4, 200, 8), Blocks.STONE_SLAB.defaultBlockState());
            reset(player, level, listener); player.moveTo(4.5, 200, 4.5, 0, 0);
            aimAt(player, new Vec3(4.5, 200.5, 8.5));
            GeodesicSkill.press(player, 5); GeodesicSkill.release(player);
            require(player.getZ() > 8.49 && Math.abs(player.getY() - 200.5) < .002, "slab landing height was incorrect");
            level.setBlockAndUpdate(new BlockPos(4, 200, 8), Blocks.AIR.defaultBlockState()); checks += 2;
            reset(player, level, listener); player.moveTo(4.5, 200, 4.5, 0, 0);
            for (int x = 2; x <= 6; x++) for (int y = 200; y <= 203; y++)
                level.setBlockAndUpdate(new BlockPos(x, y, 7), Blocks.STONE.defaultBlockState());
            GeodesicSkill.press(player, 5); GeodesicSkill.release(player);
            require(player.getZ() < 6.71 && player.getZ() > 6.5
                    && level.noCollision(player, player.getBoundingBox()), "blink passed through a tall wall or landed inside it");
            for (int x = 2; x <= 6; x++) for (int y = 200; y <= 203; y++)
                level.setBlockAndUpdate(new BlockPos(x, y, 7), Blocks.AIR.defaultBlockState()); checks++;
            reset(player, level, listener); player.moveTo(4.5, 200, 4.69, 0, 0);
            for (int y = 200; y <= 203; y++) level.setBlockAndUpdate(new BlockPos(4, y, 5), Blocks.STONE.defaultBlockState());
            GeodesicSkill.press(player, 5); GeodesicSkill.release(player);
            require(pollution(player) == 0 && cooldown(player) == 0, "failed landing consumed pollution/cooldown");
            for (int y = 200; y <= 203; y++) level.setBlockAndUpdate(new BlockPos(4, y, 5), Blocks.AIR.defaultBlockState()); checks++;

            equip(player, 5);
            reset(player, level, listener);
            // A thin wall across a diagonal path: no tunnelling, side sliding or floor snagging.
            for (int z = 4; z <= 15; z++) for (int y = 200; y <= 202; y++)
                level.setBlockAndUpdate(new BlockPos(8, y, z), Blocks.STONE.defaultBlockState());
            var diagonal = GeodesicMovement.sweep(player, new Vec3(7, 0, 7));
            require(diagonal.blocked() && player.getX() + diagonal.movement().x < 7.7
                    && Math.abs(diagonal.movement().x - diagonal.movement().z) < 1.e-6, "diagonal wall sweep");
            require(!GeodesicMovement.sweep(player, new Vec3(0, 0, 10)).blocked(), "horizontal flight snagged floor");
            for (int z = 4; z <= 15; z++) for (int y = 200; y <= 202; y++)
                level.setBlockAndUpdate(new BlockPos(8, y, z), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(4, 203, 4), Blocks.STONE.defaultBlockState());
            var ceiling = GeodesicMovement.sweep(player, new Vec3(0, 10, 0));
            require(ceiling.blocked() && ceiling.movement().y < 1.21, "ceiling missed player's head");
            level.setBlockAndUpdate(new BlockPos(4, 203, 4), Blocks.AIR.defaultBlockState());
            checks += 3;

            var cow = EntityType.COW.create(level);
            require(cow != null, "cow creation");
            cow.setNoAi(true);
            cow.moveTo(4, 200, 7);
            require(level.addFreshEntity(cow), "cow insertion");
            GeodesicSkill.press(player, 5);
            GeodesicSkill.release(player);
            require(player.getZ() < cow.getBoundingBox().minZ - .29
                    && !player.getBoundingBox().intersects(cow.getBoundingBox()), "dash penetrated living entity");
            cow.discard();
            checks++;
            reset(player, level, listener);
            var item = new ItemEntity(level, 4, 200, 7, new ItemStack(Items.DIAMOND));
            var orb = new ExperienceOrb(level, 4, 200, 9, 5);
            level.addFreshEntity(item);
            level.addFreshEntity(orb);
            require(!GeodesicMovement.sweep(player, new Vec3(0, 0, 10)).blocked(), "items/XP blocked flight");
            item.discard(); orb.discard();
            checks++;

            for (int grade = 2; grade <= 5; grade++) {
                equip(player, grade);
                reset(player, level, listener);
                GeodesicSkill.press(player, grade);
                tick(player, level, 6);
                require(GeodesicSkill.isFlying(player) && player.isNoGravity() && pollution(player) == 1, "hold activation");
                var step = GeodesicMovement.sweep(player, player.getLookAngle().scale(GeodesicSkill.speedPerTick(grade)));
                require(Math.abs(step.movement().length() * 20 - 6 * grade) < 1.e-6, "flight speed");
                player.moveTo(4, 204, 4, 0, 0);
                Vec3 start = player.position();
                for (int physicsTick = 0; physicsTick < 20; physicsTick++) {
                    require(!GeodesicMovement.flyStep(player, grade).blocked(), "unexpected flight obstruction");
                }
                require(Math.abs(player.position().distanceTo(start) - 6 * grade) < 1.e-6,
                        "actual flight distance over twenty ticks at level " + grade);
                checks++;
                tick(player, level, 19); // Exactly 20 flight ticks: still one charged second.
                require(pollution(player) == 1 && player.fallDistance == 0, "first flight second billing");
                GeodesicSkill.release(player);
                require(!GeodesicSkill.isFlying(player) && !player.isNoGravity()
                        && cooldown(player) == GeodesicSkill.shortCooldownTicks(grade) + 140, "one-second flight cooldown/cleanup");
                checks += 3;
            }
            equip(player, 5);
            reset(player, level, listener);
            GeodesicSkill.press(player, 5); tick(player, level, 26); // 21 flight ticks.
            require(pollution(player) == 2, "partial second not rounded up");
            GeodesicSkill.release(player);
            require(cooldown(player) == 182, "fractional flight cooldown");
            checks++;

            reset(player, level, listener);
            player.setPos(4, 202, 4); listener.resetPosition();
            GeodesicSkill.press(player, 5); tick(player, level, 6);
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(4, 202, 4.75, false));
            require(Math.abs(player.getZ() - 4.75) < 1.e-6 && GeodesicSkill.isFlying(player), "vanilla flight packet rejected");
            Field floating = field("clientIsFloating");
            Field floatingTicks = field("aboveGroundTickCount");
            floating.setBoolean(listener, true); floatingTicks.setInt(listener, 1000);
            listener.tick();
            require(!floating.getBoolean(listener) && !listener.disconnected && !player.getAbilities().mayfly,
                    "authorized flight kicked or granted creative flight");
            checks += 2;
            GeodesicSkill.cancel(player);
            reset(player, level, listener); player.moveTo(4, 204, 4, 0, 0); listener.resetPosition();
            GeodesicSkill.press(player, 5); tick(player, level, 6);
            Vec3 packetStart = player.position();
            for (int packetTick = 0; packetTick < 20; packetTick++) {
                level.getServer().getWorldData().overworldData().setGameTime(level.getGameTime() + 1);
                GeodesicSkill.heartbeat(player);
                listener.tick();
                listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY(),
                        player.getZ() + GeodesicSkill.speedPerTick(5), false));
                require(GeodesicSkill.isFlying(player), "thirty-block-per-second movement packet rejected");
            }
            require(Math.abs(player.position().distanceTo(packetStart) - 30) < 1.e-6,
                    "server did not accept thirty blocks of flight in twenty ticks"); checks++;
            reset(player, level, listener); player.setPos(4, 202, 4.75); listener.resetPosition();
            GeodesicSkill.press(player, 5); tick(player, level, 6);
            // The same real packet path must stop on a creature and reject excessive movement.
            cow = EntityType.COW.create(level); cow.setNoAi(true); cow.moveTo(4, 202, 6);
            level.addFreshEntity(cow);
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(4, 202, 6, false));
            require(!GeodesicSkill.isFlying(player) && !player.getBoundingBox().intersects(cow.getBoundingBox()),
                    "server packet bypassed entity collision");
            cow.discard(); checks++;
            reset(player, level, listener);
            GeodesicSkill.press(player, 5); tick(player, level, 6);
            listener.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(4, 200, 30, false));
            require(!GeodesicSkill.isFlying(player) && player.getZ() == 4, "excessive flight packet accepted");
            checks++;

            reset(player, level, listener);
            GeodesicSkill.press(player, 5); tick(player, level, 6);
            curios.setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, ItemStack.EMPTY);
            tick(player, level, 1);
            require(!GeodesicSkill.isFlying(player) && !player.isNoGravity(), "unequip did not end flight"); checks++;
            equip(player, 5); reset(player, level, listener);
            GeodesicSkill.press(player, 5); tick(player, level, 6);
            level.getServer().getWorldData().overworldData().setGameTime(level.getGameTime() + 31);
            HANDLER.onPlayerTick(new PlayerTickEvent.Post(player));
            require(!GeodesicSkill.isFlying(player) && !player.isNoGravity(), "missing heartbeat left flight active"); checks++;
            reset(player, level, listener);
            GeodesicSkill.press(player, 5); GeodesicSkill.cancel(player); GeodesicSkill.release(player);
            require(pollution(player) == 0 && cooldown(player) == 0 && player.getZ() == 4, "GUI cancel caused a dash"); checks++;
            reset(player, level, listener);
            player.setNoGravity(true);
            GeodesicSkill.press(player, 5); tick(player, level, 6); GeodesicSkill.release(player);
            require(player.isNoGravity(), "pre-existing no-gravity flag lost"); player.setNoGravity(false); checks++;

            reset(player, level, listener);
            curios.setEquippedCurio("ring", 0, new ItemStack(ModItems.MATHEMATICAL_RING.get()));
            GeodesicSkill.press(player, 5); GeodesicSkill.release(player);
            require(cooldown(player) == 0 && pollution(player) == 5, "ring dash bypass/cost");
            GeodesicSkill.press(player, 5); tick(player, level, 6); GeodesicSkill.release(player);
            require(cooldown(player) == 0 && pollution(player) == 6, "ring flight bypass/cost");
            curios.setEquippedCurio("ring", 0, ItemStack.EMPTY); checks += 2;

            var data = player.getData(ModAttachments.AXIOM_SKILL_DATA);
            data.setGeodesicCooldownTicks(182); data.setPlayfairCooldownTicks(55);
            var restored = new AxiomSkillData(); restored.deserializeNBT(level.registryAccess(), data.serializeNBT(level.registryAccess()));
            require(restored.getGeodesicCooldownTicks() == 182 && restored.getPlayfairCooldownTicks() == 55, "cooldown NBT round trip");
            var legacy = new CompoundTag(); legacy.putInt("playfair_cooldown", 91);
            restored.deserializeNBT(level.registryAccess(), legacy);
            require(restored.getGeodesicCooldownTicks() == 0 && restored.getPlayfairCooldownTicks() == 91, "old save compatibility");
            checks += 2;

            reset(player, level, listener);
            GeodesicSkill.press(player, 5); tick(player, level, 6);
            require(player.hurt(level.damageSources().genericKill(), Float.MAX_VALUE), "death damage failed");
            require(!player.isAlive() && !GeodesicSkill.isFlying(player) && !player.isNoGravity(), "death cleanup failed");
            checks++;
            return checks;
        } finally {
            GeodesicSkill.cancel(player);
            level.getServer().getWorldData().overworldData().setGameTime(originalTime);
            player.discard();
        }
    }

    private static void equip(ServerPlayer player, int grade) {
        var axiomCase = new ItemStack(ModItems.AXIOM_CASE.get());
        axiomCase.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(
                List.of(StudyNoteItem.createAxiomNote(AxiomDefinition.GEODESIC, grade))));
        AxiomCaseItem.setSelectedAxiom(axiomCase, AxiomDefinition.GEODESIC);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, axiomCase);
    }

    private static void aimAt(ServerPlayer player, Vec3 target) {
        Vec3 direction = target.subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        player.setXRot((float) Math.toDegrees(Math.atan2(-direction.y, direction.horizontalDistance())));
    }

    private static void reset(ServerPlayer player, ServerLevel level, ProbeListener listener) throws Exception {
        GeodesicSkill.cancel(player);
        player.getData(ModAttachments.AXIOM_SKILL_DATA).clearCooldowns();
        player.getData(ModAttachments.DIGITAL_POLLUTION).reset();
        player.setNoGravity(false); player.fallDistance = 10;
        player.moveTo(4, 200, 4, 0, 0); player.setDeltaMovement(Vec3.ZERO);
        listener.resetPosition();
        field("awaitingPositionFromClient").set(listener, null);
    }

    private static void tick(ServerPlayer player, ServerLevel level, int count) {
        for (int i = 0; i < count; i++) {
            level.getServer().getWorldData().overworldData().setGameTime(level.getGameTime() + 1);
            GeodesicSkill.heartbeat(player);
            HANDLER.onPlayerTick(new PlayerTickEvent.Post(player));
        }
    }

    private static int pollution(ServerPlayer player) { return player.getData(ModAttachments.DIGITAL_POLLUTION).getValue(); }
    private static int cooldown(ServerPlayer player) { return player.getData(ModAttachments.AXIOM_SKILL_DATA).getGeodesicCooldownTicks(); }
    private static Field field(String name) throws Exception {
        Field field = ServerGamePacketListenerImpl.class.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError("Geodesic: " + message); }

    private static final class ProbeListener extends ServerGamePacketListenerImpl {
        private boolean disconnected;
        private ProbeListener(ServerLevel level, ServerPlayer player) {
            super(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }
        @Override public void send(Packet<?> packet) {}
        @Override public void send(Packet<?> packet, PacketSendListener callback) {}
        @Override public void disconnect(Component reason) { this.disconnected = true; }
    }
}
