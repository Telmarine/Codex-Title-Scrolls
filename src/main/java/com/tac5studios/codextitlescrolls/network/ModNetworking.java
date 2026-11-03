package com.tac5studios.codextitlescrolls.network;

import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.data.PlayerTitleData;
import com.tac5studios.codextitlescrolls.registry.ModAttachments;
import com.tac5studios.codextitlescrolls.server.TitleSync;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TitleScrolls.MOD_ID).versioned("2");

        registrar.playToClient(
                OpenTitleScreenPayload.TYPE,
                OpenTitleScreenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.tac5studios.codextitlescrolls.client.ClientTitleHandler.openScreen(payload))
        );

        registrar.playToClient(
                NametagStylePayload.TYPE,
                NametagStylePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.tac5studios.codextitlescrolls.client.ClientNametags.onStyle(payload))
        );

        registrar.playToClient(
                PlayerTitlePayload.TYPE,
                PlayerTitlePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.tac5studios.codextitlescrolls.client.ClientNametags.onTitle(payload))
        );

        registrar.playToServer(
                SetActiveTitlePayload.TYPE,
                SetActiveTitlePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        PlayerTitleData data = serverPlayer.getData(ModAttachments.PLAYER_TITLES.get());
                        // Empty id = take the active title off.
                        if (payload.titleId().isEmpty()) {
                            serverPlayer.setData(ModAttachments.PLAYER_TITLES.get(), data.withActive(null));
                            TitleSync.titleChanged(serverPlayer);
                            return;
                        }
                        // Only allow selecting a title the player actually owns —
                        // never trust the client's claim blindly.
                        if (data.has(payload.titleId())) {
                            serverPlayer.setData(ModAttachments.PLAYER_TITLES.get(), data.withActive(payload.titleId()));
                            TitleSync.titleChanged(serverPlayer);
                        }
                    }
                })
        );
    }

    private ModNetworking() {}
}
