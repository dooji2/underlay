package com.dooji.underlay.client;

import com.dooji.underlay.mixin.MultiPlayerGameModeAccessor;
import com.dooji.underlay.main.Underlay;
import com.dooji.underlay.main.network.UnderlayNetworking;
import com.dooji.underlay.main.network.payloads.AddOverlayPayload;
import com.dooji.underlay.main.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.main.network.payloads.SyncOverlaysPayload;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Underlay.MOD_ID, value = Side.CLIENT)
public class UnderlayClient {
    @SubscribeEvent
    public static void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        UnderlayRenderer.clearAllOverlays();
        UnderlayManagerClient.removeAll();
    }

    public static void handleSyncPacket(SyncOverlaysPayload payload) {
        Map<BlockPos, IBlockState> map = new HashMap<BlockPos, IBlockState>();
        for (Map.Entry<BlockPos, NBTTagCompound> entry : payload.tags().entrySet()) {
            int stateId = payload.stateIds().getOrDefault(entry.getKey(), -1);
            IBlockState state = stateId >= 0 ? Block.getStateById(stateId) : null;
            if (state == null) {
                state = NBTUtil.readBlockState(entry.getValue());
            }

            map.put(entry.getKey(), state);
        }

        UnderlayManagerClient.sync(map);
        UnderlayRenderer.clearAllOverlays();
        for (Map.Entry<BlockPos, IBlockState> entry : UnderlayManagerClient.getAll().entrySet()) {
            UnderlayRenderer.registerOverlay(entry.getKey(), entry.getValue());
        }
    }

    public static void handleAddPacket(AddOverlayPayload payload) {
        BlockPos pos = payload.pos();
        IBlockState state = payload.stateId() >= 0 ? Block.getStateById(payload.stateId()) : null;
        if (state == null) {
            state = NBTUtil.readBlockState(payload.stateTag());
        }

        UnderlayManagerClient.syncAdd(pos, state);
        UnderlayRenderer.registerOverlay(pos, state);

        Minecraft client = Minecraft.getMinecraft();
        if (client.world != null) {
            SoundType sound = state.getBlock().getSoundType(state, client.world, pos, client.player);
            client.world.playSound(client.player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, sound.getVolume(), sound.getPitch());
        }
    }

    public static void handleRemovePacket(RemoveOverlayPayload payload) {
        BlockPos pos = payload.pos();
        IBlockState state = UnderlayManagerClient.getOverlay(pos);

        UnderlayRenderer.unregisterOverlay(pos);
        UnderlayManagerClient.syncRemove(pos);

        Minecraft client = Minecraft.getMinecraft();
        if (state != null && client.world != null) {
            SoundType sound = state.getBlock().getSoundType(state, client.world, pos, client.player);
            client.world.playSound(client.player, pos, sound.getBreakSound(), SoundCategory.BLOCKS, sound.getVolume(), sound.getPitch());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft client = Minecraft.getMinecraft();

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (client.player == null || client.world == null || client.currentScreen != null) {
            return;
        }

        handleContinuousBreaking(client);
    }

    private static void handleContinuousBreaking(Minecraft client) {
        if (client.gameSettings.keyBindAttack.isKeyDown()) {
            BlockPos hit = findOverlayUnderCrosshair(client);
            if (!(client.playerController instanceof MultiPlayerGameModeAccessor)) {
                return;
            }

            MultiPlayerGameModeAccessor playerInteraction = (MultiPlayerGameModeAccessor) client.playerController;
            if (hit != null && playerInteraction != null) {
                if (playerInteraction.getBlockBreakingCooldown() == 0) {
                    breakOverlay(client, hit);
                }
            }
        }
    }

    public static void breakOverlay(Minecraft client, BlockPos pos) {
        if (!(client.playerController instanceof MultiPlayerGameModeAccessor)) {
            return;
        }

        MultiPlayerGameModeAccessor interactionManager = (MultiPlayerGameModeAccessor) client.playerController;
        UnderlayNetworking.INSTANCE.sendToServer(new RemoveOverlayPayload(pos));
        client.player.swingArm(EnumHand.MAIN_HAND);
        interactionManager.setBlockBreakingCooldown(5);
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            UnderlayRenderer.clearAllOverlays();
            UnderlayManagerClient.removeAll();
        }
    }

    public static BlockPos findOverlayUnderCrosshair(Minecraft client) {
        EntityPlayer player = client.player;
        if (player == null) {
            return null;
        }

        RayTraceResult hit = UnderlayRaycast.trace(player, client.playerController.getBlockReachDistance(), client.getRenderPartialTicks());
        return hit == null ? null : hit.getBlockPos();
    }
}
