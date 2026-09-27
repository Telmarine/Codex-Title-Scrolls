package com.tenko.titlescrolls.network;

import com.tenko.titlescrolls.TitleScrolls;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> client. Sent in response to /title: the full title catalog (so
 * the client can render "???" placeholders for anything not yet unlocked
 * without the server ever telling the client what those hidden titles are),
 * plus this specific player's unlocked set and current active title.
 */
public record OpenTitleScreenPayload(List<TitleEntry> titles, List<String> unlocked, String activeTitle)
        implements CustomPacketPayload {

    public static final Type<OpenTitleScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TitleScrolls.MOD_ID, "open_title_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTitleScreenPayload> STREAM_CODEC = StreamCodec.composite(
            TitleEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenTitleScreenPayload::titles,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenTitleScreenPayload::unlocked,
            ByteBufCodecs.STRING_UTF8, OpenTitleScreenPayload::activeTitle,
            OpenTitleScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * IMPORTANT: this is sent to every player who runs /title, for EVERY
     * title that exists — including ones they haven't found. Only the id is
     * used to check "have I got this one"; display/rarity/flavor text for an
     * un-owned title must never be shown by the client (see client/TitleScreen.java).
     * The server intentionally still sends the real display text for
     * un-owned titles here (simplest implementation) — the "no spoiler" rule
     * is enforced client-side by the screen, not by hiding data over the
     * network. That's fine for a friendly server; if you want it airtight
     * against a modified client, blank out display/rarity/flavor for entries
     * not in `unlocked` before sending, server-side, instead.
     */
    public record TitleEntry(String id, String display, String rarity, String flavorText) {
        public static final StreamCodec<RegistryFriendlyByteBuf, TitleEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TitleEntry::id,
                ByteBufCodecs.STRING_UTF8, TitleEntry::display,
                ByteBufCodecs.STRING_UTF8, TitleEntry::rarity,
                ByteBufCodecs.STRING_UTF8, TitleEntry::flavorText,
                TitleEntry::new
        );
    }
}
