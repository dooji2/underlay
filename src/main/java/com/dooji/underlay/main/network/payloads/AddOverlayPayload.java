package com.dooji.underlay.main.network.payloads;

import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.io.IOException;

public class AddOverlayPayload implements IMessage {
    private BlockPos pos;
    private NBTTagCompound stateTag;
    private int stateId;

    public AddOverlayPayload(BlockPos pos, NBTTagCompound stateTag, int stateId) {
        this.pos = pos;
        this.stateTag = stateTag;
        this.stateId = stateId;
    }

    public AddOverlayPayload() {
    }

    public BlockPos pos() {
        return pos;
    }

    public NBTTagCompound stateTag() {
        return stateTag;
    }

    public int stateId() {
        return stateId;
    }

    @Override
    public void toBytes(ByteBuf byteBuf) {
        PacketBuffer buf = new PacketBuffer(byteBuf);
        buf.writeBlockPos(pos);
        buf.writeCompoundTag(stateTag);
        buf.writeVarInt(stateId);
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
        PacketBuffer buf = new PacketBuffer(byteBuf);
        pos = buf.readBlockPos();

        try {
            stateTag = buf.readCompoundTag();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (buf.readableBytes() > 0) {
            stateId = buf.readVarInt();
        } else {
            stateId = -1;
        }
    }
}
