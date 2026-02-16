package com.dooji.underlay.network.payloads;

import com.dooji.underlay.Underlay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PickItemFromOverlayPayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PickItemFromOverlayPayload> ID = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Underlay.MOD_ID, "pick_item_from_overlay"));

    public static final StreamCodec<FriendlyByteBuf, PickItemFromOverlayPayload> CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            PickItemFromOverlayPayload::pos,
            PickItemFromOverlayPayload::new
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
