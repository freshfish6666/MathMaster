package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.axiom.InvolutionSkillCheck;
import com.freshfish.mathmaster.axiom.MathematicalInductionCheck;
import com.freshfish.mathmaster.axiom.PlayfairSkillCheck;
import com.freshfish.mathmaster.axiom.AxiomParsingCheck;
import com.freshfish.mathmaster.axiom.ShannonEntropyCheck;
import com.freshfish.mathmaster.axiom.PollutionLevelsCheck;
import com.freshfish.mathmaster.axiom.DigitalAttackPollutionCheck;
import com.freshfish.mathmaster.axiom.PollutionAdvancementsCheck;
import com.freshfish.mathmaster.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs in an isolated disposable world through tools/checks/collision-check.gradle only. */
@EventBusSubscriber(modid = "mathmaster")
public final class SevenCollisionCheck {
    @SubscribeEvent
    public static void check(ServerStartedEvent event) {
        String result;
        try {
            var level = event.getServer().overworld();
            int y = 200;
            // Supported sand floor and a two-block-high sand wall.
            for (int x = -3; x <= 5; x++) {
                for (int z = -3; z <= 5; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, y - 2, z), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(x, y - 1, z), Blocks.SAND.defaultBlockState());
                    for (int dy = 0; dy < 4; dy++) {
                        level.setBlockAndUpdate(new BlockPos(x, y + dy, z),
                                x == 0 && dy < 2 ? Blocks.SAND.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    }
                }
            }
            int checks = 0;
            var attacker = FakePlayerFactory.getMinecraft(level);
            for (int yaw = 0; yaw < 360; yaw += 15) {
                var seven = ModEntities.SEVEN.get().create(level);
                if (seven == null) throw new AssertionError("Seven failed to instantiate");
                seven.setNoAi(true);
                seven.setYHeadRot(yaw);
                seven.setPos(3, y + 1, .5);
                seven.move(MoverType.SELF, new Vec3(0, -2, 0));
                if (Math.abs(seven.getY() - y) > 1.e-6) throw new AssertionError("Sank through sand floor at yaw " + yaw);
                seven.move(MoverType.SELF, new Vec3(-3, 0, 0));
                // Turning after wall collision must not expand the box inside that wall.
                for (int turn = 0; turn < 360; turn += 15) {
                    seven.setYHeadRot(turn);
                    seven.setPos(seven.getX(), seven.getY(), seven.getZ());
                    if (!level.noCollision(seven, seven.getBoundingBox().deflate(1.e-6))) {
                        throw new AssertionError("Wall penetration after yaw " + yaw + " -> " + turn);
                    }
                    seven.move(MoverType.SELF, new Vec3(0, -.1, 0));
                    if (Math.abs(seven.getY() - y) > 1.e-6) throw new AssertionError("Sank after turn " + turn);
                    checks++;
                }
                // Real player damage path, including hurt-triggered knockback and the entity tick.
                for (int hit = 0; hit < 100; hit++) {
                    seven.setYHeadRot((hit * 37) % 360);
                    attacker.setPos(seven.getX() + 2, seven.getY(), seven.getZ());
                    seven.invulnerableTime = 0;
                    seven.setHealth(seven.getMaxHealth());
                    if (!seven.hurt(level.damageSources().playerAttack(attacker), 1F)) {
                        throw new AssertionError("Player attack failed to apply");
                    }
                    if (seven.getDeltaMovement().x >= 0) throw new AssertionError("Attack did not knock Seven toward sand");
                    seven.move(MoverType.SELF, seven.getDeltaMovement());
                    seven.setDeltaMovement(Vec3.ZERO);
                    seven.move(MoverType.SELF, new Vec3(0, -.5, 0));
                    seven.tick();
                    if (!level.noCollision(seven, seven.getBoundingBox().deflate(1.e-6))) {
                        throw new AssertionError("Knockback into sand at yaw " + yaw + ", hit " + hit);
                    }
                    if (seven.getY() < y - 1.e-6) throw new AssertionError("Knockback sank into floor");
                    checks++;
                }
                seven.discard();
            }
            int sixChecks = SixEntityCheck.run(level);
            int fiveChecks = FiveEntityCheck.run(level);
            int involutionChecks = InvolutionSkillCheck.run(level);
            int playfairChecks = PlayfairSkillCheck.run(level);
            int inductionChecks = MathematicalInductionCheck.run();
            int parsingChecks = AxiomParsingCheck.run();
            int entropyChecks = ShannonEntropyCheck.run(level);
            int primeComboChecks = com.freshfish.mathmaster.axiom.PrimeComboSkillCheck.run(level);
            int pollutionLevelChecks = PollutionLevelsCheck.run(level);
            int waterChecks = com.freshfish.mathmaster.axiom.PollutedWaterCheck.run(level);
            int attackPollutionChecks = DigitalAttackPollutionCheck.run(level);
            int pollutionAdvancementChecks = PollutionAdvancementsCheck.run(level);
            int ringChecks = MathematicalRingCheck.run(level);
            int pollutionChecks = DigitalPollutionSleepCheck.run();
            int corruptedBlockChecks = DigitallyCorruptedBlockCheck.run(level);
            int mobDecayChecks = DigitalPollutionMobDecayCheck.run(level);
            int peanoChecks = PeanoKillCheck.run(level);
            int geodesicChecks = GeodesicSkillCheck.run(level);
            int returnChecks = com.freshfish.mathmaster.axiom.ReturnSkillCheck.run(level);
            int collatzBlockChecks = CollatzTreeBlocksCheck.run(level);
            int nRitualChecks = NAltarRitualCheck.run(level);
            int nAltarChecks = NAltarCheck.run(level);
            int paintingChecks = PaintingCheck.run(level);
            int oracleChecks = com.freshfish.mathmaster.oracle.OracleOfferingCheck.run(level);
            int offeringChecks = com.freshfish.mathmaster.ritual.NAltarOfferingsCheck.run(level);
            int altarIntellectChecks = com.freshfish.mathmaster.axiom.NAltarIntellectCheck.run(level);
            int geometryHolderChecks = GeometryHolderCheck.run(level);
            int geometryAltarChecks = GeometryAltarCheck.run(level);
            int geometryConstructChecks = com.freshfish.mathmaster.axiom.GeometryConstructCheck.run(level);
            geometryHolderChecks += com.freshfish.mathmaster.axiom.GeometryHolderCombatCheck.run(level);
            geometryHolderChecks += com.freshfish.mathmaster.axiom.GeometryHolderAttackCheck.run(level);
            geometryHolderChecks += com.freshfish.mathmaster.axiom.GeometryHolderRangedCheck.run(level);
            geometryHolderChecks += com.freshfish.mathmaster.axiom.GeometryHolderSoundCheck.run(level);
            geometryHolderChecks += com.freshfish.mathmaster.axiom.GeometryHolderUltimateCheck.run(level);
            geometryHolderChecks += com.freshfish.mathmaster.axiom.GeometryHolderSkillPoolCheck.run(level);
            geometryHolderChecks += com.freshfish.mathmaster.axiom.GeometryHolderBossBarCheck.run(level);
            geometryHolderChecks += com.freshfish.mathmaster.axiom.GeometryHolderConcurrentCheck.run(level);
            int collatzWoodChecks = CollatzWoodFamilyCheck.run(level);
            int collatzGrowthChecks = CollatzTreeGrowthCheck.run(level);
            result = "PASS: " + checks + " Seven checks (576 turns, 2400 player hits with knockback on sand); "
                    + geometryHolderChecks + " Geometry Holder checks (egg, single hit box, ordinary damage, smooth floating movement, save reload); "
                    + geometryAltarChecks + " Geometry altar checks (creative entries, escrow, cancellation, reload, repeat summon, obstruction, failed join, destruction); "
                    + geometryConstructChecks + " Geometry construct checks (egg, charge, locked ray, walls, damage, movement, save, core loot); "
                    + sixChecks + " Six checks (health, difficulty damage, shield disable); "
                    + fiveChecks + " Five checks (size, standing surface, fixed position, footprint); "
                    + involutionChecks + " Involution checks (activation, costs, cleanup, lethal-pollution packet ordering); "
                    + playfairChecks + " Playfair checks (target index, shared owners, dimensions, push timing, cleanup); "
                    + inductionChecks + " Mathematical Induction checks (registration, floor, no downgrade, cap, self, category); "
                    + parsingChecks + " Axiom parsing checks (legacy fallback, invalid IDs, read-only snapshots, induction, equipment changes); "
                    + primeComboChecks + " Prime Combo checks (real primary attack, cooldown, entropy, payment, cap, interruptions, owner HUD); "
                    + entropyChecks + " Shannon Entropy checks (exact distributions, means, variance, damage boundaries, independent rolls, real hurt, registration, removal); "
                    + pollutionLevelChecks + " pollution level checks (dimensions, effect tiers, armor, refresh, external effects, saves, direct pollution isolation); "
                    + waterChecks + " polluted water checks (buckets, dimensions, contact, armor, flow, swimming); "
                    + attackPollutionChecks + " digital attack pollution checks (exact rolls, successful hits, blocking, cancellation, identity shield, unchanged kills); "
                    + pollutionAdvancementChecks + " pollution advancement checks (effects, actual deaths, cancellation, species, 32-block boundary, dimensions); "
                    + ringChecks + " Mathematical Ring/cooldown checks (slot, bypass, removal, single event settlement, expiry, all NBT keys); "
                    + pollutionChecks + " pollution sleep checks; "
                    + corruptedBlockChecks + " digitally corrupted block checks; "
                    + mobDecayChecks + " mob pollution decay checks; "
                    + peanoChecks + " Peano kill checks (7->8->9, terminal 9, non-player death, unequipped 7/8); "
                    + collatzBlockChecks + " Collatz block checks (real placement, axes, foliage persistence/waterlogging, tool loot, tags); "
                    + nRitualChecks + " N ritual checks (aura, confirmed deaths, persistence, pulse, N loot chest); "
                    + nAltarChecks + " N altar checks (two-block placement, facing, obstruction, drops, support, creative removal); "
                    + paintingChecks + " Painting checks (preset vanilla item, creative search, random exclusion, placement, saved variant); "
                    + oracleChecks + " oracle offering checks (exact percentages, both halves/hands, consumption, chat, tiers, reload); "
                    + offeringChecks + " N offering checks (fruit, pollution, desecration, cooldowns, nearest gateway guidance); "
                    + altarIntellectChecks + " N altar intellect checks (IQ tiers, nearby average, permanent player access, legacy records, free rejection); "
                    + collatzWoodChecks + " Collatz wood checks (vanilla crafting, charcoal, fuel, loot, connections, fire); "
                    + collatzGrowthChecks + " Collatz growth checks (full sequences, turns, obstacles, timing, permanent cancellation, persistence); "
                    + returnChecks + " Return checks (anchors, induction, hidden slots, snapshots, exact teleport, dimensions, cost, cancellation, ring, persistence); "
                    + geodesicChecks + " Geodesic checks (dash, hold, sweep, billing, cooldown, real movement packets, floating exemption, cleanup, ring, old saves)";
        } catch (Throwable failure) {
            result = "FAIL: " + failure;
            failure.printStackTrace();
        }
        System.out.println("SEVEN_COLLISION_CHECK " + result);
        try { Files.writeString(Path.of("collision-result.txt"), result); }
        catch (Exception failure) { throw new RuntimeException(failure); }
        finally { event.getServer().halt(false); }
    }
}
