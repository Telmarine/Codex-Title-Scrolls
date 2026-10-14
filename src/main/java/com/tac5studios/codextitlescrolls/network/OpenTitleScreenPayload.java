package com.tac5studios.codextitlescrolls.network;

import com.tac5studios.codextitlescrolls.TitleScrolls;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

import java.util.List;

/**
 * Server -> client. Sent in response to /title: every title id (locked ones without their
 * text), the player's unlocked titles and their active title.
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
    @Nonnull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * One slot in the menu. Every title is sent so the menu can draw a "?" slot for the ones
     * the player hasn't found, but for those the server leaves display, rarity and flavor
     * text empty (see TitleCommand), so nothing can be read ahead of time.
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
