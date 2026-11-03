package com.tac5studios.codextitlescrolls.migrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Reads titles and players' titles out of another title mod's files without knowing their layout.
 *
 * <p>A title is any JSON object with a text field (display, name, title, text, ...) that sits under a
 * key with "title" in it or has title-like fields (rarity, description, color, ...). A player is a UUID
 * (as a key, a field, or the file name) with a list of strings and maybe an active one.
 * Player titles that match no title found are made into titles from their own text.
 *
 * <p>Everything found goes into migrate.json first, so the owner can check it before it's applied.
 */
final class ForeignData {

    static final class Grant {
        final Set<String> unlocked = new LinkedHashSet<>();
        String active;

        boolean isEmpty() {
            return unlocked.isEmpty() && active == null;
        }
    }

    private static final String[] TEXT_KEYS = {"display", "displayName", "display_name", "displayText", "title", "name", "text", "prefix", "label"};
    private static final String[] ID_KEYS = {"id", "key", "titleId", "title_id", "identifier"};
    private static final String[] RARITY_KEYS = {"rarity", "tier", "rank", "grade"};
    private static final String[] FLAVOR_KEYS = {"flavor_text", "flavorText", "flavor", "description", "desc", "lore", "tooltip", "hint"};
    private static final String[] COLOR_KEYS = {"color", "colour", "textColor", "text_color"};
    private static final String[] CHAT_COLOR_KEYS = {"chat_color", "chatColor"};
    private static final String[] DEF_KEYS = {"rarity", "tier", "description", "lore", "color", "colour", "conditions", "condition",
            "requirements", "unlock", "buffs", "stats", "chat_color", "chatColor", "flavor_text", "hidden", "icon"};
    private static final String[] LIST_KEYS = {"unlocked", "unlockedTitles", "unlocked_titles", "owned", "ownedTitles", "titles",
            "granted", "available", "collection", "epithets"};
    private static final String[] ACTIVE_KEYS = {"active", "activeTitle", "active_title", "selected", "selectedTitle", "current",
            "currentTitle", "equipped", "equippedTitle", "worn", "wearing", "title", "epithet"};

    private static final Pattern UUID_TEXT = Pattern.compile("[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}");
    private static final Pattern CODES = Pattern.compile("(?i)[&§](#[0-9a-f]{6}|[0-9a-fk-or])");

    /** id -> title in Title Scrolls' own format. */
    final Map<String, JsonObject> titles = new LinkedHashMap<>();
    /** UUID (or player name) -> titles. */
    final Map<String, Grant> players = new LinkedHashMap<>();

    // ---- titles ----

    /** Finds titles anywhere in one file. Returns how many were new. */
    int takeTitles(JsonElement root, String fileName) {
        int before = titles.size();
        walkTitles(root, fileName.toLowerCase(Locale.ROOT), null);
        return titles.size() - before;
    }

    private void walkTitles(JsonElement e, String container, String key) {
        if (e instanceof JsonObject obj) {
            if (isTitle(obj, container, key)) {
                addTitle(obj, key);
                return;
            }
            for (Map.Entry<String, JsonElement> child : obj.entrySet()) {
                walkTitles(child.getValue(), key == null ? container : key.toLowerCase(Locale.ROOT), child.getKey());
            }
        } else if (e instanceof JsonArray arr) {
            String inside = key == null ? container : key.toLowerCase(Locale.ROOT);
            for (JsonElement item : arr) walkTitles(item, inside, null);
        }
    }

    private static boolean isTitle(JsonObject obj, String container, String key) {
        if (text(obj, TEXT_KEYS) == null || hasStringList(obj)) return false;
        return container.contains("title") || container.contains("epithet") || has(obj, DEF_KEYS);
    }

    private void addTitle(JsonObject obj, String key) {
        String text = text(obj, TEXT_KEYS);
        if (text == null) return;
        String id = text(obj, ID_KEYS);
        if (id == null && key != null && !UUID_TEXT.matcher(key).matches() && !key.matches("\\d+")) id = key;
        id = toId(id == null ? text : id);
        if (id == null || titles.containsKey(id)) return;

        JsonObject out = new JsonObject();
        out.addProperty("display", withColor(codes(text), obj));
        String rarity = text(obj, RARITY_KEYS);
        if (rarity != null) out.addProperty("rarity", rarity.toLowerCase(Locale.ROOT));
        String flavor = text(obj, FLAVOR_KEYS);
        if (flavor != null) out.addProperty("flavor_text", codes(flavor));
        String chat = text(obj, CHAT_COLOR_KEYS);
        if (chat != null && colorCode(chat) != null) out.addProperty("chat_color", colorCode(chat));
        titles.put(id, out);
    }

