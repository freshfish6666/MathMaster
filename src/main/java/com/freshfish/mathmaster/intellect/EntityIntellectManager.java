package com.freshfish.mathmaster.intellect;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.config.MathMasterConfig;
import com.freshfish.mathmaster.intelligence.IntelligenceData;
import com.freshfish.mathmaster.intelligence.IntelligenceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class EntityIntellectManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final EntityIntellectManager INSTANCE = new EntityIntellectManager();

    private static volatile Map<ResourceLocation, EntityIntellectDefinition> definitions = builtinDefinitions();

    private EntityIntellectManager() {
        super(GSON, "entity_intellect");
    }

    public static EntityIntellectDefinition get(Entity entity) {
        EntityIntellectDefinition definition = get(entity.getType());
        if (definition == null || !(entity instanceof LivingEntity livingEntity)) {
            return definition;
        }

        int flowBonus = IntelligenceManager.getFlowIntellectBonus(livingEntity);
        if (flowBonus <= 0 || definition.intellect() >= IntelligenceData.MAX_IQ) {
            return definition;
        }

        return new EntityIntellectDefinition(Math.min(
                definition.intellect() + flowBonus,
                IntelligenceData.MAX_IQ
        ));
    }

    public static EntityIntellectDefinition get(EntityType<?> entityType) {
        if (!MathMasterConfig.isEntityIntellectEnabled()) {
            return null;
        }
        return definitions.get(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<ResourceLocation, EntityIntellectDefinition> loaded = new HashMap<>(builtinDefinitions());

        resources.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> loadDefinition(entry.getKey(), entry.getValue(), loaded));

        definitions = Map.copyOf(loaded);
        MathMaster.LOGGER.info("Loaded intellect definitions for {} entity types", definitions.size());
    }

    private static void loadDefinition(
            ResourceLocation resourceId,
            JsonElement element,
            Map<ResourceLocation, EntityIntellectDefinition> loaded
    ) {
        try {
            JsonObject object = GsonHelper.convertToJsonObject(element, resourceId.toString());
            ResourceLocation entityId = ResourceLocation.tryParse(GsonHelper.getAsString(object, "entity"));
            if (entityId == null) {
                throw new IllegalArgumentException("invalid entity id");
            }

            if (!GsonHelper.getAsBoolean(object, "enabled", true)) {
                loaded.remove(entityId);
                return;
            }

            int intellect = GsonHelper.getAsInt(object, "intellect");
            loaded.put(entityId, new EntityIntellectDefinition(intellect));
        } catch (RuntimeException exception) {
            MathMaster.LOGGER.error(
                    "Could not load entity intellect definition {}: {}",
                    resourceId,
                    exception.getMessage()
            );
        }
    }

    private static Map<ResourceLocation, EntityIntellectDefinition> builtinDefinitions() {
        Map<ResourceLocation, EntityIntellectDefinition> defaults = new HashMap<>();
        addBuiltin(defaults, "cat", 10);

        addBuiltin(defaults, "zombie", 30);
        addBuiltin(defaults, "drowned", 30);
        addBuiltin(defaults, "husk", 30);
        addBuiltin(defaults, "chicken", 30);
        addBuiltin(defaults, "mooshroom", 30);
        addBuiltin(defaults, "horse", 30);
        addBuiltin(defaults, "donkey", 30);
        addBuiltin(defaults, "llama", 30);
        addBuiltin(defaults, "cow", 30);
        addBuiltin(defaults, "sheep", 30);
        addBuiltin(defaults, "squid", 30);
        addBuiltin(defaults, "glow_squid", 30);

        addBuiltin(defaults, "skeleton", 50);
        addBuiltin(defaults, "stray", 50);
        addBuiltin(defaults, "bogged", 50);
        addBuiltin(defaults, "spider", 50);
        addBuiltin(defaults, "cave_spider", 50);
        addBuiltin(defaults, "piglin_brute", 50);
        addBuiltin(defaults, "zombie_villager", 50);
        addBuiltin(defaults, "zombified_piglin", 50);

        addBuiltin(defaults, "creeper", 60);
        addBuiltin(defaults, "guardian", 60);
        addBuiltin(defaults, "iron_golem", 60);
        addBuiltin(defaults, "witch", 60);
        addBuiltin(defaults, "wither_skeleton", 60);
        addBuiltin(defaults, "fox", 60);
        addBuiltin(defaults, "parrot", 60);
        addBuiltin(defaults, "sniffer", 60);
        addBuiltin(defaults, "strider", 60);
        addBuiltin(defaults, "wolf", 60);

        addBuiltin(defaults, "blaze", 80);
        addBuiltin(defaults, "breeze", 80);
        addBuiltin(defaults, "enderman", 80);
        addBuiltin(defaults, "piglin", 80);
        addBuiltin(defaults, "pillager", 80);
        addBuiltin(defaults, "vindicator", 80);
        addBuiltin(defaults, "pig", 80);
        addBuiltin(defaults, "dolphin", 80);
        addBuiltin(defaults, "panda", 80);

        addBuiltin(defaults, "villager", 100);
        addBuiltin(defaults, "bat", 100);
        addBuiltin(defaults, "elder_guardian", 100);
        addBuiltin(defaults, "evoker", 100);

        addBuiltin(defaults, "warden", 150);
        addBuiltin(defaults, "ender_dragon", 160);
        addBuiltin(defaults, "wither", 160);
        return Map.copyOf(defaults);
    }

    private static void addBuiltin(
            Map<ResourceLocation, EntityIntellectDefinition> definitions,
            String path,
            int intellect
    ) {
        definitions.put(
                ResourceLocation.withDefaultNamespace(path),
                new EntityIntellectDefinition(intellect)
        );
    }
}
