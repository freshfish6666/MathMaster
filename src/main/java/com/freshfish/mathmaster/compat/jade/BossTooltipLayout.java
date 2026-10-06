package com.freshfish.mathmaster.compat.jade;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Bounds of the bars actually drawn in one HUD frame, in GUI pixels. */
public final class BossTooltipLayout {
    private final Set<UUID> holders = new HashSet<>();
    private int left, top, right, bottom;
    private boolean hasBounds;

    public void clear() {
        holders.clear();
        hasBounds = false;
    }

    public void addBar(int left, int top, int right, int bottom, UUID holder) {
        if (right <= left || bottom <= top) return;
        if (hasBounds) {
            this.left = Math.min(this.left, left);
            this.top = Math.min(this.top, top);
            this.right = Math.max(this.right, right);
            this.bottom = Math.max(this.bottom, bottom);
        } else {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            hasBounds = true;
        }
        if (holder != null) holders.add(holder);
    }

    public int tooltipY(UUID target, int x, int y, int width, int height) {
        if (!hasBounds || !holders.contains(target) || width <= 0 || height <= 0) return y;
        if (x >= right || x + width <= left || y >= bottom + 6 || y + height <= top) return y;
        return Math.max(y, bottom + 6);
    }
}
