package com.tac5studios.codextitlescrolls.server;

import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.config.Feature;
import com.tac5studios.codextitlescrolls.config.Features;
import com.tac5studios.codextitlescrolls.config.Settings;
import com.tac5studios.codextitlescrolls.data.TitleDefinition;
import com.tac5studios.codextitlescrolls.network.NametagStylePayload;
import com.tac5studios.codextitlescrolls.network.PlayerTitlePayload;
import com.tac5studios.codextitlescrolls.registry.ModAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends each player's active title to the players who can see them, so their clients can draw it.
 * Sent when a player comes into view, when they change title, and on reload.
 */
@EventBusSubscriber(modid = TitleScrolls.MOD_ID)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class TitleSync {

    private TitleSync() {}

    public static NametagStylePayload style() {
        if (!Features.on(Feature.NAMETAG_TITLES)) return NametagStylePayload.OFF;
        return new NametagStylePayload(true, Settings.POSITION.get(), Settings.FORMAT.get(), Settings.SCALE.get().floatValue());
    }

    private static PlayerTitlePayload titleOf(ServerPlayer player) {
        String display = Features.on(Feature.NAMETAG_TITLES)
                ? player.getData(ModAttachments.PLAYER_TITLES.get()).activeTitle()
                        .flatMap(TitleScrolls.TITLE_REGISTRY::get).map(TitleDefinition::display).orElse("")
                : "";
        return new PlayerTitlePayload(player.getUUID(), display);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer player) PacketDistributor.sendToPlayer(player, style());
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof ServerPlayer target && e.getEntity() instanceof ServerPlayer viewer
                && Features.on(Feature.NAMETAG_TITLES)) {
            PacketDistributor.sendToPlayer(viewer, titleOf(target));
        }
    }

    /** A player put on, changed or took off a title. */
    public static void titleChanged(ServerPlayer player) {
        if (Features.on(Feature.NAMETAG_TITLES)) PacketDistributor.sendToPlayersTrackingEntity(player, titleOf(player));
    }

    /** Settings or titles reloaded: send the style and every visible title again. */
    public static void resendAll(MinecraftServer server) {
        if (server == null) return;
        NametagStylePayload style = style();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, style);
            if (style.enabled()) PacketDistributor.sendToPlayersTrackingEntity(player, titleOf(player));
        }
    }
}
