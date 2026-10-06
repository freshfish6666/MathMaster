package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.axiom.AxiomDefinition;
import com.freshfish.mathmaster.axiom.AxiomEffectManager;
import com.freshfish.mathmaster.axiom.EquippedAxiomCase;
import com.freshfish.mathmaster.entity.NineEntity;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModEntities;
import com.freshfish.mathmaster.init.ModItems;
import com.freshfish.mathmaster.item.StudyNoteItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import top.theillusivec4.curios.api.CuriosApi;

/** Exercises real death events and the equipped Curios axiom case. */
final class PeanoKillCheck {
    private static final AABB AREA = new AABB(-2, 195, -2, 10, 210, 10);

    static int run(ServerLevel level) {
        var player = FakePlayerFactory.getMinecraft(level);
        var curios = CuriosApi.getCuriosInventory(player).orElseThrow();
        ItemStack previous = EquippedAxiomCase.find(player).orElse(ItemStack.EMPTY).copy();
        try {
            // Use active spawn chunks so entity queries can observe the spawned successor immediately.
            level.getChunkAt(new BlockPos(3, 200, 3));
            player.setPos(5, 200, 5);
            var axiomCase = new ItemStack(ModItems.AXIOM_CASE.get());
            axiomCase.set(DataComponents.CONTAINER,
                    ItemContainerContents.fromItems(List.of(StudyNoteItem.createAxiomNote(AxiomDefinition.PEANO_AXIOMS, 5))));
            curios.setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, axiomCase);
            require(AxiomEffectManager.getEquippedLevel(player, AxiomDefinition.PEANO_AXIOMS) == 5, "Peano case not equipped");

            var eight = kill(level, create(level, ModEntities.SEVEN.get()), level.damageSources().playerAttack(player), ModEntities.EIGHT.get());
            var nine = kill(level, eight, level.damageSources().playerAttack(player), ModEntities.NINE.get());
            kill(level, nine, level.damageSources().playerAttack(player), null);
            kill(level, create(level, ModEntities.SEVEN.get()), level.damageSources().generic(), null);

            curios.setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, ItemStack.EMPTY);
            kill(level, create(level, ModEntities.SEVEN.get()), level.damageSources().playerAttack(player), null);
            kill(level, create(level, ModEntities.EIGHT.get()), level.damageSources().playerAttack(player), null);
            return 6;
        } finally {
            curios.setEquippedCurio(EquippedAxiomCase.SLOT_ID, 0, previous);
            player.getData(ModAttachments.DIGITAL_POLLUTION).reset();
            level.getEntitiesOfClass(NineEntity.class, AREA, mob -> true).forEach(NineEntity::discard);
        }
    }

    private static NineEntity create(ServerLevel level, EntityType<? extends NineEntity> type) {
        var mob = type.create(level);
        require(mob != null, "Digital mob creation failed");
        mob.setNoAi(true);
        mob.moveTo(3, 200, 3, 45, 0);
        require(level.addFreshEntity(mob), "Digital mob insertion failed");
        return mob;
    }

    private static NineEntity kill(ServerLevel level, NineEntity victim, DamageSource source,
            EntityType<? extends NineEntity> expectedType) {
        var position = victim.position();
        float yaw = victim.getYRot();
        require(victim.hurt(source, 1000F) && !victim.isAlive(), "Test attack did not kill target");
        var survivors = level.getEntitiesOfClass(NineEntity.class, AREA, NineEntity::isAlive);
        require(survivors.size() == (expectedType == null ? 0 : 1), "Expected exactly one successor or none, got " + survivors.size());
        victim.discard();
        if (expectedType == null) return null;
        var successor = survivors.getFirst();
        require(successor.getType() == expectedType, "Incorrect successor type");
        require(successor.position().equals(position) && successor.getYRot() == yaw, "Successor position or rotation changed");
        successor.setNoAi(true);
        return successor;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
