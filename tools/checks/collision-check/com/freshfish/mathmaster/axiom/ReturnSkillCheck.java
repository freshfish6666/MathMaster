package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import com.freshfish.mathmaster.item.StudyNoteItem;
import com.freshfish.mathmaster.network.ReturnControlPayload;
import com.freshfish.mathmaster.event.DigitalPollutionHandler;
import com.freshfish.mathmaster.pollution.DigitalPollutionDamageSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;

/** Real Curios, owner snapshots, persistence and dimension travel in the isolated server. */
public final class ReturnSkillCheck {
    private static final ReturnSkill HANDLER = new ReturnSkill();
    private static int checks;

    public static int run(ServerLevel level) throws Exception {
        checks = 0;
        require(AxiomDefinition.RETURN.ordinal() == 12 && AxiomDefinition.GEODESIC.ordinal() == 9
                && AxiomDefinition.RETURN.category() == AxiomDefinition.AxiomCategory.LOGIC
                && AxiomDefinition.RETURN.isActive() && AxiomDefinition.RETURN.acceptsMaterial(new ItemStack(Items.QUARTZ)), "registration and old ordinals");
        for (int grade = 1; grade <= 6; grade++) require(AxiomDefinition.RETURN.acceptsNoteLevel(grade) == (grade >= 2 && grade <= 5), "note level bounds");
        AxiomSkillData old = new AxiomSkillData();
        CompoundTag legacy = new CompoundTag(); legacy.putInt("geodesic_cooldown", 13);
        old.deserializeNBT(level.registryAccess(), legacy);
        require(old.getGeodesicCooldownTicks() == 13 && old.getReturnCooldownTicks() == 0
                && old.returnAnchors().selected() == -1 && old.returnAnchors().visible(5).stream().allMatch(java.util.Optional::isEmpty), "old attachment compatibility");
        try (var probe = new SkillCheckPlayer(level, "ReturnCheck")) {
            ServerPlayer player = probe.player;
            var immunity = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime"); immunity.setAccessible(true); immunity.setInt(player, 0);
            level.getChunk(0, 0);
            equip(player, 5, 0);
            ReturnSkill.control(player, ReturnControlPayload.RECORD, 0);
            require(ReturnSkill.anchors(player).get(0).isEmpty(), "closed/forged screen operations rejected");
            open(player);
            var snapshot = probe.returnPackets.getLast();
            require(snapshot.open() && snapshot.level() == 5 && snapshot.selected() == -1 && snapshot.anchors().size() == 5, "tap opens five slots");
            for (int slot = 0; slot < 5; slot++) {
                player.moveTo(3.125 + slot, 240.375, 3.875 + slot);
                ReturnSkill.control(player, ReturnControlPayload.SELECT, slot);
                ReturnSkill.control(player, ReturnControlPayload.RECORD, slot);
                var anchor = ReturnSkill.anchors(player).get(slot).orElseThrow();
                require(anchor.dimension().equals(Level.OVERWORLD.location()) && anchor.x() == 3.125 + slot
                        && anchor.y() == 240.375 && anchor.z() == 3.875 + slot, "server records exact coordinates; same dimension multiple anchors");
            }
            var data = player.getData(ModAttachments.AXIOM_SKILL_DATA);
            data.setReturnCooldownTicks(117);
            var saved = data.serializeNBT(level.registryAccess());
            var restored = new AxiomSkillData(); restored.deserializeNBT(level.registryAccess(), saved);
            require(restored.returnAnchors().save().equals(data.returnAnchors().save()) && restored.getReturnCooldownTicks() == 117, "all five slots and cooldown persist");
            equip(player, 2, 0); tick(player, 1);
            require(ReturnSkill.anchors(player).selected() == -1 && ReturnSkill.anchors(player).get(4).isPresent()
                    && probe.returnPackets.getLast().anchors().size() == 2, "hidden data preserved; selection invalidated; hidden coordinates not sent");
            ReturnSkill.control(player, ReturnControlPayload.SELECT, 4);
            ReturnSkill.control(player, ReturnControlPayload.DELETE, 4);
            require(ReturnSkill.anchors(player).get(4).isPresent() && ReturnSkill.anchors(player).selected() == -1, "hidden slot mutation rejected");
            equip(player, 2, 4); tick(player, 1);
            require(AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.RETURN) == 5
                    && probe.returnPackets.getLast().anchors().get(4).isPresent(), "induction restores highest slot");
            ReturnSkill.control(player, ReturnControlPayload.SELECT, 4);
            resetCosts(player); ReturnSkill.press(player, 5); tick(player, 7);
            equip(player, 2, 0); tick(player, 1);
            require(!ReturnSkill.isCharging(player) && pollution(player) == 0 && cooldown(player) == 0, "induction removal invalidates charged target without cost");
            equip(player, 2, 2); tick(player, 1);
            require(AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.RETURN) == 3 && probe.returnPackets.getLast().level() == 3, "induction floor 3");
            ReturnSkill.control(player, ReturnControlPayload.SELECT, 0);
            ReturnSkill.control(player, ReturnControlPayload.DELETE, 0);
            require(ReturnSkill.anchors(player).get(0).isEmpty() && ReturnSkill.anchors(player).selected() == 0, "delete leaves selected empty slot");
            ReturnSkill.control(player, ReturnControlPayload.DELETE, 0);
            require(ReturnSkill.anchors(player).get(0).isEmpty(), "empty delete no effect");
            ReturnSkill.press(player, 3); tick(player, 7);
            require(!ReturnSkill.isCharging(player) && pollution(player) == 0, "empty target cannot cast");

