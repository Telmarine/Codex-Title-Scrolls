package com.tac5studios.codextitlescrolls.network;

import com.tac5studios.codextitlescrolls.TitleScrolls;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Server -> client. One player's active title text ("" = none), sent to players who can see them.
 * Only the display text of a title the player wears; nothing about locked titles.
 */
public record PlayerTitlePayload(UUID player, String display) implements CustomPacketPayload {

    public static final Type<PlayerTitlePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TitleScrolls.MOD_ID, "player_title"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTitlePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PlayerTitlePayload::player,
            ByteBufCodecs.STRING_UTF8, PlayerTitlePayload::display,
            PlayerTitlePayload::new);

    @Override
    @Nonnull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
