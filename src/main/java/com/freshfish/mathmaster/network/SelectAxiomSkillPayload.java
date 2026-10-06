package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.AdditionCommutativitySkill;
import com.freshfish.mathmaster.axiom.AdditiveInverseSkill;
import com.freshfish.mathmaster.axiom.EquippedAxiomCase;
import com.freshfish.mathmaster.axiom.PlayfairAxiomSkill;
import com.freshfish.mathmaster.axiom.GeodesicSkill;
import com.freshfish.mathmaster.item.AxiomCaseItem;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectAxiomSkillPayload(String axiomId) implements CustomPacketPayload {
    public static final Type<SelectAxiomSkillPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "select_axiom_skill")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectAxiomSkillPayload> STREAM_CODEC =
            StreamCodec.of(SelectAxiomSkillPayload::write, SelectAxiomSkillPayload::read);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, SelectAxiomSkillPayload::handle);
    }

    private static void write(RegistryFriendlyByteBuf buffer, SelectAxiomSkillPayload payload) {
        buffer.writeUtf(payload.axiomId);
    }

    private static SelectAxiomSkillPayload read(RegistryFriendlyByteBuf buffer) {
        return new SelectAxiomSkillPayload(buffer.readUtf());
    }

    private static void handle(SelectAxiomSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            AxiomDefinition axiom;
            try {
                axiom = AxiomDefinition.byId(ResourceLocation.parse(payload.axiomId)).orElse(null);
            } catch (IllegalArgumentException | ResourceLocationException ignored) {
                return;
            }
            if (axiom == null || !axiom.isActive()) {
                return;
            }
            EquippedAxiomCase.find(player).ifPresent(caseStack -> {
                if (AxiomCaseItem.getAxioms(caseStack).contains(axiom)) {
                    if (axiom != AxiomDefinition.ADDITION_COMMUTATIVITY) {
                        AdditionCommutativitySkill.cancel(player, false);
                    }
                    if (axiom != AxiomDefinition.ADDITIVE_INVERSE) {
                        AdditiveInverseSkill.cancel(player, false);
                    }
                    if (axiom != AxiomDefinition.PARALLEL_POSTULATE) {
                        PlayfairAxiomSkill.cancel(player, false);
                    }
                    if (axiom != AxiomDefinition.GEODESIC) {
                        GeodesicSkill.cancel(player);
                    }
                    AxiomCaseItem.setSelectedAxiom(caseStack, axiom);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.mathmaster.axiom.selected",
                                    net.minecraft.network.chat.Component.translatable(axiom.translationKey())
                                            .withStyle(axiom.category().formatting())
                            ),
                            true
                    );
                }
            });
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
