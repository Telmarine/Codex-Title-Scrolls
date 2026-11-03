package com.tac5studios.codextitlescrolls.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

/** config/titlescrolls-client.toml: each player's own choices. */
public final class ClientSettings {

    public static final ModConfigSpec SPEC;
    public static final BooleanValue SHOW_TITLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SHOW_TITLES = b.comment("Show other players' titles on them.").define("show_titles", true);
        SPEC = b.build();
    }

    private ClientSettings() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "titlescrolls-client.toml");
    }

    public static boolean showTitles() {
        try {
            return SHOW_TITLES.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }
}
