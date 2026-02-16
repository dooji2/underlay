package com.dooji.underlay.network.payloads;

import java.util.HashMap;
import java.util.Map;
import com.dooji.underlay.Underlay;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncOverlaysPayload(Map<BlockPos, CompoundTag> tags) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncOverlaysPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Underlay.MOD_ID, "sync_overlays"));

    public static final StreamCodec<FriendlyByteBuf, SyncOverlaysPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.tags().size());
            
            for (var e : payload.tags().entrySet()) {
                buf.writeBlockPos(e.getKey());
                buf.writeNbt(e.getValue());
            }
        },
        buf -> {
            int count = buf.readVarInt();
            Map<BlockPos, CompoundTag> map = new HashMap<>(count);

            for (int i = 0; i < count; i++) {
                BlockPos pos = buf.readBlockPos();
                CompoundTag tag = buf.readNbt();
                map.put(pos, tag);
            }

            return new SyncOverlaysPayload(map);
        }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
