package com.tac5studios.codextitlescrolls.migrate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.data.PlayerTitleData;
import com.tac5studios.codextitlescrolls.registry.ModAttachments;
import com.tac5studios.codextitlescrolls.server.TitleSync;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Titles from a migration for players who weren't online, kept in migration/pending_players.json
 * and given at their next login.
 */
@EventBusSubscriber(modid = TitleScrolls.MOD_ID)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class PendingGrants {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private PendingGrants() {}

    private static Path file() {
        return Migration.folder().resolve("pending_players.json");
    }

    /** Gives the titles now if the player is online and returns true; otherwise keeps them for their next login. */
    static boolean give(MinecraftServer server, UUID uuid, List<String> ids, String active) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            apply(online, ids, active);
            return true;
        }
        JsonObject all = read();
        JsonObject grant = all.get(uuid.toString()) instanceof JsonObject o ? o : new JsonObject();
        JsonArray unlocked = grant.get("unlocked") instanceof JsonArray a ? a : new JsonArray();
        for (String id : ids) if (!unlocked.contains(new JsonPrimitive(id))) unlocked.add(id);
        grant.add("unlocked", unlocked);
        if (active != null && !grant.has("active")) grant.addProperty("active", active);
        all.add(uuid.toString(), grant);
        save(all);
        return false;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player) || !Files.exists(file())) return;
        JsonObject all = read();
        if (!(all.remove(player.getUUID().toString()) instanceof JsonObject grant)) return;
        List<String> ids = new ArrayList<>();
        if (grant.get("unlocked") instanceof JsonArray arr) for (JsonElement id : arr) ids.add(id.getAsString());
        String active = grant.has("active") ? grant.get("active").getAsString() : null;
        apply(player, ids, active);
        save(all);
    }

    /** Adds the titles that exist; wears the given one only if the player isn't wearing one already. */
    private static void apply(ServerPlayer player, List<String> ids, String active) {
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLES.get());
        for (String id : ids) {
            if (TitleScrolls.TITLE_REGISTRY.get(id).isPresent()) data = data.withUnlocked(id);
        }
        if (active != null && data.activeTitle().isEmpty() && data.has(active)) data = data.withActive(active);
        player.setData(ModAttachments.PLAYER_TITLES.get(), data);
        TitleSync.titleChanged(player);
        TitleScrolls.LOGGER.info("[TitleScrolls] Migrated titles given to {}.", player.getGameProfile().getName());
    }

    private static JsonObject read() {
        try {
            return Files.exists(file())
                    ? JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            TitleScrolls.LOGGER.warn("[TitleScrolls] Could not read {}: {}", file(), e.toString());
            return new JsonObject();
        }
    }

    private static void save(JsonObject all) {
        try {
            if (all.isEmpty()) {
                Files.deleteIfExists(file());
                return;
            }
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(all) + "\n", StandardCharsets.UTF_8);
        } catch (Exception e) {
            TitleScrolls.LOGGER.warn("[TitleScrolls] Could not save {}: {}", file(), e.toString());
        }
    }
}
