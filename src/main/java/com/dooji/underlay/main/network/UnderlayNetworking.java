package com.dooji.underlay.main.network;

import com.dooji.underlay.client.UnderlayClient;
import com.dooji.underlay.main.UnderlayManager;
import com.dooji.underlay.main.network.payloads.AddOverlayPayload;
import com.dooji.underlay.main.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.main.network.payloads.SyncOverlaysPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class UnderlayNetworking {
    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                SyncOverlaysPayload.TYPE,
                SyncOverlaysPayload.STREAM_CODEC,
                (payload, context) -> UnderlayClient.handleSyncPacket(payload)
        );

        registrar.playToClient(
                AddOverlayPayload.TYPE,
                AddOverlayPayload.STREAM_CODEC,
                (payload, context) -> UnderlayClient.handleAddPacket(payload)
        );

        registrar.playBidirectional(
                RemoveOverlayPayload.TYPE,
                RemoveOverlayPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> UnderlayClient.handleRemovePacket(payload),
                        (payload, context) ->{
                            ServerPlayer player = (ServerPlayer)context.player();

                            ServerLevel world = player.serverLevel();
                            BlockPos pos = payload.pos();

                            if (!world.hasChunkAt(pos) || !world.getWorldBorder().isWithinBounds(pos)) return;
                            if (!world.mayInteract(player, pos)) return;

                            if (UnderlayManager.hasOverlay(world, pos)) {
                                var oldState = UnderlayManager.getOverlay(world, pos);
                                UnderlayManager.removeOverlay(world, pos);

                                if (!player.isCreative()) {
                                    world.addFreshEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(oldState.getBlock())));
                                }

                                PacketDistributor.sendToPlayersTrackingChunk(world, new ChunkPos(pos), new RemoveOverlayPayload(pos));
                            }
                        }
                )
        );
    }

    public static void syncOverlaysToPlayer(ServerPlayer player) {
        var world = player.serverLevel();
        Map<BlockPos, CompoundTag> tags = new HashMap<>();

        UnderlayManager.getOverlaysFor(world).forEach((pos, state) ->
                tags.put(pos, NbtUtils.writeBlockState(state))
        );

        PacketDistributor.sendToPlayer(player, new SyncOverlaysPayload(tags));
    }

    public static void broadcastAdd(ServerLevel world, BlockPos pos) {
        var tag = NbtUtils.writeBlockState(UnderlayManager.getOverlay(world, pos));
        PacketDistributor.sendToPlayersTrackingChunk(world, new ChunkPos(pos), new AddOverlayPayload(pos, tag));
    }
}
