package com.dooji.underlay.main.network.payloads;

import com.dooji.underlay.main.Underlay;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AddOverlayPayload(BlockPos pos, CompoundTag stateTag) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AddOverlayPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Underlay.MOD_ID, "add_overlay"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddOverlayPayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    AddOverlayPayload::write,
                    AddOverlayPayload::read
            );

    public static AddOverlayPayload read(RegistryFriendlyByteBuf buf) {
        return new AddOverlayPayload(buf.readBlockPos(), buf.readNbt());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNbt(stateTag);
    }

    @Override
    public CustomPacketPayload.Type<AddOverlayPayload> type() {
        return TYPE;
    }
}