            for (int grade = 2; grade <= 5; grade++) {
                equip(player, grade, 0); resetCosts(player);
                var anchor = new ReturnAnchorData.Anchor(Level.OVERWORLD.location(), 8.25, 240.5, 8.75);
                ReturnSkill.anchors(player).set(0, anchor); ReturnSkill.anchors(player).select(0, grade);
                player.moveTo(4, 240, 4);
                level.setBlockAndUpdate(BlockPos.containing(anchor.x(), anchor.y(), anchor.z()), Blocks.STONE.defaultBlockState());
                ReturnSkill.press(player, grade); tick(player, 39);
                require(player.getX() == 4 && pollution(player) == 0 && cooldown(player) == 0, "no early travel or cost");
                tick(player, 1);
                require(player.position().equals(new Vec3(anchor.x(), anchor.y(), anchor.z())) && pollution(player) == 2 * grade
                        && cooldown(player) == (80 - 10 * grade) * 20 && !ReturnSkill.isCharging(player), "exact blocked destination, grade cost and cooldown");
                ReturnSkill.press(player, grade); tick(player, 7);
                require(!ReturnSkill.isCharging(player) && pollution(player) == 2 * grade, "cooldown rejects long cast");
                open(player);
                require(probe.returnPackets.getLast().open(), "tap management works during cooldown");
            }
            equip(player, 2, 0); resetCosts(player); player.moveTo(4, 240, 4);
            ReturnSkill.press(player, 2); tick(player, 10); ReturnSkill.release(player);
            require(!ReturnSkill.isCharging(player) && pollution(player) == 0 && cooldown(player) == 0, "early release cancels free");
            ReturnSkill.press(player, 2); tick(player, 6); player.moveTo(5, 240, 4); tick(player, 1);
            require(!ReturnSkill.isCharging(player) && pollution(player) == 0 && cooldown(player) == 0, "movement cancels free");
            ReturnSkill.press(player, 2); tick(player, 6);
            player.hurt(player.damageSources().magic(), 1);
            require(!ReturnSkill.isCharging(player) && pollution(player) == 0 && cooldown(player) == 0, "actual damage cancels free");
            ReturnSkill.anchors(player).set(0, new ReturnAnchorData.Anchor(ResourceLocation.parse("mathmaster:missing_dimension"), 0, 80, 0));
            ReturnSkill.press(player, 2); tick(player, 7);
            require(!ReturnSkill.isCharging(player) && pollution(player) == 0 && cooldown(player) == 0, "missing dimension fails free");

            var nether = level.getServer().getLevel(Level.NETHER); nether.getChunk(0, 0);
            ReturnSkill.anchors(player).set(0, new ReturnAnchorData.Anchor(Level.NETHER.location(), 4.25, 90.5, 4.75));
            ReturnSkill.press(player, 2); tick(player, 40);
            require(player.serverLevel() == nether && player.getX() == 4.25 && player.getY() == 90.5 && player.getZ() == 4.75
                    && pollution(player) == 4 && cooldown(player) == 1200, "actual overworld to Nether travel and single billing");
            resetCosts(player);
            ReturnSkill.anchors(player).set(0, new ReturnAnchorData.Anchor(Level.OVERWORLD.location(), 4.125, 240, 4.875));
            ReturnSkill.press(player, 2); tick(player, 40);
            require(player.serverLevel() == level && player.getX() == 4.125 && pollution(player) == 4, "actual Nether return");

            resetCosts(player); equip(player, 5, 0);
            CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("ring", 0, new ItemStack(ModItems.MATHEMATICAL_RING.get()));
            ReturnSkill.press(player, 5); tick(player, 40);
            require(cooldown(player) == 0 && pollution(player) == 10, "ring bypasses cooldown, not pollution");
            ReturnSkill.press(player, 5); tick(player, 40);
            require(cooldown(player) == 0 && pollution(player) == 20 && ReturnSkill.anchors(player).get(0).isPresent(), "repeat return does not consume anchor");
            CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("ring", 0, ItemStack.EMPTY);

