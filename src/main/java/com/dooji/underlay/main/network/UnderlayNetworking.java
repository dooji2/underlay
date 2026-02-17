package com.dooji.underlay.main.network;

import com.dooji.underlay.main.Underlay;
import com.dooji.underlay.client.UnderlayClient;
import com.dooji.underlay.main.UnderlayManager;
import com.dooji.underlay.main.network.payloads.AddOverlayPayload;
import com.dooji.underlay.main.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.main.network.payloads.SyncOverlaysPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;

public class UnderlayNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final ResourceLocation NETWORK_CHANNEL = ResourceLocation.tryBuild(Underlay.MOD_ID, "main");
    public static SimpleChannel INSTANCE;

    public static void init() {
        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(NETWORK_CHANNEL)
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(v -> true)
                .serverAcceptedVersions(v -> true)
                .simpleChannel();

        int id = 0;

        INSTANCE.messageBuilder(SyncOverlaysPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncOverlaysPayload::write)
                .decoder(SyncOverlaysPayload::read)
                .consumerNetworkThread((msg, contextSupplier) -> {
                    var context = contextSupplier.get();

                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(() -> UnderlayClient.handleSyncPacket(msg));
                    }

                    context.setPacketHandled(true);
                })
                .add();

        INSTANCE.messageBuilder(AddOverlayPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AddOverlayPayload::write)
                .decoder(AddOverlayPayload::read)
                .consumerNetworkThread((msg, contextSupplier) -> {
                    var context = contextSupplier.get();

                    if (context.getDirection().getReceptionSide().isClient()) {
                        context.enqueueWork(() -> UnderlayClient.handleAddPacket(msg));
                    }

                    context.setPacketHandled(true);
                })
                .add();

        INSTANCE.messageBuilder(RemoveOverlayPayload.class, id++, null)
                .encoder(RemoveOverlayPayload::write)
                .decoder(RemoveOverlayPayload::read)
                .consumerNetworkThread((msg, contextSupplier) -> {
                    var context = contextSupplier.get();

                    if (context.getDirection().getReceptionSide().isServer()) {
                        context.enqueueWork(() -> {
                            var player = context.getSender();
                            if (player == null) return;

                            var world = player.serverLevel();
                            var pos = msg.pos();

                            if (!world.hasChunkAt(pos) || !world.getWorldBorder().isWithinBounds(pos)) return;
                            if (!world.mayInteract(player, pos)) return;

                            if (UnderlayManager.hasOverlay(world, pos)) {
                                var oldState = UnderlayManager.getOverlay(world, pos);
                                boolean removed = UnderlayManager.removeOverlayAndBroadcast(world, pos);

                                if (removed && !player.isCreative()) {
                                    world.addFreshEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(oldState.getBlock())));
                                }
                            }
                        });
                    } else if (FMLEnvironment.dist == Dist.CLIENT) {
                        context.enqueueWork(() -> UnderlayClient.handleRemovePacket(msg));
                    }

                    context.setPacketHandled(true);
                })
                .add();
    }

    public static void syncOverlaysToPlayer(ServerPlayer player) {
        var world = player.serverLevel();
        Map<BlockPos, CompoundTag> tags = new HashMap<>();

        UnderlayManager.getOverlaysFor(world).forEach((pos, state) ->
                tags.put(pos, NbtUtils.writeBlockState(state))
        );

        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncOverlaysPayload(tags));
    }

    public static void broadcastAdd(ServerLevel world, BlockPos pos) {
        var tag = NbtUtils.writeBlockState(UnderlayManager.getOverlay(world, pos));
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(pos)), new AddOverlayPayload(pos, tag));
    }

    public static void broadcastRemove(ServerLevel world, BlockPos pos) {
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(pos)), new RemoveOverlayPayload(pos));
    }
}
