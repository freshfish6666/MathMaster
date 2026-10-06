package com.freshfish.mathmaster.oracle;

import com.freshfish.mathmaster.MathMaster;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Data packs may supply additional collections; each valid entry has equal weight. */
public final class OracleManager extends SimpleJsonResourceReloadListener {
    public static final OracleManager INSTANCE = new OracleManager();
    private static volatile Map<OracleTier, List<OracleEntry>> pools = Map.of();

    private OracleManager() {
        super(new GsonBuilder().disableHtmlEscaping().create(), "oracles");
    }

    public static List<OracleEntry> getPool(OracleTier tier) {
        return pools.getOrDefault(tier, List.of());
    }

    public static Optional<OracleEntry> draw(OracleTier tier, RandomSource random) {
        List<OracleEntry> entries = getPool(tier);
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(random.nextInt(entries.size())));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        EnumMap<OracleTier, List<OracleEntry>> loaded = new EnumMap<>(OracleTier.class);
        for (OracleTier tier : OracleTier.values()) loaded.put(tier, new ArrayList<>());
        Set<ResourceLocation> seen = new HashSet<>();
        resources.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(resource -> {
                    try {
                        var collection = GsonHelper.convertToJsonObject(resource.getValue(), "oracle collection");
                        OracleTier tier = OracleTier.byId(GsonHelper.getAsString(collection, "tier"));
                        List<OracleEntry> entries = new ArrayList<>();
                        for (JsonElement element : GsonHelper.getAsJsonArray(collection, "entries")) {
                            var entry = GsonHelper.convertToJsonObject(element, "oracle entry");
                            ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(entry, "id"));
                            String text = GsonHelper.getAsString(entry, "text");
                            if (text.isBlank()) throw new IllegalArgumentException("Empty oracle text: " + id);
                            entries.add(new OracleEntry(id, text));
                        }
                        // Validate the whole collection before adding it to the new snapshot.
                        for (OracleEntry entry : entries) {
                            if (seen.add(entry.id())) loaded.get(tier).add(entry);
                            else MathMaster.LOGGER.warn("Ignoring duplicate oracle {} in {}", entry.id(), resource.getKey());
                        }
                    } catch (RuntimeException error) {
                        MathMaster.LOGGER.warn("Ignoring invalid oracle collection {}: {}", resource.getKey(), error.getMessage());
                    }
                });
        EnumMap<OracleTier, List<OracleEntry>> snapshot = new EnumMap<>(OracleTier.class);
        loaded.forEach((tier, entries) -> snapshot.put(tier, List.copyOf(entries)));
        pools = Map.copyOf(snapshot);
        MathMaster.LOGGER.info("Loaded oracle pools: low={}, medium={}, high={}",
                getPool(OracleTier.LOW).size(), getPool(OracleTier.MEDIUM).size(), getPool(OracleTier.HIGH).size());
    }
}
