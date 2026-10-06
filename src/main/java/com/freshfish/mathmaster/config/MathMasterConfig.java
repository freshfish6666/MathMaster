package com.freshfish.mathmaster.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import com.freshfish.mathmaster.pollution.DimensionPollutionLevels;
import java.util.List;

public final class MathMasterConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue N_ALTAR_OFFERINGS;

    private static final ModConfigSpec.BooleanValue ENTITY_INTELLECT_ENABLED;
    private static final ModConfigSpec.DoubleValue DAMAGE_BONUS_LEAD_1_TO_20;
    private static final ModConfigSpec.DoubleValue DAMAGE_BONUS_LEAD_21_TO_40;
    private static final ModConfigSpec.DoubleValue DAMAGE_BONUS_LEAD_41_TO_60;
    private static final ModConfigSpec.DoubleValue DAMAGE_BONUS_LEAD_61_TO_80;
    private static final ModConfigSpec.DoubleValue DAMAGE_BONUS_LEAD_81_PLUS;
    private static final ModConfigSpec.BooleanValue JADE_INTEGRATION_ENABLED;
    private static final ModConfigSpec.BooleanValue EXTERNAL_ENTITY_INTELLECT_ENABLED;
    private static final ModConfigSpec.BooleanValue TOUHOU_LITTLE_MAID_INTEGRATION_ENABLED;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSION_POLLUTION_LEVELS;

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

        builder.comment(
                "Optional compatibility features.",
                "可选联动功能。Disabling an integration does not delete saved player data."
        ).push("integrations");

        JADE_INTEGRATION_ENABLED = builder
                .comment(
                        "Show creature Intellect and the viewing player's insight status in Jade.",
                        "在 Jade 信息框中显示生物智识与当前查看玩家的洞悉状态。"
                )
                .define("jade", true);

        EXTERNAL_ENTITY_INTELLECT_ENABLED = builder
                .comment(
                        "Load entity_intellect JSON entries targeting non-Minecraft entity namespaces.",
                        "加载目标为其他模组命名空间的 entity_intellect JSON。",
                        "Set to false to keep vanilla creature Intellect while disabling all external creature definitions.",
                        "设为 false 时保留原版生物智识，但禁用所有其他模组生物的智识定义。"
                )
                .define("external_entity_intellect", true);

        TOUHOU_LITTLE_MAID_INTEGRATION_ENABLED = builder
                .comment(
                        "Allow Touhou Little Maid maids to use MathMaster lecterns for autonomous quizzes.",
                        "允许车万女仆在工作模式中使用放有数学书的讲台自主答题。"
                )
                .define("touhou_little_maid", true);

        builder.pop();
        builder.comment("Environmental pollution levels. Unlisted dimensions have level 0.",
                "维度污染等级；未定义维度为0，目前仅被污染方块使用。技能费用等直接污染值不受影响。")
                .push("digital_pollution");
        DIMENSION_POLLUTION_LEVELS = builder.defineListAllowEmpty("dimension_levels",
                List.of("minecraft:overworld=1", "minecraft:the_nether=2", "minecraft:the_end=3"),
                () -> "minecraft:overworld=1", DimensionPollutionLevels::isValidEntry);
        builder.pop();
        builder.push("n_altar");
        N_ALTAR_OFFERINGS = builder.comment("Confirmed monster deaths required for one N blood-sacrifice reward.",
                "N祭坛每轮血祭需要的怪物死亡数；默认20。")
                .defineInRange("required_offerings",20,1,4096);
        builder.pop();
        SPEC = builder.build();
    }

    public static int nAltarOfferings() { return N_ALTAR_OFFERINGS.get(); }

    private MathMasterConfig() {
    }

    public static List<? extends String> dimensionPollutionLevels() {
        return DIMENSION_POLLUTION_LEVELS.get();
    }

    public static boolean isEntityIntellectEnabled() {
        return ENTITY_INTELLECT_ENABLED.getAsBoolean();
    }

    public static boolean isJadeIntegrationEnabled() {
        return JADE_INTEGRATION_ENABLED.getAsBoolean();
    }

    public static boolean isExternalEntityIntellectEnabled() {
        return EXTERNAL_ENTITY_INTELLECT_ENABLED.getAsBoolean();
    }

    public static boolean isTouhouLittleMaidIntegrationEnabled() {
        return TOUHOU_LITTLE_MAID_INTEGRATION_ENABLED.getAsBoolean();
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
