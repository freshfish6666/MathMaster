package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.AxiomEffectManager;
import com.freshfish.mathmaster.axiom.AxiomCooldownHandler;
import com.freshfish.mathmaster.axiom.AdditionCommutativitySkill;
import com.freshfish.mathmaster.axiom.AdditiveInverseSkill;
import com.freshfish.mathmaster.axiom.EquippedAxiomCase;
import com.freshfish.mathmaster.axiom.EuclidPrimeInfinitySkill;
import com.freshfish.mathmaster.axiom.PlayfairAxiomSkill;
import com.freshfish.mathmaster.axiom.InvolutionSkill;
import com.freshfish.mathmaster.axiom.GeodesicSkill;
import com.freshfish.mathmaster.axiom.ReturnSkill;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UseAxiomSkillPayload(boolean pressed) implements CustomPacketPayload {
    public static final Type<UseAxiomSkillPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "use_axiom_skill")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, UseAxiomSkillPayload> STREAM_CODEC =
            StreamCodec.of(UseAxiomSkillPayload::write, UseAxiomSkillPayload::read);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, UseAxiomSkillPayload::handle);
    }

    private static void write(RegistryFriendlyByteBuf buffer, UseAxiomSkillPayload payload) {
        buffer.writeBoolean(payload.pressed);
    }

    private static UseAxiomSkillPayload read(RegistryFriendlyByteBuf buffer) {
        return new UseAxiomSkillPayload(buffer.readBoolean());
    }

    private static void handle(UseAxiomSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.pressed) {
                    useSelectedSkill(player);
                } else {
                    GeodesicSkill.release(player);
                    ReturnSkill.release(player);
                }
            }
        });
    }

    private static void useSelectedSkill(ServerPlayer player) {
        boolean bypassCooldown = AxiomCooldownHandler.beforeSkillUse(player);
        ItemStack caseStack = EquippedAxiomCase.find(player).orElse(ItemStack.EMPTY);
        if (caseStack.isEmpty()) {
            message(player, "message.mathmaster.axiom.no_case");
            return;
        }
        var equippedAxioms = AxiomCaseItem.getEquippedAxioms(caseStack);
        var activeAxioms = equippedAxioms.stream()
                .filter(equipped -> equipped.definition().isActive())
                .toList();
        if (activeAxioms.isEmpty()) {
            message(player, "message.mathmaster.axiom.no_active");
            return;
        }
        AxiomDefinition selected = AxiomCaseItem.getSelectedAxiom(caseStack)
                .filter(axiom -> activeAxioms.stream().anyMatch(equipped -> equipped.definition() == axiom))
                .orElse(activeAxioms.getFirst().definition());
        AxiomCaseItem.setSelectedAxiom(caseStack, selected);

        int level = AxiomEffectManager.getEffectiveLevel(equippedAxioms, selected);
        if (selected == AxiomDefinition.ADDITION_COMMUTATIVITY) {
            AdditionCommutativitySkill.use(player, level);
        } else if (selected == AxiomDefinition.ADDITIVE_INVERSE) {
            AdditiveInverseSkill.use(player, level);
        } else if (selected == AxiomDefinition.EUCLID_PRIME_INFINITY) {
            EuclidPrimeInfinitySkill.use(player, level);
        } else if (selected == AxiomDefinition.PARALLEL_POSTULATE) {
            PlayfairAxiomSkill.use(player, level);
        } else if (selected == AxiomDefinition.INVOLUTION) {
            InvolutionSkill.use(player, level);
        } else if (selected == AxiomDefinition.GEODESIC) {
            GeodesicSkill.press(player, level);
        } else if (selected == AxiomDefinition.PRIME_COMBO) {
            com.freshfish.mathmaster.axiom.PrimeComboSkill.use(player, level);
        } else if (selected == AxiomDefinition.RETURN) {
            ReturnSkill.press(player, level);
        } else {
            message(player, "message.mathmaster.axiom.unimplemented");
        }
        AxiomCooldownHandler.afterSkillUse(player, bypassCooldown);
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key), true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