    /** Adds a color field ("gold", "#FFD700", "&6") to the front of text that has no color of its own. */
    private static String withColor(String text, JsonObject obj) {
        if (CODES.matcher(text).find()) return text;
        String color = text(obj, COLOR_KEYS);
        String code = color == null ? null : colorCode(color);
        return code == null ? text : code + text;
    }

    private static String colorCode(String color) {
        String c = color.trim();
        if (c.matches("(?i)[&§](#[0-9a-f]{6}|[0-9a-f])")) return "&" + c.substring(1);
        if (c.matches("(?i)#[0-9a-f]{6}")) return "&" + c.toUpperCase(Locale.ROOT);
        if (c.matches("\\d+")) return String.format("&#%06X", Integer.parseInt(c) & 0xFFFFFF);
        ChatFormatting named = ChatFormatting.getByName(c.toLowerCase(Locale.ROOT).replace(' ', '_'));
        return named != null && named.isColor() ? "&" + named.getChar() : null;
    }

    // ---- players ----

    /** Finds players' titles anywhere in one file. Returns how many players were found. */
    int takePlayers(JsonElement root) {
        int before = players.size();
        walkPlayers(root, null);
        return players.size() - before;
    }

    private void walkPlayers(JsonElement e, String key) {
        if (key != null && UUID_TEXT.matcher(key).matches()) {
            takePlayer(key, e);
            return;
        }
        if (e instanceof JsonObject obj) {
            String uuid = uuidField(obj);
            if (uuid != null && takePlayer(uuid, obj)) return;
            for (Map.Entry<String, JsonElement> child : obj.entrySet()) walkPlayers(child.getValue(), child.getKey());
        } else if (e instanceof JsonArray arr) {
            for (JsonElement item : arr) walkPlayers(item, null);
        }
    }

    /** One player's titles, from a file or attachment that's known to belong to them. */
    boolean takePlayer(String uuid, JsonElement e) {
        Grant g = new Grant();
        readGrant(e, g, 0);
        if (g.isEmpty()) return false;
        Grant into = players.computeIfAbsent(normalUuid(uuid), k -> new Grant());
        into.unlocked.addAll(g.unlocked);
        if (into.active == null) into.active = g.active;
        return true;
    }

    private static void readGrant(JsonElement e, Grant g, int depth) {
        if (depth > 3) return;
        if (e instanceof JsonArray arr) {
            for (JsonElement item : arr) {
                String s = item instanceof JsonObject o ? text(o, ID_KEYS) != null ? text(o, ID_KEYS) : text(o, TEXT_KEYS) : string(item);
                if (s != null && !s.isBlank()) g.unlocked.add(s);
            }
        } else if (e instanceof JsonObject obj) {
            boolean listFound = false;
            for (String k : LIST_KEYS) {
                JsonElement v = obj.get(k);
                if (v instanceof JsonArray) {
                    readGrant(v, g, depth + 1);
                    listFound = true;
                }
            }
            if (!listFound) {
                for (Map.Entry<String, JsonElement> child : obj.entrySet()) {
                    if (child.getValue() instanceof JsonArray arr && isStringList(arr)) readGrant(arr, g, depth + 1);
                }
            }
            String active = text(obj, ACTIVE_KEYS);
            if (active != null && !active.isBlank() && g.active == null) g.active = active;
            for (Map.Entry<String, JsonElement> child : obj.entrySet()) {
                if (child.getValue() instanceof JsonObject inner && !UUID_TEXT.matcher(child.getKey()).matches()) {
                    readGrant(inner, g, depth + 1);
                }
            }
        }
    }

    /**
     * Player titles that match no title found: the title with the same text is used,
     * or a new title is made from the text itself. Ids in grants are changed to match.
     */
    void linkPlayerTitles() {
        Map<String, String> byText = new LinkedHashMap<>();
        titles.forEach((id, def) -> byText.putIfAbsent(plain(def.get("display").getAsString()), id));
        for (Grant g : players.values()) {
            List<String> ids = new ArrayList<>();
            for (String s : g.unlocked) {
                String id = linkOne(s, byText);
                if (id != null) ids.add(id);
            }
            g.unlocked.clear();
            g.unlocked.addAll(ids);
            g.active = g.active == null ? null : linkOne(g.active, byText);
            if (g.active != null) g.unlocked.add(g.active);
        }
        players.values().removeIf(Grant::isEmpty);
    }

