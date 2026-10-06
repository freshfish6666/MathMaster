package com.freshfish.mathmaster.entity;

import net.minecraft.util.RandomSource;

/** Per-boss, per-distance-group history; a preference, not a fixed rotation. */
public final class GeometryHolderSkillPool {
    private final int[] skills;
    private int used, last = -1;
    public GeometryHolderSkillPool(int... skills) { this.skills = skills.clone(); }
    public void reset() { used = 0; last = -1; }
    public int pick(RandomSource random) {
        if (used == (1 << skills.length) - 1) used = 0;
        int total = 0;
        for (int i=0;i<skills.length;i++) total += weight(i);
        int roll = random.nextInt(total);
        for (int i=0;i<skills.length;i++) {
            roll -= weight(i);
            if (roll < 0) { used |= 1<<i; last = i; return skills[i]; }
        }
        throw new IllegalStateException("Empty skill pool");
    }
    private int weight(int index) { return index == last ? 1 : (used & (1<<index)) == 0 ? 8 : 2; }
}
