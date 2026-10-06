package com.freshfish.mathmaster.axiom;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class InverseLinkSavedData extends SavedData {
    private static final String DATA_NAME = "mathmaster_additive_inverse_links";
    private static final Factory<InverseLinkSavedData> FACTORY = new Factory<>(
            InverseLinkSavedData::new,
            InverseLinkSavedData::load
    );

    private final Set<InversePair> links = new HashSet<>();
    private final Set<UUID> pendingDeaths = new HashSet<>();

    public static InverseLinkSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public boolean isLinked(UUID entityId) {
        return this.links.stream().anyMatch(pair -> pair.contains(entityId));
    }

    public boolean link(UUID first, UUID second) {
        if (first.equals(second) || this.isLinked(first) || this.isLinked(second)) {
            return false;
        }
        this.links.add(InversePair.of(first, second));
        this.setDirty();
        return true;
    }

    public UUID breakLink(UUID entityId) {
        InversePair pair = this.links.stream()
                .filter(candidate -> candidate.contains(entityId))
                .findFirst()
                .orElse(null);
        if (pair == null) {
            return null;
        }
        this.links.remove(pair);
        this.setDirty();
        return pair.other(entityId);
    }

    public List<InversePair> links() {
        return List.copyOf(this.links);
    }

    public void addPendingDeath(UUID entityId) {
        if (this.pendingDeaths.add(entityId)) {
            this.setDirty();
        }
    }

    public boolean consumePendingDeath(UUID entityId) {
        if (!this.pendingDeaths.remove(entityId)) {
            return false;
        }
        this.setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag linkTags = new ListTag();
        for (InversePair pair : this.links) {
            CompoundTag pairTag = new CompoundTag();
            pairTag.putUUID("first", pair.first);
            pairTag.putUUID("second", pair.second);
            linkTags.add(pairTag);
        }
        tag.put("links", linkTags);

        ListTag pendingTags = new ListTag();
        for (UUID entityId : this.pendingDeaths) {
            CompoundTag pendingTag = new CompoundTag();
            pendingTag.putUUID("entity", entityId);
            pendingTags.add(pendingTag);
        }
        tag.put("pending_deaths", pendingTags);
        return tag;
    }

    private static InverseLinkSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        InverseLinkSavedData data = new InverseLinkSavedData();
        ListTag linkTags = tag.getList("links", Tag.TAG_COMPOUND);
        for (int index = 0; index < linkTags.size(); index++) {
            CompoundTag pairTag = linkTags.getCompound(index);
            if (pairTag.hasUUID("first") && pairTag.hasUUID("second")) {
                UUID first = pairTag.getUUID("first");
                UUID second = pairTag.getUUID("second");
                if (!first.equals(second)) {
                    data.links.add(InversePair.of(first, second));
                }
            }
        }
        ListTag pendingTags = tag.getList("pending_deaths", Tag.TAG_COMPOUND);
        for (int index = 0; index < pendingTags.size(); index++) {
            CompoundTag pendingTag = pendingTags.getCompound(index);
            if (pendingTag.hasUUID("entity")) {
                data.pendingDeaths.add(pendingTag.getUUID("entity"));
            }
        }
        return data;
    }

    public record InversePair(UUID first, UUID second) {
        private static InversePair of(UUID first, UUID second) {
            return first.compareTo(second) <= 0
                    ? new InversePair(first, second)
                    : new InversePair(second, first);
        }

        public boolean contains(UUID entityId) {
            return this.first.equals(entityId) || this.second.equals(entityId);
        }

        public UUID other(UUID entityId) {
            return this.first.equals(entityId) ? this.second : this.first;
        }
    }
}
