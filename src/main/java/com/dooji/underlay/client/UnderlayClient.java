package com.dooji.underlay.client;

import com.dooji.underlay.mixin.MultiPlayerGameModeAccessor;
import com.dooji.underlay.main.Underlay;
import com.dooji.underlay.main.network.payloads.AddOverlayPayload;
import com.dooji.underlay.main.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.main.network.payloads.SyncOverlaysPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = Underlay.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class UnderlayClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(UnderlayClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(UnderlayClient::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(UnderlayClient::onClientDisconnect);
        UnderlayRenderer.init();
    }

    private static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        UnderlayRenderer.clearAllOverlays();
        UnderlayManagerClient.removeAll();
    }

    public static void handleSyncPacket(SyncOverlaysPayload payload) {
        var client = Minecraft.getInstance();
        var map = payload.tags().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> NbtUtils.readBlockState(
                                client.level.holderLookup(Registries.BLOCK),
                                e.getValue()
                        )
                ));

        UnderlayManagerClient.sync(map);
        UnderlayRenderer.clearAllOverlays();
        UnderlayManagerClient.getAll().forEach(UnderlayRenderer::registerOverlay);
    }

    public static void handleAddPacket(AddOverlayPayload payload) {
        BlockPos pos = payload.pos();

        var client = Minecraft.getInstance();
        var state = NbtUtils.readBlockState(
                client.level.holderLookup(Registries.BLOCK),
                payload.stateTag()
        );

        UnderlayManagerClient.syncAdd(pos, state);
        UnderlayRenderer.registerOverlay(pos, state);
        client.level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, state.getSoundType().getVolume(), state.getSoundType().getPitch(), false);
    }

    public static void handleRemovePacket(RemoveOverlayPayload payload) {
        BlockPos pos = payload.pos();
        var client = Minecraft.getInstance();
        var state = UnderlayManagerClient.getOverlay(pos);

        UnderlayRenderer.unregisterOverlay(pos);
        UnderlayManagerClient.syncRemove(pos);

        if (state != null) {
            assert client.level != null;
            client.level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), state.getSoundType().getBreakSound(), SoundSource.BLOCKS, state.getSoundType().getVolume(), state.getSoundType().getPitch(), false);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null || client.screen != null) {
            return;
        }

        handleContinuousBreaking(client);
    }

    private static void handleContinuousBreaking(Minecraft client) {
        if (client.options.keyAttack.isDown()) {
            BlockPos hit = findOverlayUnderCrosshair(client);
            MultiPlayerGameModeAccessor playerInteraction = (MultiPlayerGameModeAccessor) client.gameMode;
            if (hit != null && playerInteraction != null) {
                if (playerInteraction.getBlockBreakingCooldown() == 0) {
                    breakOverlay(client, hit);
                }
            }
        }
    }

    public static void breakOverlay(Minecraft client, BlockPos pos) {
        MultiPlayerGameModeAccessor interactionManager = (MultiPlayerGameModeAccessor) client.gameMode;
        PacketDistributor.sendToServer(new RemoveOverlayPayload(pos));
        interactionManager.setBlockBreakingCooldown(5);
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            UnderlayRenderer.clearAllOverlays();
            UnderlayManagerClient.removeAll();
        }
    }

    public static BlockPos findOverlayUnderCrosshair(Minecraft client) {
        double reach = Player.DEFAULT_BLOCK_INTERACTION_RANGE;
        float partialTicks = (float)(client.getFrameTimeNs() / 50_000_000L);
        assert client.player != null;
        BlockHitResult result = UnderlayRaycast.trace(client.player, reach, partialTicks);
        return result == null ? null : result.getBlockPos();
    }
}
