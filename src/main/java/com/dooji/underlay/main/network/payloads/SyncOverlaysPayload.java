package com.dooji.underlay.main.network.payloads;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class SyncOverlaysPayload implements IMessage {
    private Map<BlockPos, NBTTagCompound> tags = new HashMap<BlockPos, NBTTagCompound>();
    private Map<BlockPos, Integer> stateIds = new HashMap<BlockPos, Integer>();

    public SyncOverlaysPayload(Map<BlockPos, NBTTagCompound> tags) {
        this.tags = tags;
    }

    public SyncOverlaysPayload(Map<BlockPos, NBTTagCompound> tags, Map<BlockPos, Integer> stateIds) {
        this.tags = tags;
        this.stateIds = stateIds;
    }

    public SyncOverlaysPayload() {
    }

    public Map<BlockPos, NBTTagCompound> tags() {
        return tags;
    }

    public Map<BlockPos, Integer> stateIds() {
        return stateIds;
    }

    @Override
    public void toBytes(ByteBuf byteBuf) {
        PacketBuffer buf = new PacketBuffer(byteBuf);
        buf.writeInt(tags.size());

        for (Map.Entry<BlockPos, NBTTagCompound> entry : tags.entrySet()) {
            buf.writeBlockPos(entry.getKey());
            buf.writeCompoundTag(entry.getValue());
            buf.writeVarInt(stateIds.getOrDefault(entry.getKey(), -1));
        }
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
        PacketBuffer buf = new PacketBuffer(byteBuf);
        int count = buf.readInt();
        tags = new HashMap<>(count);
        stateIds = new HashMap<>(count);

        for (int i = 0; i < count; i++) {
            BlockPos pos = buf.readBlockPos();
            NBTTagCompound tag;

            try {
                tag = buf.readCompoundTag();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            tags.put(pos, tag);
            if (buf.readableBytes() > 0) {
                stateIds.put(pos, buf.readVarInt());
            }
        }
    }
}
