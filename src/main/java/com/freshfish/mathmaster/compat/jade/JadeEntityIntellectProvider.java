package com.freshfish.mathmaster.compat.jade;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.api.MathMasterApi;
import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.init.ModAttachments;
import com.freshfish.mathmaster.init.ModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IToggleableProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.OptionalInt;

public enum JadeEntityIntellectProvider implements
        IServerDataProvider<EntityAccessor>, IEntityComponentProvider, IToggleableProvider {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(MathMaster.MODID, "entity_intellect");
    private static final String INTELLECT_TAG = "MathMasterIntellect";
    private static final String INSIGHTED_TAG = "MathMasterInsighted";
    private static final String PRIME_MARK_TAG = "MathMasterPrimeMark";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!MathMasterConfig.isJadeIntegrationEnabled()) {
            return;
        }

        OptionalInt intellect = MathMasterApi.getEffectiveEntityIntellect(accessor.getEntity());
        if (intellect.isEmpty()) {
            return;
        }

        data.putInt(INTELLECT_TAG, intellect.getAsInt());
        boolean insighted = accessor.getPlayer() instanceof ServerPlayer serverPlayer
                && MathMasterApi.hasSuccessfullyInsighted(serverPlayer, accessor.getEntity().getType());
        data.putBoolean(INSIGHTED_TAG, insighted);
        if (accessor.getEntity() instanceof net.minecraft.world.entity.LivingEntity livingEntity
                && livingEntity.hasEffect(ModMobEffects.PRIME_MARK)) {
            data.putInt(PRIME_MARK_TAG, livingEntity.getData(ModAttachments.PRIME_MARK).getPrime());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(INTELLECT_TAG) || !data.contains(INSIGHTED_TAG)) {
            return;
        }

        tooltip.add(Component.translatable("jade.mathmaster.entity_intellect", data.getInt(INTELLECT_TAG)));
        tooltip.add(Component.translatable(
                "jade.mathmaster.insight_status",
                Component.translatable(data.getBoolean(INSIGHTED_TAG)
                        ? "jade.mathmaster.insight_status.completed"
                        : "jade.mathmaster.insight_status.not_completed")
        ));
        if (data.contains(PRIME_MARK_TAG)) {
            tooltip.add(Component.translatable("jade.mathmaster.prime_mark", data.getInt(PRIME_MARK_TAG)));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
