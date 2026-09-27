package com.tenko.titlescrolls.client;

import com.tenko.titlescrolls.network.OpenTitleScreenPayload;
import net.minecraft.client.Minecraft;

public class ClientTitleHandler {
    public static void openScreen(OpenTitleScreenPayload payload) {
        Minecraft.getInstance().setScreen(new TitleScreen(payload));
    }

    private ClientTitleHandler() {}
}
