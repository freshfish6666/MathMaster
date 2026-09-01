package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.intelligence.IntelligenceData;
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
                    () -> AttachmentType.serializable(IntelligenceData::new).copyOnDeath().build());

    public static final Supplier<AttachmentType<HighestTierRewardData>> HIGHEST_TIER_REWARD =
            ATTACHMENT_TYPES.register("highest_tier_reward",
                    () -> AttachmentType.serializable(HighestTierRewardData::new).copyOnDeath().build());
}
