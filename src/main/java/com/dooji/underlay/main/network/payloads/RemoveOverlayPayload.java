package com.dooji.underlay.main.network.payloads;

import io.netty.buffer.ByteBuf;

import net.minecraft.util.math.BlockPos;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class RemoveOverlayPayload implements IMessage {
    private BlockPos pos;

    public RemoveOverlayPayload(BlockPos pos) {
        this.pos = pos;
    }

    public RemoveOverlayPayload() {
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public void toBytes(ByteBuf byteBuf) {
        PacketBuffer buf = new PacketBuffer(byteBuf);
        buf.writeBlockPos(pos);
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
        PacketBuffer buf = new PacketBuffer(byteBuf);
        pos = buf.readBlockPos();
    }
}
