package com.dooji.underlay.network.payloads;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record AddOverlayPayload(BlockPos pos, NbtCompound stateTag) implements CustomPayload {
    public static final CustomPayload.Id<AddOverlayPayload> ID = new CustomPayload.Id<>(Identifier.of("underlay", "add_overlay"));

    public static final PacketCodec<PacketByteBuf, AddOverlayPayload> CODEC = PacketCodec.ofStatic(
        (buf, payload) -> {
            buf.writeBlockPos(payload.pos());
            buf.writeNbt(payload.stateTag());
        },
        buf -> {
            BlockPos pos = buf.readBlockPos();
            NbtCompound tag = buf.readNbt();
            return new AddOverlayPayload(pos, tag);
        }
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
