package com.dooji.underlay.main.network.payloads;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record RemoveOverlayPayload(BlockPos pos) {

    public static void write(RemoveOverlayPayload message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
    }

    public static RemoveOverlayPayload read(FriendlyByteBuf buf) {
        return new RemoveOverlayPayload(buf.readBlockPos());
    }
}
