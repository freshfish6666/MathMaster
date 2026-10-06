package com.freshfish.mathmaster.api;

import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.intellect.EntityIntellectDefinition;
import com.freshfish.mathmaster.intellect.EntityIntellectManager;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.freshfish.mathmaster.quiz.QuizBank;
import com.freshfish.mathmaster.quiz.QuizLauncher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Stable integration surface for MathMaster.
 *
 * <p>Consumers should use this class instead of MathMaster's internal managers,
 * attachments, menus, or network payloads. Methods in API version 1 will retain
 * their meaning and binary signature throughout MathMaster 1.x.</p>
 */
public final class MathMasterApi {
    /** Current major version of the public API contract. */
    public static final int API_VERSION = 1;

    private MathMasterApi() {
    }

    /** Returns whether the server-authoritative creature Intellect system is enabled. */
    public static boolean isEntityIntellectEnabled() {
        return MathMasterConfig.isEntityIntellectEnabled();
    }

    /** Returns configured base Intellect without temporary effects such as Flow. */
    public static OptionalInt getBaseEntityIntellect(EntityType<?> entityType) {
        return asOptional(EntityIntellectManager.get(entityType));
    }

    /** Returns current effective entity Intellect, including Flow when applicable. */
    public static OptionalInt getEffectiveEntityIntellect(Entity entity) {
        return asOptional(EntityIntellectManager.get(entity));
    }

    /** Returns current effective player Intellect, including Flow and equipment bonuses, capped at 200. */
    public static int getEffectivePlayerIntellect(Player player) {
        return IntelligenceManager.getEffectiveIq(player);
    }

    /** Returns whether this player has fully and successfully insighted the entity type. */
    public static boolean hasSuccessfullyInsighted(ServerPlayer player, EntityType<?> entityType) {
        return hasSuccessfullyInsighted(player, EntityType.getKey(entityType));
    }

    /** Server-authoritative variant accepting an entity registry ID. */
    public static boolean hasSuccessfullyInsighted(ServerPlayer player, ResourceLocation entityTypeId) {
        return player.getData(ModAttachments.INSIGHT_RESULTS)
                .hasSuccessfullyInsighted(entityTypeId);
    }

    /** Returns an immutable snapshot of all successfully insighted entity type IDs. */
    public static Set<ResourceLocation> getSuccessfullyInsightedEntityTypes(ServerPlayer player) {
        return player.getData(ModAttachments.INSIGHT_RESULTS)
                .getSuccessfullyInsightEntityTypes();
    }

    /**
     * Returns an immutable, server-authoritative snapshot of books whose quiz
     * banks currently contain at least one valid question.
     */
    public static List<QuizBook> getAvailableQuizBooks() {
        return java.util.Arrays.stream(QuizBank.values())
                .filter(QuizBank::hasQuestions)
                .sorted(Comparator.comparingInt(QuizBank::difficulty))
                .map(bank -> new QuizBook(
                        BuiltInRegistries.ITEM.getKey(bank.bookItem()),
                        bank.bookItem().getDescriptionId(),
                        bank.difficulty()
                ))
                .toList();
    }

    /**
     * Requests that the logical server start a quiz from the selected book.
     * The book ID and current question-bank availability are validated before
     * the existing server-owned quiz menu is opened.
     *
     * @return true when a quiz was opened; false for an unknown or empty book
     */
    public static boolean startQuiz(ServerPlayer player, ResourceLocation bookId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(bookId, "bookId");
        return QuizLauncher.openRemoteQuiz(player, bookId);
    }

    /** Immutable public description of one available mathematical book. */
    public record QuizBook(ResourceLocation id, String translationKey, int difficulty) {
        public QuizBook {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(translationKey, "translationKey");
        }
    }

    private static OptionalInt asOptional(EntityIntellectDefinition definition) {
        return definition == null ? OptionalInt.empty() : OptionalInt.of(definition.intellect());
    }
}
