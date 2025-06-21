package com.dooji.underlay.main.network.payloads;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record AddOverlayPayload(BlockPos pos, CompoundTag stateTag) {
    public static void write(AddOverlayPayload message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
        buf.writeNbt(message.stateTag);
    }

    public static AddOverlayPayload read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        CompoundTag tag = buf.readNbt();
        return new AddOverlayPayload(pos, tag);
    }
}
