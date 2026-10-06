package com.freshfish.mathmaster.compat.touhoulittlemaid;

import com.freshfish.mathmaster.MathMaster;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.neoforged.neoforge.common.NeoForge;

@LittleMaidExtension
public final class MathMasterLittleMaidExtension implements ILittleMaid {
    private static TaskDataKey<MaidStudyData> studyDataKey;

    public MathMasterLittleMaidExtension() {
        NeoForge.EVENT_BUS.register(new MaidStudyHandler());
    }

    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(MaidStudyTask.INSTANCE);
        MathMaster.LOGGER.info("Registered Touhou Little Maid study task");
    }

    @Override
    public void registerTaskData(TaskDataRegister register) {
        studyDataKey = register.register(new MaidStudyDataKey());
    }

    static TaskDataKey<MaidStudyData> getStudyDataKey() {
        return studyDataKey;
    }
}