            resetCosts(player); ReturnSkill.press(player, 5); tick(player, 6);
            CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, ItemStack.EMPTY);
            tick(player, 1);
            require(!ReturnSkill.isCharging(player) && pollution(player) == 0 && cooldown(player) == 0
                    && ReturnSkill.anchors(player).get(4).isPresent(), "unequip cancels, keeps records");
            data.setReturnCooldownTicks(20); data.tickCooldowns(); require(cooldown(player) == 19, "shared tick includes new cooldown");
            data.clearCooldowns(); require(cooldown(player) == 0 && data.returnAnchors().get(4).isPresent(), "cooldown clearing preserves anchors");
        }
        checkLethalReturn(level);
        return checks;
    }

    private static void checkLethalReturn(ServerLevel overworld) throws Exception {
        var server = overworld.getServer();
        var end = server.getLevel(Level.END);
        var nether = server.getLevel(Level.NETHER);
        for (ServerLevel[] route : new ServerLevel[][] {
                {end, overworld}, {nether, overworld}, {overworld, end}, {overworld, overworld}}) {
            var origin = route[0]; var destination = route[1];
            destination.getChunk(0, 0);
            try (var probe = new SkillCheckPlayer(origin, "ReturnLethal")) {
                var player = probe.player;
                equip(player, 5, 0);
                player.getData(ModAttachments.DIGITAL_POLLUTION).add(99);
                ReturnSkill.anchors(player).set(0, new ReturnAnchorData.Anchor(destination.dimension().location(), 4.25, 240, 4.75));
                ReturnSkill.anchors(player).select(0, 5);
                int[] deaths = {0};
                java.util.function.Consumer<LivingDeathEvent> observe = event -> {
                    if (event.getEntity() == player) {
                        deaths[0]++;
                        require(event.getSource() instanceof DigitalPollutionDamageSource, "lethal return keeps pollution death source");
                    }
                };
                NeoForge.EVENT_BUS.addListener(observe);
                try {
                    ReturnSkill.press(player, 5); tick(player, 40);
                    require(player.serverLevel() == destination && player.getX() == 4.25 && cooldown(player) == 600,
                            "lethal return still completes travel and cooldown");
                    var handler = new DigitalPollutionHandler();
                    if (origin != destination) {
                        require(player.isChangingDimension() && player.isAlive() && pollution(player) == 100,
                                "99 pollution level V cross-dimension travel keeps pending 100 instead of free reset");
                        for (int i = 0; i < 3; i++) handler.onPlayerTick(new PlayerTickEvent.Post(player));
                        require(player.isAlive() && pollution(player) == 100 && deaths[0] == 0,
                                "waiting for teleport acknowledgement neither clears pollution nor kills early");
                        var saved = player.getData(ModAttachments.DIGITAL_POLLUTION).serializeNBT(origin.registryAccess());
                        var restored = new com.freshfish.mathmaster.pollution.DigitalPollutionData();
                        restored.deserializeNBT(origin.registryAccess(), saved);
                        require(restored.getValue() == 100, "pending threshold survives save/reload without new NBT keys");
                        acknowledgeTravel(player);
                        require(!player.isChangingDimension(), "real teleport acknowledgement removes vanilla travel protection");
                        handler.onPlayerTick(new PlayerTickEvent.Post(player));
                    }
                    require(!player.isAlive() && pollution(player) == 0 && deaths[0] == 1,
                            "threshold kills once and resets only when lethal damage can be applied");
                    handler.onPlayerTick(new PlayerTickEvent.Post(player));
                    require(deaths[0] == 1, "no duplicate death after settlement");
                } finally { NeoForge.EVENT_BUS.unregister(observe); }
            }
        }
    }

    private static void acknowledgeTravel(ServerPlayer player) throws Exception {
        var field = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");
        field.setAccessible(true);
        player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(field.getInt(player.connection)));
    }

    private static void equip(ServerPlayer player, int grade, int induction) {
        var notes = new ArrayList<ItemStack>(); notes.add(StudyNoteItem.createAxiomNote(AxiomDefinition.RETURN, grade));
        if (induction > 0) notes.add(StudyNoteItem.createAxiomNote(AxiomDefinition.MATHEMATICAL_INDUCTION, induction));
        ItemStack axiomCase = new ItemStack(ModItems.AXIOM_CASE.get());
        axiomCase.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(notes));
        AxiomCaseItem.setSelectedAxiom(axiomCase, AxiomDefinition.RETURN);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, axiomCase);
    }
    private static void open(ServerPlayer player) {
        ReturnSkill.press(player, AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.RETURN)); ReturnSkill.release(player);
    }
    private static void resetCosts(ServerPlayer player) {
        ReturnSkill.cancel(player); player.getData(ModAttachments.AXIOM_SKILL_DATA).clearCooldowns();
        player.getData(ModAttachments.DIGITAL_POLLUTION).reset();
    }
    private static int pollution(ServerPlayer player) { return player.getData(ModAttachments.DIGITAL_POLLUTION).getValue(); }
    private static int cooldown(ServerPlayer player) { return player.getData(ModAttachments.AXIOM_SKILL_DATA).getReturnCooldownTicks(); }
    private static void tick(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            ReturnSkill.control(player, ReturnControlPayload.HEARTBEAT, -1);
            HANDLER.onPlayerTick(new PlayerTickEvent.Post(player));
        }
    }
    private static void require(boolean value, String message) { checks++; if (!value) throw new AssertionError("Return: " + message); }
}
