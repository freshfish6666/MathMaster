package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.axiom.AxiomSkillData;
import com.freshfish.mathmaster.axiom.PrimeMarkData;
import com.freshfish.mathmaster.intelligence.IntelligenceData;
import com.freshfish.mathmaster.intellect.InsightResultData;
import com.freshfish.mathmaster.pollution.DigitalPollutionData;
import com.freshfish.mathmaster.quiz.QuizProgressData;
import com.freshfish.mathmaster.reward.HighestTierRewardData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MathMaster.MODID);

    public static final Supplier<AttachmentType<IntelligenceData>> INTELLIGENCE =
            ATTACHMENT_TYPES.register("intelligence",
                    () -> AttachmentType.serializable(IntelligenceData::new)
                            .copyOnDeath()
                            .sync((holder, player) -> holder == player, IntelligenceData.STREAM_CODEC)
                            .build());

    public static final Supplier<AttachmentType<HighestTierRewardData>> HIGHEST_TIER_REWARD =
            ATTACHMENT_TYPES.register("highest_tier_reward",
                    () -> AttachmentType.serializable(HighestTierRewardData::new).copyOnDeath().build());

    public static final Supplier<AttachmentType<QuizProgressData>> QUIZ_PROGRESS =
            ATTACHMENT_TYPES.register("quiz_progress",
                    () -> AttachmentType.serializable(QuizProgressData::new)
                            .copyOnDeath()
                            .sync((holder, player) -> holder == player, QuizProgressData.STREAM_CODEC)
                            .build());

    public static final Supplier<AttachmentType<InsightResultData>> INSIGHT_RESULTS =
            ATTACHMENT_TYPES.register("insight_results",
                    () -> AttachmentType.serializable(InsightResultData::new)
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<DigitalPollutionData>> DIGITAL_POLLUTION =
            ATTACHMENT_TYPES.register("digital_pollution",
                    () -> AttachmentType.serializable(DigitalPollutionData::new)
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<AxiomSkillData>> AXIOM_SKILL_DATA =
            ATTACHMENT_TYPES.register("axiom_skill_data",
                    () -> AttachmentType.serializable(AxiomSkillData::new)
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<PrimeMarkData>> PRIME_MARK =
            ATTACHMENT_TYPES.register("prime_mark",
                    () -> AttachmentType.serializable(PrimeMarkData::new)
                            .sync((holder, player) -> true, PrimeMarkData.STREAM_CODEC)
                            .build());
}
