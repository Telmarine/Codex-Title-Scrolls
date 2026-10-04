package com.tenko.titlescrolls.network;

import com.tenko.titlescrolls.TitleScrolls;
import com.tenko.titlescrolls.data.PlayerTitleData;
import com.tenko.titlescrolls.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TitleScrolls.MOD_ID).versioned("1");

        registrar.playToClient(
                OpenTitleScreenPayload.TYPE,
                OpenTitleScreenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.tenko.titlescrolls.client.ClientTitleHandler.openScreen(payload))
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
                            return;
                        }
                        // Only allow selecting a title the player actually owns —
                        // never trust the client's claim blindly.
                        if (data.has(payload.titleId())) {
                            serverPlayer.setData(ModAttachments.PLAYER_TITLES.get(), data.withActive(payload.titleId()));
                        }
                    }
                })
        );
    }

    private ModNetworking() {}
}
