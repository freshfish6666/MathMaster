package com.freshfish.mathmaster.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MathMasterConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENTITY_INTELLECT_ENABLED;
    private static final ModConfigSpec.DoubleValue DAMAGE_BONUS_LEAD_1_TO_20;
    private static final ModConfigSpec.DoubleValue DAMAGE_BONUS_LEAD_21_TO_40;
    private static final ModConfigSpec.DoubleValue DAMAGE_BONUS_LEAD_41_TO_60;
    private static final ModConfigSpec.DoubleValue DAMAGE_BONUS_LEAD_61_TO_80;
    private static final ModConfigSpec.DoubleValue DAMAGE_BONUS_LEAD_81_PLUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "MathMaster creature Intellect settings.",
                "数学大师的生物智识设置。Server values are authoritative and are synced to clients."
        ).push("entity_intellect");

        ENTITY_INTELLECT_ENABLED = builder
                .comment(
                        "Master switch for creature Intellect features.",
                        "生物智识总开关。Set to false to disable insight interactions, insight rewards,",
                        "creature Intellect records, and Intellect-based melee damage bonuses."
                )
                .define("enabled", true);

        builder.comment(
                "Pre-mitigation direct melee damage bonuses.",
                "直接近战伤害的减伤前增伤百分比。The bonus multiplies the current incoming damage",
                "before shields, armor, enchantment protection, and resistance are applied.",
                "Values are percentages: 15.0 means +15% damage. Range: 0.0 to 1000.0."
        ).push("damage_bonus_percent");

        DAMAGE_BONUS_LEAD_1_TO_20 = definePercent(
                builder,
                "lead_1_to_20",
                5.0,
                "Effective player IQ leads target Intellect by 1 to 20."
        );
        DAMAGE_BONUS_LEAD_21_TO_40 = definePercent(
                builder,
                "lead_21_to_40",
                15.0,
                "Effective player IQ leads target Intellect by 21 to 40."
        );
        DAMAGE_BONUS_LEAD_41_TO_60 = definePercent(
                builder,
                "lead_41_to_60",
                25.0,
                "Effective player IQ leads target Intellect by 41 to 60."
        );
        DAMAGE_BONUS_LEAD_61_TO_80 = definePercent(
                builder,
                "lead_61_to_80",
                30.0,
                "Effective player IQ leads target Intellect by 61 to 80."
        );
        DAMAGE_BONUS_LEAD_81_PLUS = definePercent(
                builder,
                "lead_81_plus",
                40.0,
                "Effective player IQ leads target Intellect by 81 or more."
        );

        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }

    private MathMasterConfig() {
    }

    public static boolean isEntityIntellectEnabled() {
        return ENTITY_INTELLECT_ENABLED.getAsBoolean();
    }

    public static float damageBonusRate(int intellectLead) {
        if (!isEntityIntellectEnabled() || intellectLead <= 0) {
            return 0.0F;
        }

        double percent;
        if (intellectLead <= 20) {
            percent = DAMAGE_BONUS_LEAD_1_TO_20.getAsDouble();
        } else if (intellectLead <= 40) {
            percent = DAMAGE_BONUS_LEAD_21_TO_40.getAsDouble();
        } else if (intellectLead <= 60) {
            percent = DAMAGE_BONUS_LEAD_41_TO_60.getAsDouble();
        } else if (intellectLead <= 80) {
            percent = DAMAGE_BONUS_LEAD_61_TO_80.getAsDouble();
        } else {
            percent = DAMAGE_BONUS_LEAD_81_PLUS.getAsDouble();
        }
        return (float) (percent / 100.0);
    }

    private static ModConfigSpec.DoubleValue definePercent(
            ModConfigSpec.Builder builder,
            String path,
            double defaultValue,
            String rangeDescription
    ) {
        return builder.comment(rangeDescription).defineInRange(path, defaultValue, 0.0, 1000.0);
    }
}
