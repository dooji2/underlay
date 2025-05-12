package com.dooji.underlay.network.payloads;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record SyncOverlaysPayload(Map<BlockPos, NbtCompound> tags) {
    public static final Identifier ID = new Identifier("underlay", "sync_overlays");

    public static void write(PacketByteBuf buf, SyncOverlaysPayload payload) {
        buf.writeVarInt(payload.tags().size());

        for (var e : payload.tags().entrySet()) {
            buf.writeBlockPos(e.getKey());
            buf.writeNbt(e.getValue());
        }
    }

    public static SyncOverlaysPayload read(PacketByteBuf buf) {
        int count = buf.readVarInt();
        Map<BlockPos, NbtCompound> map = new HashMap<>(count);

        for (int i = 0; i < count; i++) {
            BlockPos pos = buf.readBlockPos();
            NbtCompound tag = buf.readNbt();
            map.put(pos, tag);
        }

        return new SyncOverlaysPayload(map);
    }
}
