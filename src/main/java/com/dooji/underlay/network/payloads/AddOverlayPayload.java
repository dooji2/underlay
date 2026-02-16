package com.dooji.underlay.network.payloads;

import com.dooji.underlay.Underlay;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AddOverlayPayload(BlockPos pos, CompoundTag stateTag) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AddOverlayPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Underlay.MOD_ID, "add_overlay"));

    public static final StreamCodec<FriendlyByteBuf, AddOverlayPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeBlockPos(payload.pos());
            buf.writeNbt(payload.stateTag());
        },
        buf -> {
            BlockPos pos = buf.readBlockPos();
            CompoundTag tag = buf.readNbt();
            return new AddOverlayPayload(pos, tag);
        }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
