package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.init.ModMobEffects;
import com.freshfish.mathmaster.item.StudyNoteItem;
import com.freshfish.mathmaster.pollution.DigitalPollutionManager;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Exact seeded rolls plus actual melee, blocking and canceled-damage paths. */
public final class DigitalAttackPollutionCheck {
    private static int checks;

    public static int run(ServerLevel level) throws Exception {
        checks = 0;
        Difficulty originalDifficulty = level.getDifficulty();
        try (var fixture = new SkillCheckPlayer(level, "DigitalAttacks")) {
            var player = fixture.player;
            var data = player.getData(ModAttachments.DIGITAL_POLLUTION);
            var spawnProtection = player.getClass().getDeclaredField("spawnInvulnerableTime");
            spawnProtection.setAccessible(true);
            spawnProtection.setInt(player, 0);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            long winningSeed = -1;
            for (int number = 9; number >= 6; number--) {
                float chance = switch (number) { case 9 -> .10F; case 8 -> .15F; case 7 -> .20F; default -> .25F; };
                int triggered = 0;
                for (int sample = 0; sample < 400; sample++) {
                    long seed = sample * 7919L;
                    boolean expected = RandomSource.create(seed).nextFloat() < chance;
                    if (number == 9 && expected) winningSeed = seed;
                    data.reset(); player.getRandom().setSeed(seed);
                    DigitalPollutionManager.tryAddFromAttack(player, number);
                    require(data.getValue() == (expected ? 10 - number : 0), "roll/increment " + number + "/" + sample);
                    require(player.getRandom().nextFloat() == nextFloatAfterRoll(seed), "single roll " + number);
                    if (expected) triggered++;
                }
                require(triggered > 0 && triggered < 400, "both outcomes " + number);
            }
            require(winningSeed >= 0, "winning seed");
            // Kill rolls remain the old 1% / 2%, always +1; legacy attack overload remains usable.
            for (boolean eight : new boolean[]{false, true}) {
                for (int sample = 0; sample < 100; sample++) {
                    long seed = sample * 7919L;
                    data.reset(); player.getRandom().setSeed(seed);
                    DigitalPollutionManager.tryAddFromKill(player, eight);
                    require(data.getValue() == (RandomSource.create(seed).nextFloat() < (eight ? .02F : .01F) ? 1 : 0), "kill unchanged");
                }
                data.reset(); player.getRandom().setSeed(winningSeed);
                DigitalPollutionManager.tryAddFromAttack(player, eight);
                require(data.getValue() == (eight ? 2 : 1), "legacy attack overload");
            }
            // Direct attack pollution must ignore IQ and full Lingxu protection, and grant no effect.
            player.getData(ModAttachments.INTELLIGENCE).setIq(160);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.LINGXU_HELMET.get()));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.LINGXU_CHESTPLATE.get()));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.LINGXU_LEGGINGS.get()));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.LINGXU_BOOTS.get()));
            data.reset(); player.getRandom().setSeed(winningSeed);
            DigitalPollutionManager.tryAddFromAttack(player, 6);
            require(data.getValue() == 4 && !player.hasEffect(ModMobEffects.DIGITAL_POLLUTION), "direct value only, armor/IQ independent");
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
                player.setItemSlot(slot, ItemStack.EMPTY);

            ItemStack axiomCase = new ItemStack(ModItems.AXIOM_CASE.get());
            axiomCase.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                    StudyNoteItem.createAxiomNote(AxiomDefinition.ADDITIVE_IDENTITY, 5))));
            var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
            inventory.setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, axiomCase);
            CompoundTag shield = new CompoundTag(); shield.putInt("value", 10); shield.putInt("identity_shields", 1);
            data.deserializeNBT(level.registryAccess(), shield);
            player.getRandom().setSeed(winningSeed); DigitalPollutionManager.tryAddFromAttack(player, 6);
            require(data.getValue() == 10 && data.serializeNBT(level.registryAccess()).getInt("identity_shields") == 0, "identity absorbs whole increase");
            inventory.setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, ItemStack.EMPTY);

            Mob[] mobs = {ModEntities.NINE.get().create(level), ModEntities.EIGHT.get().create(level),
                    ModEntities.SEVEN.get().create(level), ModEntities.SIX.get().create(level)};
            for (int index = 0; index < mobs.length; index++) {
                Mob mob = mobs[index]; int number = 9 - index, triggered = 0;
                require(mob != null, "mob created " + number);
                mob.setNoAi(true); mob.setPos(4, 240, 5);
                try {
                    for (int hit = 0; hit < 100; hit++) {
                        player.setPos(4, 240, 4); player.setHealth(player.getMaxHealth());
                        player.invulnerableTime = 0; data.reset();
                        player.getRandom().setSeed(hit * 7919L);
                        require(mob.doHurtTarget(player), "real hit " + number);
                        require(player.isAlive() && player.getHealth() < player.getMaxHealth(), "real health loss " + number);
                        require(data.getValue() == 0 || data.getValue() == 10 - number, "no duplicate increment " + number);
                        if (data.getValue() > 0) triggered++;
                    }
                    require(triggered > 0 && triggered < 100, "real hit both outcomes " + number);
                    Consumer<LivingIncomingDamageEvent> cancel = event -> {
                        if (event.getEntity() == player && event.getSource().getEntity() == mob) event.setCanceled(true);
                    };
                    NeoForge.EVENT_BUS.addListener(cancel);
                    try {
                        data.reset(); player.invulnerableTime = 0; player.getRandom().setSeed(winningSeed);
                        require(!mob.doHurtTarget(player) && data.getValue() == 0, "canceled hit " + number);
                    } finally { NeoForge.EVENT_BUS.unregister(cancel); }
                    player.getCooldowns().removeCooldown(Items.SHIELD);
                    player.setPos(4, 240, 4); player.setYRot(0); player.setYHeadRot(0);
                    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
                    player.startUsingItem(InteractionHand.OFF_HAND);
                    // ServerPlayer.tick only updates server bookkeeping; doTick advances item use.
                    for (int tick = 0; tick < 10; tick++) player.doTick();
                    player.setPos(4, 240, 4); player.invulnerableTime = 0; data.reset();
                    require(player.isBlocking(), "blocking fixture " + number);
                    require(!mob.doHurtTarget(player) && data.getValue() == 0, "fully blocked " + number);
                    if (number == 6) require(player.getCooldowns().isOnCooldown(Items.SHIELD), "six still disables shield");
                    player.stopUsingItem(); player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                } finally { mob.discard(); }
            }
        } finally { level.getServer().setDifficulty(originalDifficulty, true); }
        return checks;
    }

    private static float nextFloatAfterRoll(long seed) {
        var random = RandomSource.create(seed); random.nextFloat(); return random.nextFloat();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message); checks++;
    }
}
