package com.dooji.underlay.network.payloads;

import com.dooji.underlay.Underlay;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record PickItemFromOverlayPayload(BlockPos pos) implements CustomPayload {
    public static final CustomPayload.Id<PickItemFromOverlayPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(Underlay.MOD_ID, "pick_item_from_overlay"));

    public static final PacketCodec<PacketByteBuf, PickItemFromOverlayPayload> CODEC =
        PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            PickItemFromOverlayPayload::pos,
            PickItemFromOverlayPayload::new
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
