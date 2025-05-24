package com.dooji.underlay.main.network.payloads;

import com.dooji.underlay.main.Underlay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemoveOverlayPayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RemoveOverlayPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Underlay.MOD_ID, "remove_overlay"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveOverlayPayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    RemoveOverlayPayload::write,
                    RemoveOverlayPayload::read
            );

    public static RemoveOverlayPayload read(RegistryFriendlyByteBuf buf) {
        return new RemoveOverlayPayload(buf.readBlockPos());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public CustomPacketPayload.Type<RemoveOverlayPayload> type() {
        return TYPE;
    }
}
