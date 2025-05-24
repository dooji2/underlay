package com.dooji.underlay.main.network.payloads;

import com.dooji.underlay.main.Underlay;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.HashMap;

public record SyncOverlaysPayload(Map<BlockPos, CompoundTag> tags) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncOverlaysPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Underlay.MOD_ID, "sync_overlays"));

    public static final StreamCodec<RegistryFriendlyByteBuf,SyncOverlaysPayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    SyncOverlaysPayload::write,
                    SyncOverlaysPayload::read
            );

    public static SyncOverlaysPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<BlockPos, CompoundTag> tags = new HashMap<>();

        for (int i = 0; i < size; i++) {
            tags.put(buf.readBlockPos(), buf.readNbt());
        }

        return new SyncOverlaysPayload(tags);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(tags.size());
        for (var e : tags.entrySet()) {
            buf.writeBlockPos(e.getKey());
            buf.writeNbt(e.getValue());
        }
    }

    @Override public CustomPacketPayload.Type<SyncOverlaysPayload> type() {
        return TYPE;
    }
}
