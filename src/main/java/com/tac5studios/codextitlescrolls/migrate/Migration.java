package com.tac5studios.codextitlescrolls.migrate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.data.TitleDefinition;
import com.tac5studios.codextitlescrolls.data.TitleStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Moving to Title Scrolls from another title mod, through config/titlescrolls/migration/migrate.json:
 *
 * <ol>
 *   <li>/titlescroll migrate scan: reads the other mod's files (titles in config/, saved data in the world)
 *       and fills migrate.json with what it found. A report of every file read goes in scan-report.txt.</li>
 *   <li>The owner checks migrate.json, or fills it in by hand for a mod the scan can't read.</li>
 *   <li>/titlescroll migrate apply: writes each title to titles/, gives players their titles
 *       (now if online, at their next login if not), then renames migrate.json so it can't run twice.</li>
 * </ol>
 */
public final class Migration {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private Migration() {}

    public static Path folder() {
        return FMLPaths.CONFIGDIR.get().resolve(TitleScrolls.MOD_ID).resolve("migration");
    }

    static Path migrateFile() {
        return folder().resolve("migrate.json");
    }

    /** Makes the migration folder with an empty migrate.json the first time. */
    public static void createTemplate() {
        if (Files.exists(folder())) return;
        try {
            Files.createDirectories(folder());
            write(migrateFile(), template());
        } catch (IOException e) {
            TitleScrolls.LOGGER.warn("[TitleScrolls] Could not make {}: {}", folder(), e.toString());
        }
    }

    private static JsonObject template() {
        JsonObject root = new JsonObject();
        root.addProperty("_help", "Titles and players to bring over from another title mod. /titlescroll migrate scan fills this in, "
                + "or fill it in by hand. Title fields are the same as a title file. Players are a UUID or a player name, "
                + "with the title ids they own and the one they wear. Run /titlescroll migrate apply when it looks right.");
        JsonObject titles = new JsonObject();
        JsonObject example = new JsonObject();
        example.addProperty("display", "&6Dragon Slayer");
        example.addProperty("rarity", "epic");
        example.addProperty("flavor_text", "Brought down the dragon.");
        example.addProperty("chat_color", "&e");
        titles.add("example_dragon_slayer", example);
        root.add("titles", titles);
        JsonObject players = new JsonObject();
        JsonObject grant = new JsonObject();
        JsonArray unlocked = new JsonArray();
        unlocked.add("example_dragon_slayer");
        grant.add("unlocked", unlocked);
        grant.addProperty("active", "example_dragon_slayer");
        players.add("ExamplePlayer", grant);
        root.add("players", players);
        return root;
    }

    // ---- scan ----

    public record ScanResult(int files, int titles, int players) {}

