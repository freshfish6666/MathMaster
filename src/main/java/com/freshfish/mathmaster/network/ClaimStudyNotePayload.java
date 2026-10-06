package com.freshfish.mathmaster.network;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.menu.MathMasterGuideMenu;
import com.freshfish.mathmaster.quiz.QuizBank;
import com.freshfish.mathmaster.quiz.QuizQuestionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClaimStudyNotePayload(String bankId) implements CustomPacketPayload {
    public static final Type<ClaimStudyNotePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "claim_study_note")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimStudyNotePayload> STREAM_CODEC =
            StreamCodec.of(ClaimStudyNotePayload::write, ClaimStudyNotePayload::read);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, ClaimStudyNotePayload::handle);
    }

    private static void write(RegistryFriendlyByteBuf buffer, ClaimStudyNotePayload payload) {
        buffer.writeUtf(payload.bankId);
    }

    private static ClaimStudyNotePayload read(RegistryFriendlyByteBuf buffer) {
        return new ClaimStudyNotePayload(buffer.readUtf());
    }

    private static void handle(ClaimStudyNotePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof MathMasterGuideMenu menu)) {
                return;
            }

            QuizBank bank = QuizBank.byDataId(payload.bankId);
            if (bank == null || menu.getQuizBank() != bank) {
                return;
            }

            var noteItem = bank.studyNoteItem();
            if (player.getCooldowns().isOnCooldown(noteItem)) {
                return;
            }

            var questions = QuizQuestionManager.getQuestions(bank);
            var correctIds = player.getData(ModAttachments.QUIZ_PROGRESS).getCorrectQuestionIds(bank);
            if (questions.isEmpty() || questions.stream().anyMatch(question -> !correctIds.contains(question.id()))) {
                return;
            }

            ItemStack note = noteItem.getDefaultInstance();
            if (!player.getInventory().add(note)) {
                player.drop(note, false);
            }
            player.getCooldowns().addCooldown(noteItem, bank.difficulty() * 20);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
