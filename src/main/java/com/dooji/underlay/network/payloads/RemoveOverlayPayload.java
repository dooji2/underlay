package com.dooji.underlay.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record RemoveOverlayPayload(BlockPos pos) implements CustomPayload {
    public static final CustomPayload.Id<RemoveOverlayPayload> ID = new CustomPayload.Id<>(Identifier.of("underlay", "remove_overlay"));

    public static final PacketCodec<PacketByteBuf, RemoveOverlayPayload> CODEC =
        PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            RemoveOverlayPayload::pos,
            RemoveOverlayPayload::new
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
