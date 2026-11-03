package com.tac5studios.codextitlescrolls.config;

import com.tac5studios.codextitlescrolls.compat.OverlapGuard;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Master Control File: config/titlescrolls/features.toml. One switch per feature.
 * A feature that's switched off doesn't run. Changes here need a restart.
 */
public final class Features {

    public static final ModConfigSpec SPEC;
    private static final Map<Feature, BooleanValue> SWITCHES = new EnumMap<>(Feature.class);

    public static final BooleanValue GUARD_ENABLED, AUTO_DETECT;
    public static final ConfigValue<List<? extends String>> FORCE_ON, GUARD_MODS, IGNORE_MODS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Codex: Title Scrolls master control file. Every feature has its own switch.",
                "A feature that's switched off doesn't run. Changes here need a restart.").push("features");
        for (Feature f : Feature.values()) {
            SWITCHES.put(f, b.comment(f.comment).define(f.key, f.onByDefault));
        }
        b.pop();

        b.comment("Turns a feature off when another installed mod already does the same job.").push("overlap_guard");
        GUARD_ENABLED = b.comment("Use the overlap guard at all.").define("enabled", true);
        AUTO_DETECT = b.comment("Find mods that show titles on players by themselves (checked at start).",
                        "A mod counts when it's about titles and it draws name tags or shows text over players' heads.",
                        "Every mod found is named in the server log.")
                .define("auto_detect", true);
        GUARD_MODS = b.comment("Extra mods to guard against, \"feature=modid\", e.g. \"nametag_titles=somemod\".")
                .defineListAllowEmpty("mods", List.of(), () -> "", o -> o instanceof String);
        IGNORE_MODS = b.comment("Mods auto-detection must never count, by mod id.")
                .defineListAllowEmpty("ignore_mods", List.of("elements_nexus"), () -> "", o -> o instanceof String);
        FORCE_ON = b.comment("Features to keep on even when the guard would turn them off, e.g. [\"nametag_titles\"].")
                .defineListAllowEmpty("force_on", List.of(), () -> "", o -> o instanceof String);
        b.pop();

        SPEC = b.build();
    }

    private Features() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "titlescrolls/features.toml");
    }

    /** The switch itself, ignoring the overlap guard. Before the file is loaded, everything counts as on. */
    public static boolean switchedOn(Feature f) {
        try {
            return SWITCHES.get(f).get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** Switched on and not turned off by the overlap guard. */
    public static boolean on(Feature f) {
        return switchedOn(f) && !OverlapGuard.blocks(f);
    }
}
