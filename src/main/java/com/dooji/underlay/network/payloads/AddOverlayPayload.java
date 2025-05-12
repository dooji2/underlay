package com.dooji.underlay.network.payloads;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record AddOverlayPayload(BlockPos pos, NbtCompound stateTag) {
    public static final Identifier ID = new Identifier("underlay", "add_overlay");

    public static void write(PacketByteBuf buf, AddOverlayPayload payload) {
        buf.writeBlockPos(payload.pos());
        buf.writeNbt(payload.stateTag());
    }

    public static AddOverlayPayload read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        NbtCompound tag = buf.readNbt();
        return new AddOverlayPayload(pos, tag);
    }
}
