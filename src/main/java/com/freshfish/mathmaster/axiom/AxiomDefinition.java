package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public enum AxiomDefinition {
    PEANO_AXIOMS("peano_axioms", AxiomCategory.ARITHMETIC, AxiomSkillType.PASSIVE, 1, 5, () -> Items.REDSTONE),
    ADDITION_COMMUTATIVITY("addition_commutativity", AxiomCategory.ARITHMETIC, AxiomSkillType.ACTIVE, 1, 5, () -> Items.REDSTONE),
    ADDITIVE_IDENTITY("additive_identity", AxiomCategory.ARITHMETIC, AxiomSkillType.PASSIVE, 1, 5, () -> Items.REDSTONE),
    ADDITIVE_INVERSE("additive_inverse", AxiomCategory.ARITHMETIC, AxiomSkillType.ACTIVE, 3, 5, () -> Items.REDSTONE),
    EUCLID_PRIME_INFINITY("euclid_prime_infinity", AxiomCategory.NUMBER_THEORY, AxiomSkillType.ACTIVE, 1, 5, () -> Items.IRON_INGOT),
    FUNDAMENTAL_THEOREM_OF_ARITHMETIC("fundamental_theorem_of_arithmetic", AxiomCategory.NUMBER_THEORY, AxiomSkillType.PASSIVE, 1, 5, () -> Items.IRON_INGOT),
    MATHEMATICAL_INDUCTION("mathematical_induction", AxiomCategory.LOGIC, AxiomSkillType.PASSIVE, 2, 4,
            () -> Items.QUARTZ),
    INVOLUTION("involution", AxiomCategory.LOGIC, AxiomSkillType.ACTIVE, 5, 5, () -> Items.QUARTZ),
    PARALLEL_POSTULATE("parallel_postulate", AxiomCategory.GEOMETRY, AxiomSkillType.ACTIVE, 1, 5, () -> Items.ENDER_PEARL),
    GEODESIC("geodesic", AxiomCategory.GEOMETRY, AxiomSkillType.ACTIVE, 2, 5, () -> Items.ENDER_PEARL),
    GODEL_FIRST_INCOMPLETENESS("godel_first_incompleteness", AxiomCategory.CHAOS, 5, ModItems.LINGXU_INGOT::get),
    SHANNON_ENTROPY("shannon_entropy", AxiomCategory.CHAOS, AxiomSkillType.PASSIVE, 1, 5,
            ModItems.LINGXU_INGOT::get),
    RETURN("return", AxiomCategory.LOGIC, AxiomSkillType.ACTIVE, 2, 5, () -> Items.QUARTZ),
    PRIME_COMBO("prime_combo", AxiomCategory.NUMBER_THEORY, AxiomSkillType.ACTIVE, 2, 5, () -> Items.IRON_INGOT);

    private final ResourceLocation id;
    private final AxiomCategory category;
    private final AxiomSkillType skillType;
    private final int requiredNoteLevel;
    private final int maximumNoteLevel;
    private final Supplier<Item> material;

    AxiomDefinition(String path, AxiomCategory category, int requiredNoteLevel, Supplier<Item> material) {
        this(path, category, AxiomSkillType.NONE, requiredNoteLevel, 5, material);
    }

    AxiomDefinition(
            String path,
            AxiomCategory category,
            AxiomSkillType skillType,
            int requiredNoteLevel,
            int maximumNoteLevel,
            Supplier<Item> material
    ) {
        this.id = ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, path);
        this.category = category;
        this.skillType = skillType;
        this.requiredNoteLevel = requiredNoteLevel;
        this.maximumNoteLevel = maximumNoteLevel;
        this.material = material;
    }

    public ResourceLocation id() {
        return this.id;
    }

    public AxiomCategory category() {
        return this.category;
    }

    public AxiomSkillType skillType() {
        return this.skillType;
    }

    public boolean isActive() {
        return this.skillType == AxiomSkillType.ACTIVE;
    }

    public int requiredNoteLevel() {
        return this.requiredNoteLevel;
    }

    public int maximumNoteLevel() {
        return this.maximumNoteLevel;
    }

    public boolean acceptsNoteLevel(int level) {
        return level >= this.requiredNoteLevel && level <= this.maximumNoteLevel;
    }

    public boolean acceptsMaterial(ItemStack stack) {
        return stack.is(this.material.get());
    }

    public ItemStack materialStack() {
        return new ItemStack(this.material.get());
    }

    public String translationKey() {
        return "axiom.mathmaster." + this.id.getPath();
    }

    public Component skillTooltip(int noteLevel) {
        int level = Math.max(this.requiredNoteLevel, Math.min(this.maximumNoteLevel, noteLevel));
        return switch (this) {
            case PEANO_AXIOMS -> Component.translatable(
                    "tooltip.mathmaster.axiom.peano",
                    level,
                    this.requiredNoteLevel,
                    this.maximumNoteLevel,
                    20 * level
            );
            case ADDITIVE_IDENTITY -> Component.translatable(
                    "tooltip.mathmaster.axiom.additive_identity",
                    level,
                    this.requiredNoteLevel,
                    this.maximumNoteLevel,
                    6 - level,
                    (level + 1) / 2
            );
            case ADDITION_COMMUTATIVITY -> Component.translatable(
                    "tooltip.mathmaster.axiom.addition_commutativity",
                    level,
                    this.requiredNoteLevel,
                    this.maximumNoteLevel,
                    30 - 5 * level,
                    level
            );
            case ADDITIVE_INVERSE -> Component.translatable(
                    "tooltip.mathmaster.axiom.additive_inverse",
                    level,
                    this.requiredNoteLevel,
                    this.maximumNoteLevel,
                    10 * level - 20,
                    90 - 10 * level,
                    2 * level
            );
            case EUCLID_PRIME_INFINITY -> Component.translatable(
                    "tooltip.mathmaster.axiom.euclid_prime_infinity",
                    level,
                    this.requiredNoteLevel,
                    this.maximumNoteLevel,
                    8 + 2 * level,
                    10 + 10 * level,
                    120 - 20 * level,
                    4 * level
            );
            case FUNDAMENTAL_THEOREM_OF_ARITHMETIC -> Component.translatable(
                    "tooltip.mathmaster.axiom.fundamental_theorem_of_arithmetic",
                    level,
                    this.requiredNoteLevel,
                    this.maximumNoteLevel,
                    20 * level,
                    level
            );
            case MATHEMATICAL_INDUCTION -> Component.translatable(
                    "tooltip.mathmaster.axiom.mathematical_induction",
                    level,
                    this.requiredNoteLevel,
                    this.maximumNoteLevel,
                    level + 1
            );
            case PARALLEL_POSTULATE -> Component.translatable(
                    "tooltip.mathmaster.axiom.playfair",
                    level,
                    this.requiredNoteLevel,
                    this.maximumNoteLevel,
                    20 + 2 * level,
                    5 + 5 * level,
                    60 - 10 * level,
                    2 * level
            );
            case GEODESIC -> Component.translatable(
                    "tooltip.mathmaster.axiom.geodesic", level, this.requiredNoteLevel,
                    this.maximumNoteLevel, 2 * level, 32 - 6 * level, level, 6 * level
            );
            case PRIME_COMBO -> Component.translatable(
                    "tooltip.mathmaster.axiom.prime_combo", level, this.requiredNoteLevel, this.maximumNoteLevel,
                    level, PrimeComboSkill.doubleChance(level), 80-10*level
            );
            case RETURN -> Component.translatable(
                    "tooltip.mathmaster.axiom.return", level, this.requiredNoteLevel, this.maximumNoteLevel,
                    level, 80 - 10 * level, 2 * level
            );
            case SHANNON_ENTROPY -> Component.translatable(
                    "tooltip.mathmaster.axiom.shannon_entropy", level, this.requiredNoteLevel,
                    this.maximumNoteLevel,
                    ShannonEntropySkill.attackHalfChance(level), ShannonEntropySkill.attackNormalChance(level),
                    ShannonEntropySkill.attackDoubleChance(level),
                    ShannonEntropySkill.defenseHalfChance(level), ShannonEntropySkill.defenseNormalChance(level),
                    ShannonEntropySkill.defenseDoubleChance(level), 100 + 4 * level
            );
            case INVOLUTION -> Component.empty()
                    .append(Component.translatable(
                            "tooltip.mathmaster.axiom.involution",
                            5,
                            5,
                            30,
                            100,
                            20
                    ))
                    .append(Component.literal("\n"))
                    .append(Component.translatable("tooltip.mathmaster.axiom.involution.warning")
                            .withStyle(ChatFormatting.RED));
            default -> Component.translatable(
                    "tooltip.mathmaster.axiom.undefined",
                    this.requiredNoteLevel,
                    this.maximumNoteLevel
            );
        };
    }

    public static Optional<AxiomDefinition> byId(ResourceLocation id) {
        return Arrays.stream(values()).filter(axiom -> axiom.id.equals(id)).findFirst();
    }

    public static boolean isMaterial(ItemStack stack) {
        return Arrays.stream(values()).anyMatch(axiom -> axiom.acceptsMaterial(stack));
    }

    public enum AxiomCategory {
        ARITHMETIC("arithmetic", 0xFFD13B35, ChatFormatting.RED),
        NUMBER_THEORY("number_theory", 0xFF9E9E9E, ChatFormatting.GRAY),
        LOGIC("logic", 0xFFF2EFE5, ChatFormatting.WHITE),
        GEOMETRY("geometry", 0xFF36B5A5, ChatFormatting.AQUA),
        CHAOS("chaos", 0xFFAC62D4, ChatFormatting.LIGHT_PURPLE);

        private final String path;
        private final int color;
        private final ChatFormatting formatting;

        AxiomCategory(String path, int color, ChatFormatting formatting) {
            this.path = path;
            this.color = color;
            this.formatting = formatting;
        }

        public String translationKey() {
            return "axiom_category.mathmaster." + this.path;
        }

        public int color() {
            return this.color;
        }

        public ChatFormatting formatting() {
            return this.formatting;
        }
    }

    public enum AxiomSkillType {
        NONE,
        ACTIVE,
        PASSIVE
    }
}
