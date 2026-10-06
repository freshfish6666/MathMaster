package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.axiom.AdditionCommutativitySkill;
import com.freshfish.mathmaster.axiom.AxiomSkillData;
import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.EquippedAxiomCase;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.item.EquippedMathematicalRing;
import com.freshfish.mathmaster.item.StudyNoteItem;
import com.freshfish.mathmaster.network.UseAxiomSkillPayload;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Verifies the real Curios ring slot and centralized axiom cooldown bypass. */
final class MathematicalRingCheck {
    private MathematicalRingCheck() {
    }

    static int run(net.minecraft.server.level.ServerLevel level) {
        var player = FakePlayerFactory.getMinecraft(level);
        var curios = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new AssertionError("Curios inventory was unavailable"));
        if (curios.getStacksHandler(EquippedMathematicalRing.SLOT_ID).isEmpty()) {
            throw new AssertionError("Curios ring slot was not assigned to players");
        }
        curios.setEquippedCurio(EquippedMathematicalRing.SLOT_ID, 0,
                new ItemStack(ModItems.MATHEMATICAL_RING.get()));
        if (!EquippedMathematicalRing.isEquipped(player)) {
            throw new AssertionError("Mathematical Ring was not detected in the ring slot");
        }

        var data = player.getData(ModAttachments.AXIOM_SKILL_DATA);
        data.setAdditionCommutativityCooldownTicks(200);
        data.setAdditiveInverseCooldownTicks(200);
        data.setEuclidPrimeInfinityCooldownTicks(200);
        data.setPlayfairCooldownTicks(200);
        data.setInvolutionCooldownTicks(200);
        data.setGeodesicCooldownTicks(200);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        if (data.getAdditionCommutativityCooldownTicks() != 0
                || data.getAdditiveInverseCooldownTicks() != 0
                || data.getEuclidPrimeInfinityCooldownTicks() != 0
                || data.getPlayfairCooldownTicks() != 0
                || data.getInvolutionCooldownTicks() != 0
                || data.getGeodesicCooldownTicks() != 0) {
            throw new AssertionError("Mathematical Ring did not clear every axiom cooldown");
        }

        curios.setEquippedCurio(EquippedMathematicalRing.SLOT_ID, 0, ItemStack.EMPTY);
        setAllCooldowns(data, 10);
        // Individual skills must never advance the shared countdown.
        new AdditionCommutativitySkill().onPlayerTick(new PlayerTickEvent.Post(player));
        assertAllCooldowns(data, 10);
        // Exercise the real registrations: one event must decrement all six exactly once.
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        assertAllCooldowns(data, 9);
        setAllCooldowns(data, 1);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        assertAllCooldowns(data, 0);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        assertAllCooldowns(data, 0);
        if (data.hasCooldowns()) {
            throw new AssertionError("Expired cooldowns remained active");
        }
        // Each field must independently keep ticking when the other five are idle.
        String[] keys = {"addition_commutativity_cooldown", "additive_inverse_cooldown",
                "euclid_prime_infinity_cooldown", "playfair_cooldown",
                "involution_cooldown", "geodesic_cooldown"};
        for (String key : keys) {
            CompoundTag legacy = new CompoundTag();
            legacy.putInt(key, 2);
            data.deserializeNBT(level.registryAccess(), legacy);
            if (!data.hasCooldowns()) throw new AssertionError("Missed active cooldown: " + key);
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
            CompoundTag saved = data.serializeNBT(level.registryAccess());
            for (String savedKey : keys) {
                if (saved.getInt(savedKey) != (savedKey.equals(key) ? 1 : 0)) {
                    throw new AssertionError("Cooldown/NBT mismatch: " + savedKey);
                }
            }
            AxiomSkillData restored = new AxiomSkillData();
            restored.deserializeNBT(level.registryAccess(), saved);
            if (!restored.serializeNBT(level.registryAccess()).equals(saved)) {
                throw new AssertionError("Cooldown NBT round trip changed: " + key);
            }
        }
        data.clearCooldowns();
        checkActivation(player);
        return 19;
    }

    private static void checkActivation(ServerPlayer player) {
        var curios = CuriosApi.getCuriosInventory(player).orElseThrow();
        ItemStack previousCase = EquippedAxiomCase.find(player).orElse(ItemStack.EMPTY).copy();
        var data = player.getData(ModAttachments.AXIOM_SKILL_DATA);
        var pollution = player.getData(ModAttachments.DIGITAL_POLLUTION);
        try {
            player.setPos(120, 250, 120);
            var axiomCase = new ItemStack(ModItems.AXIOM_CASE.get());
            axiomCase.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                    StudyNoteItem.createAxiomNote(AxiomDefinition.EUCLID_PRIME_INFINITY, 5))));
            curios.setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, axiomCase);
            curios.setEquippedCurio(EquippedMathematicalRing.SLOT_ID, 0,
                    new ItemStack(ModItems.MATHEMATICAL_RING.get()));
            pollution.reset();
            setAllCooldowns(data, 200);
            invokeSelectedSkill(player);
            assertAllCooldowns(data, 0);
            if (pollution.getValue() != 20) throw new AssertionError("Ring changed skill pollution cost");
            invokeSelectedSkill(player);
            assertAllCooldowns(data, 0);
            if (pollution.getValue() != 40) throw new AssertionError("Ring blocked a second same-tick use");
            curios.setEquippedCurio(EquippedMathematicalRing.SLOT_ID, 0, ItemStack.EMPTY);
            invokeSelectedSkill(player);
            if (data.getEuclidPrimeInfinityCooldownTicks() != 400 || pollution.getValue() != 60) {
                throw new AssertionError("Skill cooldown/cost changed after ring removal");
            }
            invokeSelectedSkill(player);
            if (data.getEuclidPrimeInfinityCooldownTicks() != 400 || pollution.getValue() != 60) {
                throw new AssertionError("Cooldown failed to block repeated skill use");
            }
        } finally {
            curios.setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, previousCase);
            curios.setEquippedCurio(EquippedMathematicalRing.SLOT_ID, 0, ItemStack.EMPTY);
            data.clearCooldowns();
            pollution.reset();
        }
    }

    private static void invokeSelectedSkill(ServerPlayer player) {
        try {
            var method = UseAxiomSkillPayload.class.getDeclaredMethod("useSelectedSkill", ServerPlayer.class);
            method.setAccessible(true);
            method.invoke(null, player);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Skill network entry failed", failure);
        }
    }

    private static void setAllCooldowns(AxiomSkillData data, int ticks) {
        data.setAdditionCommutativityCooldownTicks(ticks);
        data.setAdditiveInverseCooldownTicks(ticks);
        data.setEuclidPrimeInfinityCooldownTicks(ticks);
        data.setPlayfairCooldownTicks(ticks);
        data.setInvolutionCooldownTicks(ticks);
        data.setGeodesicCooldownTicks(ticks);
    }

    private static void assertAllCooldowns(AxiomSkillData data, int expected) {
        if (data.getAdditionCommutativityCooldownTicks() != expected
                || data.getAdditiveInverseCooldownTicks() != expected
                || data.getEuclidPrimeInfinityCooldownTicks() != expected
                || data.getPlayfairCooldownTicks() != expected
                || data.getInvolutionCooldownTicks() != expected
                || data.getGeodesicCooldownTicks() != expected) {
            throw new AssertionError("Expected every cooldown to be " + expected);
        }
    }
}
