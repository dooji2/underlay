package com.dooji.underlay.main;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dooji.underlay.main.network.UnderlayNetworking;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.init.Blocks;

public class UnderlayManager {
    private static final Map<String, Map<BlockPos, IBlockState>> OVERLAYS = new ConcurrentHashMap<String, Map<BlockPos, IBlockState>>();

    public static void addOverlay(EntityPlayerMP player, World world, BlockPos pos, IBlockState blockState) {
        if (world == null || pos == null || blockState == null) {
            return;
        }

        if (!UnderlayApi.isOverlayBlock(blockState.getBlock())) {
            return;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, IBlockState> worldOverlays = OVERLAYS.computeIfAbsent(dimensionKey, k -> new ConcurrentHashMap<BlockPos, IBlockState>());

        if (worldOverlays.containsKey(pos)) {
            IBlockState old = worldOverlays.get(pos);
            if (!player.isCreative()) {
                ItemStack stack = createStackForState(old, world, pos, player);
                if (!stack.isEmpty()) {
                    world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), stack));
                }
            }
        }

        try {
            worldOverlays.put(pos, blockState);
            UnderlayNetworking.broadcastAdd((WorldServer) world, pos);

            if (!world.isRemote && world instanceof WorldServer) {
                UnderlayPersistenceHandler.saveOverlays(world, worldOverlays);
            }
        } catch (Exception e) {
            Underlay.LOGGER.error("Failed to add overlay at " + pos, e);
        }
    }

    public static void addOverlayFromStructure(World world, BlockPos pos, IBlockState blockState) {
        if (world == null || pos == null || blockState == null) {
            return;
        }

        if (!UnderlayApi.isOverlayBlock(blockState.getBlock())) {
            return;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, IBlockState> worldOverlays = OVERLAYS.computeIfAbsent(dimensionKey, k -> new ConcurrentHashMap<BlockPos, IBlockState>());

        try {
            worldOverlays.put(pos, blockState);
            UnderlayNetworking.broadcastAdd((WorldServer) world, pos);
            UnderlayPersistenceHandler.saveOverlays(world, worldOverlays);
        } catch (Exception e) {
            Underlay.LOGGER.error("Failed to add structure overlay at " + pos, e);
        }
    }

    public static boolean removeOverlay(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, IBlockState> worldOverlays = OVERLAYS.get(dimensionKey);

        try {
            if (worldOverlays != null && worldOverlays.containsKey(pos)) {
                worldOverlays.remove(pos);

                if (!world.isRemote && world instanceof WorldServer) {
                    UnderlayPersistenceHandler.saveOverlays(world, worldOverlays);
                }

                return true;
            }
        } catch (Exception e) {
            Underlay.LOGGER.error("Failed to remove overlay at " + pos, e);
        }

        return false;
    }

    public static boolean hasOverlay(World world, BlockPos pos) {
        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, IBlockState> worldOverlays = OVERLAYS.get(dimensionKey);

        return worldOverlays != null && worldOverlays.containsKey(pos);
    }

    public static IBlockState getOverlay(World world, BlockPos pos) {
        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, IBlockState> worldOverlays = OVERLAYS.get(dimensionKey);

        if (worldOverlays != null && worldOverlays.containsKey(pos)) {
            return worldOverlays.get(pos);
        }

        return Blocks.AIR.getDefaultState();
    }

    public static Map<BlockPos, IBlockState> getOverlaysFor(World world) {
        String key = getDimensionKey(world);
        Map<BlockPos, IBlockState> map = OVERLAYS.get(key);
        return map == null ? Collections.<BlockPos, IBlockState>emptyMap() : map;
    }

    public static void loadOverlays(World world) {
        if (world.isRemote || !(world instanceof WorldServer)) {
            return;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, IBlockState> worldOverlays = UnderlayPersistenceHandler.loadOverlays(world);

        OVERLAYS.put(dimensionKey, worldOverlays);
    }

    private static String getDimensionKey(World world) {
        return Integer.toString(world.provider.getDimension());
    }

    public static ItemStack createStackForState(IBlockState state, World world, BlockPos pos, EntityPlayerMP player) {
        ItemStack stack = state.getBlock().getPickBlock(state, new RayTraceResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), EnumFacing.UP, pos), world, pos, player);
        if (stack.isEmpty()) {
            stack = new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state));
        }

        return stack;
    }
}
