package com.tac5studios.codextitlescrolls.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rarities from [rarities] list in settings.toml: "name=#RRGGBB". The color is the slot border in the /title menu
 * and the rarity text on a scroll; the order is the menu's sort order. A rarity that isn't listed is gray and sorts last.
 */
public final class Rarities {

    public static final int UNLISTED = 0xAAAAAA;

    private Rarities() {}

    /** Rarity name (lower case) -> RGB color, in list order. Bad entries are skipped. */
    public static Map<String, Integer> parse(List<? extends String> entries) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String entry : entries) {
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            String color = entry.substring(eq + 1).trim();
            if (!color.matches("#[0-9A-Fa-f]{6}")) continue;
            out.putIfAbsent(entry.substring(0, eq).trim().toLowerCase(Locale.ROOT), Integer.parseInt(color.substring(1), 16));
        }
        return out;
    }

    /** The server's rarities right now. */
    public static Map<String, Integer> current() {
        return parse(Settings.SPEC.isLoaded() ? Settings.RARITIES.get() : Settings.RARITIES.getDefault());
    }

    public static int color(Map<String, Integer> rarities, String rarity) {
        return rarities.getOrDefault(rarity.toLowerCase(Locale.ROOT), UNLISTED);
    }
}
