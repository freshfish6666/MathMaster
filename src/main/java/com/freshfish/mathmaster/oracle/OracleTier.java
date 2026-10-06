package com.freshfish.mathmaster.oracle;

/** Oracle pools are independent of the altar that requests them. */
public enum OracleTier {
    LOW("low"), MEDIUM("medium"), HIGH("high");

    private final String id;

    OracleTier(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static OracleTier byId(String id) {
        for (OracleTier tier : values()) {
            if (tier.id.equals(id)) return tier;
        }
        throw new IllegalArgumentException("Unknown oracle tier: " + id);
    }
}
