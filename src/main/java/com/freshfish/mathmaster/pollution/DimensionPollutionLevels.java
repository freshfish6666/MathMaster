package com.freshfish.mathmaster.pollution;

import com.freshfish.mathmaster.config.MathMasterConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared dimension environment parameter; querying it never applies pollution. */
public final class DimensionPollutionLevels {
    private static List<? extends String> cachedEntries;
    private static Map<ResourceLocation, Integer> levels = Map.of();

    private DimensionPollutionLevels() {}

    public static int get(Level level) { return get(level.dimension().location()); }

    public static int get(ResourceLocation dimension) {
        var entries = MathMasterConfig.dimensionPollutionLevels();
        if (entries != cachedEntries) {
            Map<ResourceLocation,Integer> parsed = new HashMap<>();
            for (String entry : entries) {
                int split = entry.lastIndexOf('=');
                parsed.put(ResourceLocation.parse(entry.substring(0,split).trim()),
                        Integer.parseInt(entry.substring(split+1).trim()));
            }
            levels = Map.copyOf(parsed);
            cachedEntries = entries;
        }
        return levels.getOrDefault(dimension,0);
    }

    public static boolean isValidEntry(Object value) {
        if (!(value instanceof String entry)) return false;
        int split = entry.lastIndexOf('=');
        if (split <= 0) return false;
        try {
            return ResourceLocation.tryParse(entry.substring(0,split).trim()) != null
                    && Integer.parseInt(entry.substring(split+1).trim()) >= 0;
        } catch (NumberFormatException ignored) { return false; }
    }
}
