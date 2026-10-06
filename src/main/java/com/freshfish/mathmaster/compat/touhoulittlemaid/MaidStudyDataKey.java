package com.freshfish.mathmaster.compat.touhoulittlemaid;

import com.freshfish.mathmaster.MathMaster;
import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

final class MaidStudyDataKey implements TaskDataKey<MaidStudyData> {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "maid_study");

    @Override
    public ResourceLocation getKey() {
        return ID;
    }

    @Override
    public CompoundTag writeSaveData(MaidStudyData data) {
        return data.save();
    }

    @Override
    public MaidStudyData readSaveData(CompoundTag compound) {
        return MaidStudyData.load(compound);
    }
}
