package com.tenko.titlescrolls.integration;

import com.tenko.titlescrolls.TitleScrolls;
import com.tenko.titlescrolls.data.TitleDefinition;
import com.tenko.titlescrolls.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;

/**
 * OPTIONAL bridge to NeoEssentials' Placeholder API. Only ever referenced
 * from one guarded call in TitleScrolls#commonSetup(), which checks
 * ModList.get().isLoaded("neoessentials") first.
 *
 * Registers {active_title}, resolving to the calling player's active title
 * display text (or "" if none picked). Add it to NeoEssentials' own
 * chat-format config, e.g.:
 *   "{prefix}{active_title} <{player}>: {message}"
 *
 * Renamed from {tenko_title} (27 Sep 2026) ahead of publishing this mod
 * publicly — a generic placeholder name makes sense for any server owner
 * installing this, not just Tenko's own. Tenko's own chat.json was updated
 * to match in the same pass.
 */
public class NeoEssentialsIntegration {

    public static void register() {
        com.zerog.neoessentials.api.PlaceholderAPI.registerPlaceholder("active_title",
                (player, params) -> resolve(player));
        TitleScrolls.LOGGER.info("[TitleScrolls] Registered {{active_title}} placeholder with NeoEssentials.");
    }

    private static String resolve(ServerPlayer player) {
        var data = player.getData(ModAttachments.PLAYER_TITLES.get());
        return data.activeTitle()
                .flatMap(TitleScrolls.TITLE_REGISTRY::get)
                .map(TitleDefinition::display)
                .orElse("");
    }

    private NeoEssentialsIntegration() {}
}