    private String linkOne(String s, Map<String, String> byText) {
        String t = s.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("#none") || t.equalsIgnoreCase("none")) return null;
        if (titles.containsKey(t)) return t;
        String low = t.toLowerCase(Locale.ROOT);
        if (titles.containsKey(low)) return low;
        int colon = low.indexOf(':');
        if (colon > 0 && titles.containsKey(low.substring(colon + 1))) return low.substring(colon + 1);
        String match = byText.get(plain(t));
        if (match != null) return match;
        // A title known only by its text: make it a title of its own.
        String display = t.equalsIgnoreCase("#newbie") ? "&aNewcomer" : codes(t.startsWith("#") ? t.substring(1) : t);
        String id = toId(display);
        if (id == null) return null;
        if (!titles.containsKey(id)) {
            JsonObject out = new JsonObject();
            out.addProperty("display", display);
            titles.put(id, out);
            byText.put(plain(display), id);
        }
        return id;
    }

    // ---- helpers ----

    /** A file name safe id from any text: "&6Dragon Slayer!" -> "dragon_slayer". */
    static String toId(String text) {
        if (text == null) return null;
        String s = plain(text).toLowerCase(Locale.ROOT);
        int colon = s.indexOf(':');
        if (colon > 0 && s.substring(0, colon).matches("[a-z0-9_.-]+")) s = s.substring(colon + 1); // drop a mod namespace
        s = s.replaceAll("[^a-z0-9_.-]+", "_").replaceAll("_+", "_").replaceAll("^[_.-]+|[_.-]+$", "");
        if (s.length() > 48) s = s.substring(0, 48);
        return s.isEmpty() ? null : s;
    }

    static String plain(String text) {
        return CODES.matcher(text).replaceAll("").trim();
    }

    /** § codes become & codes, the way Title Scrolls writes them. */
    static String codes(String text) {
        return text.replace('§', '&');
    }

    private static String normalUuid(String s) {
        String hex = s.replace("-", "");
        try {
            return UUID.fromString(hex.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5")).toString();
        } catch (IllegalArgumentException e) {
            return s;
        }
    }

    private static String uuidField(JsonObject obj) {
        for (String k : new String[]{"uuid", "UUID", "player", "playerUuid", "player_uuid", "owner"}) {
            String v = string(obj.get(k));
            if (v != null && UUID_TEXT.matcher(v).matches()) return v;
            // NBT int-array UUIDs come through as four numbers.
            if (obj.get(k) instanceof JsonArray arr && arr.size() == 4) {
                try {
                    long most = ((long) arr.get(0).getAsInt() << 32) | (arr.get(1).getAsInt() & 0xFFFFFFFFL);
                    long least = ((long) arr.get(2).getAsInt() << 32) | (arr.get(3).getAsInt() & 0xFFFFFFFFL);
                    return new UUID(most, least).toString();
                } catch (RuntimeException ignored) {
                    // not a UUID
                }
            }
        }
        return null;
    }

    private static boolean has(JsonObject obj, String[] keys) {
        for (String k : keys) if (obj.has(k)) return true;
        return false;
    }

    private static boolean hasStringList(JsonObject obj) {
        for (String k : LIST_KEYS) {
            if (obj.get(k) instanceof JsonArray arr && !arr.isEmpty() && isStringList(arr)) return true;
        }
        return false;
    }

    private static boolean isStringList(JsonArray arr) {
        for (JsonElement item : arr) if (string(item) == null) return false;
        return !arr.isEmpty();
    }

    /** The first of these fields that holds text (or a text component's "text"). */
    private static String text(JsonObject obj, String[] keys) {
        for (String k : keys) {
            JsonElement v = obj.get(k);
            String s = string(v);
            if (s == null && v instanceof JsonObject comp) s = string(comp.get("text"));
            if (s != null && !s.isBlank()) return s;
        }
        return null;
    }

    private static String string(JsonElement e) {
        return e instanceof JsonPrimitive p && p.isString() ? p.getAsString() : null;
    }
}
