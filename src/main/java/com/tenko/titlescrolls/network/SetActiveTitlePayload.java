package com.tenko.titlescrolls.network;

import com.tenko.titlescrolls.TitleScrolls;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server. Sent when a player clicks a collected title in the GUI. */
public record SetActiveTitlePayload(String titleId) implements CustomPacketPayload {

    public static final Type<SetActiveTitlePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TitleScrolls.MOD_ID, "set_active_title"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetActiveTitlePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetActiveTitlePayload::titleId, SetActiveTitlePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
