package com.dooji.underlay.main.network;

import com.dooji.underlay.client.UnderlayClient;
import com.dooji.underlay.main.Underlay;
import com.dooji.underlay.main.UnderlayManager;
import com.dooji.underlay.main.network.payloads.AddOverlayPayload;
import com.dooji.underlay.main.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.main.network.payloads.SyncOverlaysPayload;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class UnderlayNetworking {
    public static final String PROTOCOL_VERSION = "1";
    public static final ResourceLocation NETWORK_CHANNEL = new ResourceLocation(Underlay.MOD_ID, "main");
    public static SimpleNetworkWrapper INSTANCE;

    public static void init() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Underlay.MOD_ID);

        int id = 0;

        INSTANCE.registerMessage(SyncHandler.class, SyncOverlaysPayload.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(AddHandler.class, AddOverlayPayload.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(RemoveServerHandler.class, RemoveOverlayPayload.class, id++, Side.SERVER);
        INSTANCE.registerMessage(RemoveClientHandler.class, RemoveOverlayPayload.class, id++, Side.CLIENT);
    }

    public static void syncOverlaysToPlayer(EntityPlayerMP player) {
        WorldServer world = player.getServerWorld();
        Map<BlockPos, NBTTagCompound> tags = new HashMap<BlockPos, NBTTagCompound>();
        Map<BlockPos, Integer> stateIds = new HashMap<BlockPos, Integer>();

        for (Map.Entry<BlockPos, IBlockState> entry : UnderlayManager.getOverlaysFor(world).entrySet()) {
            tags.put(entry.getKey(), NBTUtil.writeBlockState(new NBTTagCompound(), entry.getValue()));
            stateIds.put(entry.getKey(), Block.getStateId(entry.getValue()));
        }

        INSTANCE.sendTo(new SyncOverlaysPayload(tags, stateIds), player);
    }

    public static void broadcastAdd(WorldServer world, BlockPos pos) {
        IBlockState state = UnderlayManager.getOverlay(world, pos);
        NBTTagCompound tag = NBTUtil.writeBlockState(new NBTTagCompound(), state);
        int stateId = Block.getStateId(state);
        sendToTrackingChunk(world, pos, new AddOverlayPayload(pos, tag, stateId));
    }

    public static class SyncHandler implements IMessageHandler<SyncOverlaysPayload, IMessage> {
        @Override
        public IMessage onMessage(final SyncOverlaysPayload message, final MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    UnderlayClient.handleSyncPacket(message);
                }
            });
            return null;
        }
    }

    public static class AddHandler implements IMessageHandler<AddOverlayPayload, IMessage> {
        @Override
        public IMessage onMessage(final AddOverlayPayload message, final MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    UnderlayClient.handleAddPacket(message);
                }
            });
            return null;
        }
    }

    public static class RemoveServerHandler implements IMessageHandler<RemoveOverlayPayload, IMessage> {
        @Override
        public IMessage onMessage(final RemoveOverlayPayload message, final MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    BlockPos pos = message.pos();
                    WorldServer world = player.getServerWorld();

                    if (!world.isBlockLoaded(pos)) return;
                    if (!world.getWorldBorder().contains(pos)) return;
                    if (!world.isBlockModifiable(player, pos)) return;

                    if (UnderlayManager.hasOverlay(world, pos)) {
                        IBlockState oldState = UnderlayManager.getOverlay(world, pos);
                        UnderlayManager.removeOverlay(world, pos);

                        if (!player.isCreative()) {
                            ItemStack stack = UnderlayManager.createStackForState(oldState, world, pos, player);
                            if (!stack.isEmpty()) {
                                world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), stack));
                            }
                        }

        sendToTrackingChunk(world, pos, new RemoveOverlayPayload(pos));
                    }
                }
            });
            return null;
        }
    }

    public static class RemoveClientHandler implements IMessageHandler<RemoveOverlayPayload, IMessage> {
        @Override
        public IMessage onMessage(final RemoveOverlayPayload message, final MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    UnderlayClient.handleRemovePacket(message);
                }
            });
            return null;
        }
    }

    private static void sendToTrackingChunk(WorldServer world, BlockPos pos, IMessage message) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        for (EntityPlayerMP player : world.getMinecraftServer().getPlayerList().getPlayers()) {
            if (player.dimension != world.provider.getDimension()) continue;

            if (world.getPlayerChunkMap().isPlayerWatchingChunk(player, chunkX, chunkZ)) {
                INSTANCE.sendTo(message, player);
            }
        }
    }
}
