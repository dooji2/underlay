package com.dooji.underlay.main.network.payloads;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record SyncOverlaysPayload(Map<BlockPos, CompoundTag> tags) {

    public static void write(SyncOverlaysPayload message, FriendlyByteBuf buf) {
        buf.writeInt(message.tags.size());

        for (var entry : message.tags.entrySet()) {
            buf.writeBlockPos(entry.getKey());
            buf.writeNbt(entry.getValue());
        }
    }

    public static SyncOverlaysPayload read(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Map<BlockPos, CompoundTag> map = new HashMap<>(count);

        for (int i = 0; i < count; i++) {
            BlockPos pos = buf.readBlockPos();
            CompoundTag tag = buf.readNbt();
            map.put(pos, tag);
        }

        return new SyncOverlaysPayload(map);
    }
}
