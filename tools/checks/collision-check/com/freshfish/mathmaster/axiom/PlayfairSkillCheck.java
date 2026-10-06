package com.freshfish.mathmaster.axiom;

import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.item.StudyNoteItem;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Exercises real targeting/movement and every removal path of the derived target index. */
public final class PlayfairSkillCheck {
    private static final PlayfairAxiomSkill HANDLER = new PlayfairAxiomSkill();

    public static int run(ServerLevel level) throws Exception {
        var cow = EntityType.COW.create(level);
        var other = EntityType.COW.create(level);
        if (cow == null || other == null) throw new AssertionError("Playfair targets missing");
        cow.setNoAi(true); other.setNoAi(true);
        cow.setPos(4, 240, 9); other.setPos(14, 240, 9);
        level.addFreshEntity(cow); level.addFreshEntity(other);
        try (var first = new SkillCheckPlayer(level, "PlayfairOne");
             var second = new SkillCheckPlayer(level, "PlayfairTwo");
             var third = new SkillCheckPlayer(level, "PlayfairThree")) {
            int checks = 0;
            second.player.setPos(6, 240, 4);
            third.player.setPos(14, 240, 4);
            start(first.player, cow, new Vec3(4, 240, 4));
            start(second.player, cow, new Vec3(6, 240, 4));
            start(third.player, other, new Vec3(14, 240, 4));
            require(index().size() == 2 && index().values().stream().mapToInt(Map::size).sum() == 3,
                    "shared and separate targets were not indexed"); checks++;
            require(first.player.getData(ModAttachments.AXIOM_SKILL_DATA).getPlayfairCooldownTicks() == 200
                    && first.player.getData(ModAttachments.DIGITAL_POLLUTION).getValue() == 10,
                    "level-five cost/cooldown changed"); checks++;

            // Pending selection must not push the target. Unrelated entities have no matching bucket.
            cow.setPos(4, 240, 5);
            Vec3 pendingPosition = cow.position();
            HANDLER.onTargetTick(new EntityTickEvent.Post(cow));
            require(cow.position().equals(pendingPosition), "pending axis pushed target"); checks++;
            first.player.setPos(4, 240, 6);
            cow.setPos(4, 240, 7);
            HANDLER.onPlayerTick(new PlayerTickEvent.Post(first.player));
            require(Math.abs(cow.getZ() - 7.6) < 1.e-6, "player-tick push timing/distance changed"); checks++;
            cow.setPos(4, 240, 7);
            cow.setTarget(first.player);
            HANDLER.onTargetTick(new EntityTickEvent.Post(cow));
            require(Math.abs(cow.getZ() - 7.6) < 1.e-6 && cow.getTarget() == null,
                    "indexed target tick failed to push/clear attack target"); checks++;
            Vec3 otherPosition = other.position();
            HANDLER.onTargetTick(new EntityTickEvent.Post(other));
            require(other.position().equals(otherPosition), "different target used another owner's effect"); checks++;
            var unrelated = EntityType.COW.create(level);
            unrelated.setPos(4, 240, 7);
            HANDLER.onTargetTick(new EntityTickEvent.Post(unrelated));
            require(unrelated.getZ() == 7, "unrelated creature pushed"); checks++;

            HANDLER.onEntityLeaveLevel(new EntityLeaveLevelEvent(cow, level.getServer().getLevel(Level.NETHER)));
            require(effects().size() == 3, "wrong dimension removed target owners"); checks++;
            PlayfairAxiomSkill.cancel(first.player, false);
            require(effects().size() == 2 && index().size() == 2,
                    "cancel removed other owners or retained empty bucket"); checks++;
            HANDLER.onPlayerLogout(new PlayerEvent.PlayerLoggedOutEvent(second.player));
            require(effects().size() == 1 && index().size() == 1, "logout left shared-target index"); checks++;
            HANDLER.onPlayerChangedDimension(new PlayerEvent.PlayerChangedDimensionEvent(
                    third.player, Level.OVERWORLD, Level.NETHER));
            require(effects().isEmpty() && index().isEmpty(), "dimension change left index"); checks++;

            cow.setPos(4, 240, 9);
            start(first.player, cow, new Vec3(4, 240, 4));
            var effect = effects().get(first.player.getUUID());
            Field ticks = effect.getClass().getDeclaredField("remainingTicks");
            ticks.setAccessible(true); ticks.setInt(effect, 1);
            HANDLER.onPlayerTick(new PlayerTickEvent.Post(first.player));
            require(effects().isEmpty() && index().isEmpty(), "expiry left target index"); checks++;
            start(first.player, cow, new Vec3(4, 240, 4));
            CuriosApi.getCuriosInventory(first.player).orElseThrow()
                    .setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, ItemStack.EMPTY);
            HANDLER.onPlayerTick(new PlayerTickEvent.Post(first.player));
            require(effects().isEmpty() && index().isEmpty(), "unequip left target index"); checks++;
            start(first.player, cow, new Vec3(4, 240, 4));
            first.player.setPos(40, 240, 4);
            HANDLER.onPlayerTick(new PlayerTickEvent.Post(first.player));
            require(effects().isEmpty() && index().isEmpty(), "out-of-range removal left index"); checks++;
            start(first.player, cow, new Vec3(4, 240, 4));
            first.player.setPos(4, 240, 6);
            HANDLER.onPlayerTick(new PlayerTickEvent.Post(first.player));
            first.player.setPos(6, 240, 6);
            HANDLER.onPlayerTick(new PlayerTickEvent.Post(first.player));
            require(effects().isEmpty() && index().isEmpty(), "axis deviation left index"); checks++;

            start(first.player, cow, new Vec3(4, 240, 4));
            start(second.player, cow, new Vec3(6, 240, 4));
            cow.discard(); // Actual leave-level event must remove all owners, including pending axes.
            require(effects().isEmpty() && index().isEmpty(), "target unload left shared owners"); checks++;
            return checks;
        } finally {
            // A failed assertion must also leave the other regression checks with clean runtime state.
            for (UUID owner : List.copyOf(effects().keySet())) {
                var player = level.getServer().getPlayerList().getPlayer(owner);
                if (player != null) PlayfairAxiomSkill.cancel(player, false);
            }
            cow.discard(); other.discard();
        }
    }

    private static void start(ServerPlayer player, Mob target, Vec3 origin) throws Exception {
        PlayfairAxiomSkill.cancel(player, false);
        player.getData(ModAttachments.AXIOM_SKILL_DATA).setPlayfairCooldownTicks(0);
        player.getData(ModAttachments.DIGITAL_POLLUTION).reset();
        var stack = new ItemStack(ModItems.AXIOM_CASE.get());
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                StudyNoteItem.createAxiomNote(AxiomDefinition.PARALLEL_POSTULATE, 5))));
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, stack);
        player.setPos(origin);
        Vec3 aim = target.getEyePosition().subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-aim.x, aim.z)));
        player.setXRot((float) -Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
        // ProjectileUtil uses the previous-tick view vector; keep fixture rotations synchronized.
        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();
        player.setYHeadRot(player.getYRot());
        player.yHeadRotO = player.getYRot();
        PlayfairAxiomSkill.use(player, 5);
        if (!effects().containsKey(player.getUUID())) {
            var finder = PlayfairAxiomSkill.class.getDeclaredMethod("findTarget", ServerPlayer.class, double.class);
            finder.setAccessible(true);
            throw new AssertionError("Playfair targeting failed for " + player.getGameProfile().getName()
                    + ": actual=" + finder.invoke(null, player, 30.0) + ", expected=" + target
                    + ", view=" + player.getViewVector(0));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ?> effects() throws Exception {
        Field field = PlayfairAxiomSkill.class.getDeclaredField("ACTIVE_EFFECTS");
        field.setAccessible(true);
        return (Map<UUID, ?>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<?, Map<UUID, ?>> index() throws Exception {
        Field field = PlayfairAxiomSkill.class.getDeclaredField("EFFECTS_BY_TARGET");
        field.setAccessible(true);
        return (Map<?, Map<UUID, ?>>) field.get(null);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("Playfair: " + message);
    }
}
