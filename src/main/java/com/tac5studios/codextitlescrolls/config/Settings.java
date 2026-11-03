package com.tac5studios.codextitlescrolls.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.tac5studios.codextitlescrolls.TitleScrolls;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** config/titlescrolls/settings.toml. Reloads with /titlescroll reload. */
public final class Settings {

    public static final ModConfigSpec SPEC;

    // [nametag]
    public static final ConfigValue<String> POSITION, FORMAT;
    public static final DoubleValue SCALE;

    // [rarities]
    public static final ConfigValue<List<? extends String>> RARITIES;

    // [loot]
    public static final ConfigValue<List<? extends String>> LOOT_TABLES, LOOT_EXCLUDE_TABLES, RARITY_WEIGHTS, LOOT_EXCLUDE_TITLES;
    public static final DoubleValue LOOT_CHANCE;

    // [cleanup]
    public static final ModConfigSpec.BooleanValue REMOVE_MISSING;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Codex: Title Scrolls settings. The on/off switches are in features.toml.",
                "Reload with /titlescroll reload.");

        b.comment("A player's active title, shown on them (nametag_titles in features.toml).").push("nametag");
        POSITION = b.comment("\"above\"  = its own line over the name",
                        "\"beside\" = after the name, on the same line")
                .define("position", "above", o -> o instanceof String s && (s.equals("above") || s.equals("beside")));
        FORMAT = b.comment("Text around the title. {title} is replaced by the title, which keeps its own colors.",
                        "& color codes work, e.g. \"&7✦ {title} &7✦\" or \"&8« {title}&8 »\".")
                .define("format", "{title}", o -> o instanceof String s && s.contains("{title}"));
        SCALE = b.comment("Size of the title line compared to the name (\"above\" only).")
                .defineInRange("scale", 1.0, 0.25, 3.0);
        b.pop();

        b.comment("Rarities a title file can use (its \"rarity\" field).").push("rarities");
        RARITIES = b.comment("\"name=#RRGGBB\": the color is the slot border in the /title menu and the rarity text on a scroll.",
                        "The order here is the menu's order (first = first slot). Add a line to add a rarity, e.g. \"mythic=#00FFFF\".",
                        "A rarity that isn't listed shows gray and sorts last. To make a new rarity drop from loot, add it to [loot] rarity_weights too.")
                .defineListAllowEmpty("list", List.of("common=#55FF55", "rare=#5555FF", "epic=#AA00AA", "legendary=#FFAA00", "unique=#FF5555"),
                        () -> "", o -> o instanceof String s && s.matches("[^=]+=#[0-9A-Fa-f]{6}"));
        b.pop();

        b.comment("Title Scrolls in loot (scroll_loot in features.toml).").push("loot");
        LOOT_TABLES = b.comment("Loot tables that can hold a scroll. * matches anything, e.g. \"minecraft:chests/*\" or \"somemod:chests/tower\".")
                .defineListAllowEmpty("tables", List.of("minecraft:chests/*"), () -> "", o -> o instanceof String);
        LOOT_EXCLUDE_TABLES = b.comment("Loot tables never given a scroll, even when they match tables.")
                .defineListAllowEmpty("exclude_tables", List.of("minecraft:chests/spawn_bonus_chest"), () -> "", o -> o instanceof String);
        LOOT_CHANCE = b.comment("Chance that a matching loot table gets one scroll (0.05 = 5%).")
                .defineInRange("chance", 0.05, 0.0, 1.0);
        RARITY_WEIGHTS = b.comment("Which title the scroll is for, picked by rarity: \"rarity=weight\". Higher weight = more often.",
                        "A rarity that isn't listed (or weight 0) never drops from loot.")
                .defineListAllowEmpty("rarity_weights", List.of("common=50", "rare=25", "epic=15", "legendary=8", "unique=2"),
                        () -> "", o -> o instanceof String s && s.matches("[^=]+=\\d+"));
        LOOT_EXCLUDE_TITLES = b.comment("Title ids that never drop from loot.")
                .defineListAllowEmpty("exclude_titles", List.of(), () -> "", o -> o instanceof String);
        b.pop();

        b.comment("Titles whose file was removed from config/titlescrolls/titles.").push("cleanup");
        REMOVE_MISSING = b.comment("true = take a removed title off players who own or wear it (at login and on /titlescroll reload).",
                        "false = players keep it, and it comes back if the file is put back.",
                        "Never runs when the title folder is empty or a title file has a problem, so a bad file can't wipe titles.")
                .define("remove_missing_titles", false);
        b.pop();

        SPEC = b.build();
    }

    private Settings() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "titlescrolls/settings.toml");
    }

    /**
     * Re-reads settings.toml. NeoForge also picks up a saved file by itself; this makes sure the values
     * in use match the file right now. Values that don't pass their check are left as they are.
     */
    public static void reload() {
        if (!SPEC.isLoaded()) return;
        Path path = FMLPaths.CONFIGDIR.get().resolve("titlescrolls").resolve("settings.toml");
        if (!Files.exists(path)) return;
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(path).build()) {
            cfg.load();
            apply(SPEC.getValues(), cfg, new ArrayList<>());
        } catch (Exception e) {
            TitleScrolls.LOGGER.warn("[TitleScrolls] Could not re-read {}: {}", path, e.toString());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void apply(UnmodifiableConfig values, UnmodifiableConfig file, List<String> path) {
        for (UnmodifiableConfig.Entry entry : values.entrySet()) {
            List<String> at = new ArrayList<>(path);
            at.add(entry.getKey());
            Object v = entry.getValue();
            if (v instanceof UnmodifiableConfig section) {
                apply(section, file, at);
            } else if (v instanceof ConfigValue value) {
                Object raw = file.get(at);
                if (raw == null || raw.equals(value.get()) || !value.getSpec().test(raw)) continue;
                value.set(raw);
            }
        }
    }
}
