package com.freshfish.mathmaster.axiom;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Five persistent positions; reducing effective level hides slots without deleting them. */
public final class ReturnAnchorData {
    public static final int MAX_SLOTS = 5;
    private final Anchor[] anchors = new Anchor[MAX_SLOTS];
    private int selected = -1;

    public record Anchor(ResourceLocation dimension, double x, double y, double z) {
        public Anchor {
            if (dimension == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || Math.abs(y) > 30_000_000) {
                throw new IllegalArgumentException("Invalid return anchor");
            }
        }
    }

    public Optional<Anchor> get(int slot) {
        return slot >= 0 && slot < MAX_SLOTS ? Optional.ofNullable(anchors[slot]) : Optional.empty();
    }

    public int selected() { return selected; }

    public boolean select(int slot, int capacity) {
        if (slot < 0 || slot >= capacity || slot >= MAX_SLOTS) return false;
        selected = slot;
        return true;
    }

    public void set(int slot, Anchor anchor) {
        if (slot < 0 || slot >= MAX_SLOTS) throw new IllegalArgumentException("Invalid slot");
        anchors[slot] = anchor;
    }

    public void normalizeSelection(int capacity) {
        if (selected >= capacity) selected = -1;
    }

    public List<Optional<Anchor>> visible(int capacity) {
        return Arrays.stream(anchors).limit(Math.max(0, Math.min(MAX_SLOTS, capacity)))
                .map(Optional::ofNullable).toList();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("selected", selected);
        ListTag slots = new ListTag();
        for (int i = 0; i < MAX_SLOTS; i++) {
            Anchor anchor = anchors[i];
            if (anchor == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", i);
            entry.putString("dimension", anchor.dimension().toString());
            entry.putDouble("x", anchor.x());
            entry.putDouble("y", anchor.y());
            entry.putDouble("z", anchor.z());
            slots.add(entry);
        }
        tag.put("slots", slots);
        return tag;
    }

    public void load(CompoundTag tag) {
        Arrays.fill(anchors, null);
        selected = tag.contains("selected", Tag.TAG_ANY_NUMERIC) ? tag.getInt("selected") : -1;
        if (selected < -1 || selected >= MAX_SLOTS) selected = -1;
        ListTag slots = tag.getList("slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(MAX_SLOTS, slots.size()); i++) {
            CompoundTag entry = slots.getCompound(i);
            int slot = entry.getInt("slot");
            if (slot < 0 || slot >= MAX_SLOTS || !entry.contains("x", Tag.TAG_ANY_NUMERIC)
                    || !entry.contains("y", Tag.TAG_ANY_NUMERIC) || !entry.contains("z", Tag.TAG_ANY_NUMERIC)) continue;
            try {
                anchors[slot] = new Anchor(ResourceLocation.tryParse(entry.getString("dimension")),
                        entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"));
            } catch (IllegalArgumentException ignored) {
                // Invalid new data does not affect existing skill cooldowns or other slots.
            }
        }
    }
}
