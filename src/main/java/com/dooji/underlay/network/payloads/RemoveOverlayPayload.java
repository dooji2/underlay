package com.dooji.underlay.network.payloads;

import com.dooji.underlay.Underlay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RemoveOverlayPayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RemoveOverlayPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Underlay.MOD_ID, "remove_overlay"));

    public static final StreamCodec<FriendlyByteBuf, RemoveOverlayPayload> CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            RemoveOverlayPayload::pos,
            RemoveOverlayPayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
