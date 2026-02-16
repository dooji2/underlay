package com.dooji.underlay.network.payloads;

import com.dooji.underlay.Underlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class RequestOverlaySyncPayload implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestOverlaySyncPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Underlay.MOD_ID, "request_overlay_sync"));

    public static final StreamCodec<FriendlyByteBuf, RequestOverlaySyncPayload> CODEC = StreamCodec.unit(new RequestOverlaySyncPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
