package com.tac5studios.codextitlescrolls.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.server.TitleSync;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Every title, one JSON file each, in config/titlescrolls/titles/.
 *
 * <p>A file directly in the folder is known by its file name: titles/kitsune.json is "kitsune".
 * A file one folder down is known as folder:name: titles/starter/knight.json is "starter:knight"
 * (only needed when two titles share a name). Deeper folders are skipped.
 *
 * <p>Read at server start and on /titlescroll reload. Vanilla /reload doesn't touch titles.
 */
public final class TitleStore {

    private Map<String, TitleDefinition> titles = Map.of();
    private boolean healthy;

    public static Path folder() {
        return FMLPaths.CONFIGDIR.get().resolve(TitleScrolls.MOD_ID).resolve("titles");
    }

    /** The id a title file is known by, or null for a file that isn't a title. */
    static String idOf(Path root, Path file) {
        Path rel = root.relativize(file);
        String name = rel.getFileName().toString();
        if (!name.endsWith(".json")) return null;
        name = name.substring(0, name.length() - ".json".length());
        if (name.isEmpty()) return null;
        return switch (rel.getNameCount()) {
            case 1 -> name;
            case 2 -> rel.getName(0) + ":" + name;
            default -> null;
        };
    }

    /** Where the title with this id is saved. */
    public static Path fileFor(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? folder().resolve(id + ".json")
                : folder().resolve(id.substring(0, colon)).resolve(id.substring(colon + 1) + ".json");
    }

    /** True when the id can be saved as a file (letters, digits, _ - . and at most one folder:name). */
    public static boolean validId(String id) {
        return id.matches("([a-z0-9_.-]+:)?[a-z0-9_.-]+") && !id.startsWith(".") && !id.contains(":.");
    }

    /** Saves one title file unless one with that id is already there. Returns true when it was written. */
    public static boolean saveNew(String id, JsonElement json) throws IOException {
        Path file = fileFor(id);
        if (Files.exists(file)) return false;
        Files.createDirectories(file.getParent());
        Files.writeString(file, PRETTY.toJson(json) + "\n", StandardCharsets.UTF_8);
        return true;
    }

    private static final Gson PRETTY = new GsonBuilder()
            .setPrettyPrinting().disableHtmlEscaping().create();

    /** Reads every title file again, then sends titles on players to clients again. */
    public void load(MinecraftServer server) {
        Path root = folder();
        Map<String, TitleDefinition> map = new LinkedHashMap<>();
        boolean ok = true;
        try {
            Files.createDirectories(root);
            List<Path> files = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(root, 3)) {
                walk.filter(Files::isRegularFile).sorted().forEach(files::add);
            }
            for (Path file : files) {
                String id = idOf(root, file);
                if (id == null) {
                    if (file.toString().endsWith(".json")) {
                        TitleScrolls.LOGGER.warn("[TitleScrolls] Skipped {}: title files go in titles/ or one folder down.", root.relativize(file));
                    }
                    continue;
                }
                Optional<TitleDefinition> def = read(file);
                if (def.isPresent()) map.put(id, def.get());
                else ok = false;
            }
        } catch (IOException e) {
            TitleScrolls.LOGGER.error("[TitleScrolls] Could not read {}: {}", root, e.toString());
            ok = false;
        }
        titles = Collections.unmodifiableMap(map); // keeps file order
        healthy = ok && !map.isEmpty();
        TitleScrolls.LOGGER.info("[TitleScrolls] Loaded {} title(s) from config/titlescrolls/titles.", map.size());
        TitleSync.resendAll(server);
        TitleCleanup.runOnline(server);
    }

    /** True when titles loaded with no file problems and at least one title, so removing missing titles is safe. */
    public boolean healthy() {
        return healthy;
    }

    private static Optional<TitleDefinition> read(Path file) {
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(in);
            return TitleDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> TitleScrolls.LOGGER.error("[TitleScrolls] Title file {} has a problem: {}",
                            folder().relativize(file), error));
        } catch (Exception e) {
            TitleScrolls.LOGGER.error("[TitleScrolls] Title file {} isn't valid JSON: {}", folder().relativize(file), e.getMessage());
            return Optional.empty();
        }
    }

    public Map<String, TitleDefinition> getAll() {
        return titles;
    }

    public Optional<TitleDefinition> get(String id) {
        return Optional.ofNullable(titles.get(id));
    }
}