    public static ScanResult scan(MinecraftServer server) throws IOException {
        ForeignData found = new ForeignData();
        List<String> report = new ArrayList<>();
        int files = 0;

        // Title lists in config/ (any JSON with "title" in its name, outside Title Scrolls' own folder).
        Path config = FMLPaths.CONFIGDIR.get();
        Path own = config.resolve(TitleScrolls.MOD_ID);
        for (Path file : list(config, 4)) {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (file.startsWith(own) || !name.endsWith(".json") || notTitleFile(config.relativize(file).toString())) continue;
            JsonElement json = readJson(file, report, config);
            if (json == null) continue;
            files++;
            int t = found.takeTitles(json, name);
            int p = found.takePlayers(json);
            report.add(config.relativize(file) + ": " + t + " title(s), " + p + " player(s)");
        }

        // Saved data in the world: data/*.dat with "title" in the name, and each player's file.
        Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        for (Path file : list(world.resolve("data"), 1)) {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!name.endsWith(".dat") || notTitleFile(name)) continue;
            Tag tag = readNbt(file, report, world);
            if (tag == null) continue;
            files++;
            JsonElement json = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tag);
            int t = found.takeTitles(json, name);
            int p = found.takePlayers(json);
            report.add(world.relativize(file) + ": " + t + " title(s), " + p + " player(s)");
        }
        int playerFiles = 0, fromPlayerFiles = 0;
        for (Path file : list(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toAbsolutePath().normalize(), 1)) {
            String name = file.getFileName().toString();
            if (!name.endsWith(".dat")) continue;
            Tag tag = readNbt(file, report, world);
            if (!(tag instanceof CompoundTag player)) continue;
            playerFiles++;
            String uuid = name.substring(0, name.length() - ".dat".length());
            if (takeFromPlayerFile(found, uuid, player)) fromPlayerFiles++;
        }
        if (playerFiles > 0) {
            files += playerFiles;
            report.add("playerdata: " + playerFiles + " player file(s) read, titles from another mod found in " + fromPlayerFiles);
        }

        found.linkPlayerTitles();
        int[] added = mergeIntoMigrateFile(found);
        report.add("");
        report.add(added[0] + " title(s) and " + added[1] + " player(s) added to migrate.json. Check it, then run /titlescroll migrate apply.");
        write(folder().resolve("scan-report.txt"), String.join("\n", report) + "\n");
        return new ScanResult(files, added[0], added[1]);
    }

    /** True for file names that can't hold titles from another mod (no "title" or "epithet", or a title screen / main menu mod). */
    private static boolean notTitleFile(String path) {
        String p = path.toLowerCase(Locale.ROOT).replace('\\', '/');
        if (p.matches(".*(title[ _-]?screens?|main[ _-]?menu).*")) return true;
        return !p.contains("title") && !p.contains("epithet");
    }

    /** Another mod's title data saved on a player: attachments and capabilities with "title" in their name. */
    private static boolean takeFromPlayerFile(ForeignData found, String uuid, CompoundTag player) {
        boolean any = false;
        for (String holder : new String[]{"neoforge:attachments", "ForgeCaps", "NeoForgeData", "ForgeData"}) {
            if (!(player.get(holder) instanceof CompoundTag data)) continue;
            for (String key : data.getAllKeys()) {
                String k = key.toLowerCase(Locale.ROOT);
                if (k.startsWith(TitleScrolls.MOD_ID + ":") || !(k.contains("title") || k.contains("epithet"))) continue;
                Tag value = data.get(key);
                if (value == null) continue;
                JsonElement json = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, value);
                found.takeTitles(json, k);
                if (found.takePlayer(uuid, json)) any = true;
            }
        }
        return any;
    }

    /** Adds what the scan found to migrate.json, keeping what's already in it. Returns {titles added, players added}. */
    private static int[] mergeIntoMigrateFile(ForeignData found) throws IOException {
        JsonObject root = Files.exists(migrateFile()) ? readObject(migrateFile()) : template();
        JsonObject titles = child(root, "titles");
        JsonObject players = child(root, "players");
        titles.remove("example_dragon_slayer");
        players.remove("ExamplePlayer");
        int t = 0, p = 0;
        for (Map.Entry<String, JsonObject> e : found.titles.entrySet()) {
            if (!titles.has(e.getKey())) {
                titles.add(e.getKey(), e.getValue());
                t++;
            }
        }
        for (Map.Entry<String, ForeignData.Grant> e : found.players.entrySet()) {
            JsonObject grant = players.has(e.getKey()) && players.get(e.getKey()).isJsonObject()
                    ? players.getAsJsonObject(e.getKey()) : null;
            if (grant == null) {
                grant = new JsonObject();
                grant.add("unlocked", new JsonArray());
                players.add(e.getKey(), grant);
                p++;
            }
            JsonArray unlocked = grant.has("unlocked") && grant.get("unlocked").isJsonArray()
                    ? grant.getAsJsonArray("unlocked") : new JsonArray();
            for (String id : e.getValue().unlocked) {
                if (!unlocked.contains(new JsonPrimitive(id))) unlocked.add(id);
            }
            grant.add("unlocked", unlocked);
            if (!grant.has("active") && e.getValue().active != null) grant.addProperty("active", e.getValue().active);
        }
        write(migrateFile(), root);
        return new int[]{t, p};
    }

    // ---- apply ----

    public record ApplyResult(int titlesWritten, int titlesSkipped, int playersNow, int playersLater, List<String> problems) {}

    public static ApplyResult apply(MinecraftServer server) throws IOException {
        if (!Files.exists(migrateFile())) throw new IOException("There's no migration/migrate.json. Run /titlescroll migrate scan first.");
        JsonObject root = readObject(migrateFile());
        List<String> problems = new ArrayList<>();

        int written = 0, skipped = 0;
        for (Map.Entry<String, JsonElement> e : child(root, "titles").entrySet()) {
            String id = e.getKey().toLowerCase(Locale.ROOT);
            if (id.startsWith("example_")) continue;
            if (!TitleStore.validId(id)) {
                problems.add("Title \"" + e.getKey() + "\": the id can only use a-z, 0-9, _ - . (and one folder:name).");
                continue;
            }
            var parsed = TitleDefinition.CODEC.parse(JsonOps.INSTANCE, e.getValue());
            if (parsed.error().isPresent()) {
                problems.add("Title \"" + id + "\": " + parsed.error().get().message());
                continue;
            }
            if (TitleStore.saveNew(id, e.getValue())) written++;
            else skipped++;
        }
        TitleScrolls.TITLE_REGISTRY.load(server);

        int now = 0, later = 0;
        for (Map.Entry<String, JsonElement> e : child(root, "players").entrySet()) {
            if (e.getKey().equals("ExamplePlayer") || !e.getValue().isJsonObject()) continue;
            Optional<UUID> uuid = resolve(server, e.getKey());
            if (uuid.isEmpty()) {
                problems.add("Player \"" + e.getKey() + "\": not a UUID, and no player by that name has joined this server.");
                continue;
            }
            JsonObject grant = e.getValue().getAsJsonObject();
            List<String> ids = new ArrayList<>();
            if (grant.get("unlocked") instanceof JsonArray arr) {
                for (JsonElement id : arr) if (id.isJsonPrimitive()) ids.add(id.getAsString());
            }
            String active = grant.has("active") && grant.get("active").isJsonPrimitive() ? grant.get("active").getAsString() : null;
            List<String> missing = ids.stream().filter(id -> TitleScrolls.TITLE_REGISTRY.get(id).isEmpty()).toList();
            if (!missing.isEmpty()) problems.add("Player \"" + e.getKey() + "\": no title called " + String.join(", ", missing) + ".");
            if (PendingGrants.give(server, uuid.get(), ids, active)) now++;
            else later++;
        }

        Path done = folder().resolve("migrate-applied-" + LocalDateTime.now().format(STAMP) + ".json");
        Files.move(migrateFile(), done);
        if (!problems.isEmpty()) write(folder().resolve("apply-problems.txt"), String.join("\n", problems) + "\n");
        return new ApplyResult(written, skipped, now, later, problems);
    }

    private static Optional<UUID> resolve(MinecraftServer server, String key) {
        try {
            return Optional.of(UUID.fromString(key));
        } catch (IllegalArgumentException ignored) {
            GameProfileCache cache = server.getProfileCache();
            return cache == null ? Optional.empty() : cache.get(key).map(GameProfile::getId);
        }
    }

    // ---- files ----

    private static List<Path> list(Path dir, int depth) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> walk = Files.walk(dir, depth)) {
            return walk.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static JsonElement readJson(Path file, List<String> report, Path base) {
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Exception e) {
            report.add(base.relativize(file) + ": skipped, not valid JSON");
            return null;
        }
    }

    private static Tag readNbt(Path file, List<String> report, Path base) {
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            report.add(base.relativize(file) + ": skipped, couldn't be read");
            return null;
        }
    }

    private static JsonObject readObject(Path file) throws IOException {
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException(folder().getParent().getParent().relativize(file) + " isn't valid JSON: " + e.getMessage());
        }
    }

    private static JsonObject child(JsonObject root, String key) {
        if (root.get(key) instanceof JsonObject obj) return obj;
        JsonObject made = new JsonObject();
        root.add(key, made);
        return made;
    }

    private static void write(Path file, Object content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content instanceof String s ? s : GSON.toJson((JsonElement) content) + "\n", StandardCharsets.UTF_8);
    }
}
