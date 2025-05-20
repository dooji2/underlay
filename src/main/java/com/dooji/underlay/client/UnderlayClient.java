package com.dooji.underlay.client;

import com.dooji.underlay.client.mixin.MultiPlayerGameModeAccessor;
import com.dooji.underlay.main.Underlay;
import com.dooji.underlay.main.network.UnderlayNetworking;
import com.dooji.underlay.main.network.payloads.AddOverlayPayload;
import com.dooji.underlay.main.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.main.network.payloads.SyncOverlaysPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Underlay.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class UnderlayClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(UnderlayClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(UnderlayClient::onLevelUnload);
        MinecraftForge.EVENT_BUS.addListener(UnderlayClient::onClientDisconnect);

        event.enqueueWork(() -> {});
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

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft client = Minecraft.getInstance();

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

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
        UnderlayNetworking.INSTANCE.send(PacketDistributor.SERVER.noArg(), new RemoveOverlayPayload(pos));
        interactionManager.setBlockBreakingCooldown(5);
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            UnderlayRenderer.clearAllOverlays();
            UnderlayManagerClient.removeAll();
        }
    }

    public static BlockPos findOverlayUnderCrosshair(Minecraft client) {
        var hit = UnderlayRaycast.trace(client.player, client.gameMode.getPickRange(), client.getFrameTime());
        return hit == null ? null : hit.getBlockPos();
    }
}
