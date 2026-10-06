package com.freshfish.mathmaster.compat.touhoulittlemaid;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.init.ModItems;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class MaidStudyTask implements IMaidTask {
    static final MaidStudyTask INSTANCE = new MaidStudyTask();
    static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "study");

    private MaidStudyTask() {
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    @Override
    public ItemStack getIcon() {
        return ModItems.ELEMENTARY_GRADE_2_MATH.get().getDefaultInstance();
    }

    @Override
    public @Nullable SoundEvent getAmbientSound(EntityMaid maid) {
        return null;
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        return List.of();
    }

    @Override
    public boolean enableLookAndRandomWalk(EntityMaid maid) {
        return false;
    }

    @Override
    public String getMaidActionSummary() {
        return "Find a lectern holding a MathMaster quiz book and answer its questions.";
    }
}
