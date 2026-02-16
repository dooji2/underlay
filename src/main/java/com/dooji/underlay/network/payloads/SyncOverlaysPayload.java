package com.dooji.underlay.network.payloads;

import java.util.HashMap;
import java.util.Map;
import com.dooji.underlay.Underlay;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record SyncOverlaysPayload(Map<BlockPos, NbtCompound> tags) implements CustomPayload {
    public static final CustomPayload.Id<SyncOverlaysPayload> ID =
        new CustomPayload.Id<>(Identifier.of(Underlay.MOD_ID, "sync_overlays"));

    public static final PacketCodec<PacketByteBuf, SyncOverlaysPayload> CODEC = PacketCodec.ofStatic(
        (buf, payload) -> {
            buf.writeVarInt(payload.tags().size());
            
            for (var e : payload.tags().entrySet()) {
                buf.writeBlockPos(e.getKey());
                buf.writeNbt(e.getValue());
            }
        },
        buf -> {
            int count = buf.readVarInt();
            Map<BlockPos, NbtCompound> map = new HashMap<>(count);

            for (int i = 0; i < count; i++) {
                BlockPos pos = buf.readBlockPos();
                NbtCompound tag = buf.readNbt();
                map.put(pos, tag);
            }

            return new SyncOverlaysPayload(map);
        }
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
