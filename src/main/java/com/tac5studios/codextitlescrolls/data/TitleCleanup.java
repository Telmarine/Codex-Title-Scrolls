package com.tac5studios.codextitlescrolls.data;

import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.config.Settings;
import com.tac5studios.codextitlescrolls.registry.ModAttachments;
import com.tac5studios.codextitlescrolls.server.TitleSync;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * [cleanup] remove_missing_titles (off by default): takes titles whose file was removed off players,
 * at login and on /titlescroll reload. Skipped while the title folder is empty or a file has a problem.
 */
@EventBusSubscriber(modid = TitleScrolls.MOD_ID)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class TitleCleanup {

    private TitleCleanup() {}

    private static boolean active() {
        return Settings.SPEC.isLoaded() && Settings.REMOVE_MISSING.get() && TitleScrolls.TITLE_REGISTRY.healthy();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer player && active()) clean(player);
    }

    static void runOnline(MinecraftServer server) {
        if (server == null || !active()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) clean(player);
    }

    private static void clean(ServerPlayer player) {
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLES.get());
        List<String> kept = data.unlockedTitles().stream().filter(id -> TitleScrolls.TITLE_REGISTRY.get(id).isPresent()).toList();
        Optional<String> active = data.activeTitle().filter(id -> TitleScrolls.TITLE_REGISTRY.get(id).isPresent());
        if (kept.size() == data.unlockedTitles().size() && active.equals(data.activeTitle())) return;
        List<String> removed = data.unlockedTitles().stream().filter(id -> !kept.contains(id)).toList();
        player.setData(ModAttachments.PLAYER_TITLES.get(), new PlayerTitleData(new ArrayList<>(kept), active));
        if (!active.equals(data.activeTitle())) TitleSync.titleChanged(player);
        TitleScrolls.LOGGER.info("[TitleScrolls] Removed title(s) with no file from {}: {}", player.getGameProfile().getName(),
                removed.isEmpty() ? data.activeTitle().orElse("") : String.join(", ", removed));
    }
}
