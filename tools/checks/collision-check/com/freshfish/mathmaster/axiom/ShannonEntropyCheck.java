package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.item.StudyNoteItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.List;

public final class ShannonEntropyCheck {
    public static int run(ServerLevel level) throws Exception {
        int checks = 0;
        int[][] attack = {{24,60,16},{32,44,24},{40,28,32},{46,15,39},{50,5,45}};
        int[][] defense = {{24,64,12},{36,46,18},{48,28,24},{56,16,28},{64,4,32}};
        double previousAttackVariance = -1, previousDefenseVariance = -1;
        for (int x = 1; x <= 5; x++) {
            for (boolean outgoing : new boolean[] {true,false}) {
                int[] counts = new int[3];
                double sum = 0, squares = 0;
                for (int roll = 0; roll < 100; roll++) {
                    float value = ShannonEntropySkill.multiplier(x, outgoing, roll);
                    counts[value == .5F ? 0 : value == 1F ? 1 : 2]++;
                    sum += value; squares += value * value;
                }
                int[] expected = (outgoing ? attack : defense)[x-1];
                if (!java.util.Arrays.equals(counts, expected)) throw new AssertionError("Distribution " + x);
                double mean = sum / 100, variance = squares / 100 - mean * mean;
                if (Math.abs(mean - (outgoing ? 1 + .04 * x : 1)) > 1.e-9
                        || variance <= (outgoing ? previousAttackVariance : previousDefenseVariance)) {
                    throw new AssertionError("Mean/variance " + x);
                }
                if (outgoing) previousAttackVariance = variance; else previousDefenseVariance = variance;
                checks++;
            }
            ItemStack note = StudyNoteItem.createAxiomNote(AxiomDefinition.SHANNON_ENTROPY, x);
            if (StudyNoteItem.getAxiom(note).orElseThrow() != AxiomDefinition.SHANNON_ENTROPY
                    || StudyNoteItem.getAxiomLevel(note) != x) throw new AssertionError("Note " + x);
            checks++;
        }
        if (AxiomDefinition.SHANNON_ENTROPY.ordinal() != 11
                || AxiomDefinition.GODEL_FIRST_INCOMPLETENESS.ordinal() != 10
                || AxiomDefinition.SHANNON_ENTROPY.category() != AxiomDefinition.AxiomCategory.CHAOS
                || AxiomDefinition.SHANNON_ENTROPY.skillType() != AxiomDefinition.AxiomSkillType.PASSIVE
                || !AxiomDefinition.SHANNON_ENTROPY.acceptsMaterial(new ItemStack(ModItems.LINGXU_INGOT.get()))) {
            throw new AssertionError("Registration or legacy ordinal changed");
        }
        checks++;
        try (var first = new SkillCheckPlayer(level, "EntropyAttack");
             var second = new SkillCheckPlayer(level, "EntropyDefense")) {
            ServerPlayer attacker = first.player, target = second.player;
            // Fresh real players start with spawn protection, unlike an established combatant.
            var spawnProtection = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnProtection.setAccessible(true);
            spawnProtection.setInt(target,0);
            Entity arrow = EntityType.ARROW.create(level);
            Entity trident = EntityType.TRIDENT.create(level);
            for (var type : List.of(DamageTypes.PLAYER_ATTACK, DamageTypes.MOB_ATTACK,
                    DamageTypes.MOB_ATTACK_NO_AGGRO, DamageTypes.STING)) {
                if (!ShannonEntropySkill.isDirectDamage(source(level,type,attacker,attacker)))
                    throw new AssertionError("Rejected melee " + type);
                checks++;
            }
            for (var type : List.of(DamageTypes.ARROW, DamageTypes.TRIDENT, DamageTypes.MOB_PROJECTILE,
                    DamageTypes.FIREBALL, DamageTypes.THROWN)) {
                if (!ShannonEntropySkill.isDirectDamage(source(level,type,
                        type == DamageTypes.TRIDENT ? trident : arrow,attacker)))
                    throw new AssertionError("Rejected projectile " + type);
                checks++;
            }
            for (var type : List.of(DamageTypes.FALL, DamageTypes.DROWN, DamageTypes.IN_FIRE,
                    DamageTypes.ON_FIRE, DamageTypes.LAVA, DamageTypes.MAGIC, DamageTypes.INDIRECT_MAGIC,
                    DamageTypes.WITHER, DamageTypes.THORNS, DamageTypes.EXPLOSION,
                    DamageTypes.PLAYER_EXPLOSION, DamageTypes.GENERIC_KILL, DamageTypes.FELL_OUT_OF_WORLD)) {
                // Even attributed damage must not turn environmental/effect damage into direct hits.
                if (ShannonEntropySkill.isDirectDamage(source(level,type,attacker,attacker))
                        || ShannonEntropySkill.isDirectDamage(source(level,type,arrow,attacker)))
                    throw new AssertionError("Accepted excluded damage " + type);
                checks++;
            }
            if (ShannonEntropySkill.isDirectDamage(source(level,DamageTypes.ARROW,arrow,null)))
                throw new AssertionError("Ownerless environmental projectile accepted");
            checks++;
            var skill = new ShannonEntropySkill();
            for (int x = 1; x <= 5; x++) {
                equip(attacker,x); equip(target,x);
                for (int seed = 0; seed < 30; seed++) {
                    attacker.getRandom().setSeed(seed); target.getRandom().setSeed(seed+1000L);
                    float expected = 2 * ShannonEntropySkill.multiplier(x,true,RandomSource.create(seed).nextInt(100))
                            * ShannonEntropySkill.multiplier(x,false,RandomSource.create(seed+1000L).nextInt(100));
                    var hit = new LivingIncomingDamageEvent(target,
                            new DamageContainer(source(level,DamageTypes.MOB_ATTACK,attacker,attacker),2));
                    skill.onIncomingDamage(hit);
                    if (hit.getAmount() != expected) throw new AssertionError("Independent rolls " + x);
                    attacker.getRandom().setSeed(seed); target.getRandom().setSeed(seed+1000L);
                    target.setHealth(20); target.invulnerableTime = 0;
                    boolean applied = target.hurt(source(level,DamageTypes.MOB_ATTACK,attacker,attacker),2);
                    if (!applied
                            || Math.abs(target.getHealth() - (20-expected)) > 1.e-5) {
                        throw new AssertionError("Real hurt/registration settlement " + x + "/" + seed
                                + " applied=" + applied + " health=" + target.getHealth() + " expected=" + (20-expected));
                    }
                    checks++;
                }
            }
            equip(attacker,0); equip(target,0);
            var unchanged = new LivingIncomingDamageEvent(target,
                    new DamageContainer(source(level,DamageTypes.MOB_ATTACK,attacker,attacker),4));
            skill.onIncomingDamage(unchanged);
            if (unchanged.getAmount() != 4) throw new AssertionError("Unequipped modifier remained");
            checks++;
            equip(attacker,5); equip(target,5);
            for (float amount : new float[] {0,-1,Float.NaN,Float.POSITIVE_INFINITY}) {
                attacker.getRandom().setSeed(42); target.getRandom().setSeed(43);
                var hit = new LivingIncomingDamageEvent(target,
                        new DamageContainer(source(level,DamageTypes.MOB_ATTACK,attacker,attacker),amount));
                skill.onIncomingDamage(hit);
                if (Float.floatToIntBits(hit.getAmount()) != Float.floatToIntBits(amount)
                        || attacker.getRandom().nextInt(100) != RandomSource.create(42).nextInt(100)
                        || target.getRandom().nextInt(100) != RandomSource.create(43).nextInt(100))
                    throw new AssertionError("Invalid amount consumed randomness");
                checks++;
            }
            var canceled = new LivingIncomingDamageEvent(target,
                    new DamageContainer(source(level,DamageTypes.MOB_ATTACK,attacker,attacker),4));
            canceled.setCanceled(true); skill.onIncomingDamage(canceled);
            if (canceled.getAmount() != 4) throw new AssertionError("Canceled damage modified");
            checks++;
        }
        return checks;
    }

    private static void equip(ServerPlayer player,int level) {
        ItemStack stack = ItemStack.EMPTY;
        if (level > 0) {
            stack = new ItemStack(ModItems.AXIOM_CASE.get());
            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(
                    List.of(StudyNoteItem.createAxiomNote(AxiomDefinition.SHANNON_ENTROPY,level))));
        }
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(EquippedAxiomCase.SLOT_ID,0,stack);
    }

    private static DamageSource source(ServerLevel level, ResourceKey<DamageType> type, Entity direct, Entity owner) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),direct,owner);
    }
}
