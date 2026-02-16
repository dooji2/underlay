package com.dooji.underlay.network.payloads;

import com.dooji.underlay.Underlay;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class RequestOverlaySyncPayload implements CustomPayload {
    public static final CustomPayload.Id<RequestOverlaySyncPayload> ID =
        new CustomPayload.Id<>(Identifier.of(Underlay.MOD_ID, "request_overlay_sync"));

    public static final PacketCodec<PacketByteBuf, RequestOverlaySyncPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {},
        buf -> new RequestOverlaySyncPayload()
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
