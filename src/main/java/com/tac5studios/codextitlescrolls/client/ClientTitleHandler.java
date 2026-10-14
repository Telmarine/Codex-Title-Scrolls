package com.tac5studios.codextitlescrolls.client;

import com.tac5studios.codextitlescrolls.network.OpenTitleScreenPayload;
import net.minecraft.client.Minecraft;

public class ClientTitleHandler {
    public static void openScreen(OpenTitleScreenPayload payload) {
        Minecraft.getInstance().setScreen(new TitleScreen(payload));
    }

    private ClientTitleHandler() {}
}
