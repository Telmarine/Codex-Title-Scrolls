package com.tac5studios.codextitlescrolls.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.tac5studios.codextitlescrolls.TitleScrolls;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Copies title files from datapacks (data/<namespace>/titles/<id>.json, the 1.0.2 layout) into
 * config/titlescrolls/titles/, keeping every id so unlocked titles and scrolls keep working.
 * Runs by itself once, on the first start with 1.0.3, and again with /titlescroll import datapacks.
 * A title that already has a file in the config folder is left alone.
 */
public final class DatapackImport {

    public record Result(int imported, int skipped, int failed) {}

    private DatapackImport() {}

    private static Path marker() {
        return TitleStore.folder().getParent().resolve(".datapack_import_done");
    }

    /** First start with 1.0.3: import once and leave a marker so it doesn't run again. */
    public static void runOnce(MinecraftServer server) {
        if (Files.exists(marker())) return;
        Result r = run(server);
        try {
            Files.createDirectories(marker().getParent());
            Files.writeString(marker(), "Datapack titles imported on " + LocalDate.now() + ": " + r.imported()
                    + " imported, " + r.skipped() + " already there, " + r.failed() + " failed.\n"
                    + "Run /titlescroll import datapacks to import again.\n", StandardCharsets.UTF_8);
        } catch (Exception e) {
            TitleScrolls.LOGGER.warn("[TitleScrolls] Could not write {}: {}", marker(), e.toString());
        }
    }

    public static Result run(MinecraftServer server) {
        Map<ResourceLocation, Resource> found = server.getResourceManager()
                .listResources("titles", rl -> rl.getPath().endsWith(".json"));
        Set<String> used = new HashSet<>();
        int imported = 0, skipped = 0, failed = 0;
        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            ResourceLocation rl = entry.getKey();
            String path = rl.getPath().substring("titles/".length(), rl.getPath().length() - ".json".length());
            // Same ids as 1.0.2: the file name, or namespace:name for the second title with that name.
            String id = used.add(path) ? path : rl.getNamespace() + ":" + path;
            if (!TitleStore.validId(id)) {
                TitleScrolls.LOGGER.warn("[TitleScrolls] Datapack title {} not imported: its id can't be a file name.", rl);
                failed++;
                continue;
            }
            try (Reader in = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(in);
                if (TitleDefinition.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent()) {
                    TitleScrolls.LOGGER.warn("[TitleScrolls] Datapack title {} not imported: it isn't a valid title.", rl);
                    failed++;
                } else if (TitleStore.saveNew(id, json)) {
                    imported++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                TitleScrolls.LOGGER.warn("[TitleScrolls] Datapack title {} not imported: {}", rl, e.toString());
                failed++;
            }
        }
        if (imported > 0) {
            TitleScrolls.LOGGER.info("[TitleScrolls] Imported {} title(s) from datapacks into config/titlescrolls/titles. "
                    + "Titles in datapacks aren't read any more, so the datapack can be removed.", imported);
        }
        return new Result(imported, skipped, failed);
    }
}
