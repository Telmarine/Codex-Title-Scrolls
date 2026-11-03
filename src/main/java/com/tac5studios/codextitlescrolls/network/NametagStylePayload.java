package com.tac5studios.codextitlescrolls.network;

import com.tac5studios.codextitlescrolls.TitleScrolls;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/** Server -> client. How titles on players look, from the server's settings. Sent on login and reload. */
public record NametagStylePayload(boolean enabled, String position, String format, float scale) implements CustomPacketPayload {

    public static final Type<NametagStylePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TitleScrolls.MOD_ID, "nametag_style"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NametagStylePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, NametagStylePayload::enabled,
            ByteBufCodecs.STRING_UTF8, NametagStylePayload::position,
            ByteBufCodecs.STRING_UTF8, NametagStylePayload::format,
            ByteBufCodecs.FLOAT, NametagStylePayload::scale,
            NametagStylePayload::new);

    public static final NametagStylePayload OFF = new NametagStylePayload(false, "above", "{title}", 1.0F);

    @Override
    @Nonnull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
