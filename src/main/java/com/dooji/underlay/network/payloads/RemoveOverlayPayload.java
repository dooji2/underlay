package com.dooji.underlay.network.payloads;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record RemoveOverlayPayload(BlockPos pos) {
    public static final Identifier ID = new Identifier("underlay", "remove_overlay");

    public static void write(PacketByteBuf buf, RemoveOverlayPayload payload) {
        buf.writeBlockPos(payload.pos());
    }

    public static RemoveOverlayPayload read(PacketByteBuf buf) {
        return new RemoveOverlayPayload(buf.readBlockPos());
    }
}
