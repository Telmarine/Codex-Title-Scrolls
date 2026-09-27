package com.tenko.titlescrolls.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.tenko.titlescrolls.TitleScrolls;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads every data/<namespace>/titles/<id>.json across all active datapacks
 * (this server's own pack, and anyone else's if they add more) into an
 * in-memory catalog. Runs on every datapack /reload, same as any vanilla
 * loot table or recipe — no custom reload command needed.
 *
 * A server owner adds a new title by dropping one more JSON file in this
 * folder. Nothing here is Tenko-specific or hardcoded.
 */
public class TitleReloadListener extends SimpleJsonResourceReloadListener {

    private Map<String, TitleDefinition> titles = Map.of();

    public TitleReloadListener() {
        super(new Gson(), "titles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, TitleDefinition> map = new LinkedHashMap<>();
        loaded.forEach((id, json) -> TitleDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> TitleScrolls.LOGGER.error("[TitleScrolls] Failed to parse title {}: {}", id, error))
                .ifPresent(def -> map.put(id.getPath(), def)));
        this.titles = Map.copyOf(map);
        TitleScrolls.LOGGER.info("[TitleScrolls] Loaded {} title definition(s).", map.size());
    }

    public Map<String, TitleDefinition> getAll() {
        return titles;
    }

    public Optional<TitleDefinition> get(String id) {
        return Optional.ofNullable(titles.get(id));
    }
}