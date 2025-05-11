package com.dooji.underlay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dooji.underlay.network.UnderlayNetworking;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UnderlayManager {
    private static final Map<String, Map<BlockPos, BlockState>> OVERLAYS = new ConcurrentHashMap<>();

    public static void addOverlay(ServerPlayerEntity player, World world, BlockPos pos, BlockState blockState) {
        if (world == null || pos == null || blockState == null) {
            Underlay.LOGGER.warn("Attempted to add overlay with null parameters");
            return;
        }

        if (!UnderlayApi.isOverlayBlock(blockState.getBlock())) {
            Underlay.LOGGER.warn("Attempted to add non-overlay block as overlay: " + blockState);
            return;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = OVERLAYS.computeIfAbsent(dimensionKey, k -> new ConcurrentHashMap<>());

        if (worldOverlays.containsKey(pos)) {
            BlockState old = worldOverlays.get(pos);
            if (!player.isCreative()) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(old.getBlock()));
            }
        }

        try {
            worldOverlays.put(pos.toImmutable(), blockState);
            UnderlayNetworking.broadcastAdd((ServerWorld)world, pos);

            if (!world.isClient() && world instanceof ServerWorld) {
                UnderlayPersistenceHandler.saveOverlays(world, worldOverlays);
            }
        } catch (Exception e) {
            Underlay.LOGGER.error(("Failed to add overlay at ") + pos, e);
        }
    }

    public static boolean removeOverlay(World world, BlockPos pos) {
        if (world == null || pos == null) {
            Underlay.LOGGER.warn("Attempted to remove overlay with null parameters");
            return false;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = OVERLAYS.get(dimensionKey);

        try {
            if (worldOverlays != null && worldOverlays.containsKey(pos)) {
                worldOverlays.remove(pos);

                if (!world.isClient() && world instanceof ServerWorld) {
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
        Map<BlockPos, BlockState> worldOverlays = OVERLAYS.get(dimensionKey);

        return worldOverlays != null && worldOverlays.containsKey(pos);
    }

    public static BlockState getOverlay(World world, BlockPos pos) {
        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = OVERLAYS.get(dimensionKey);

        if (worldOverlays != null && worldOverlays.containsKey(pos)) {
            return worldOverlays.get(pos);
        }

        return Blocks.AIR.getDefaultState();
    }

    public static Map<BlockPos, BlockState> getOverlaysFor(World world) {
        String key = world.getRegistryKey().getValue().toString();
        return OVERLAYS.getOrDefault(key, Map.of());
    }

    public static void loadOverlays(World world) {
        if (world.isClient() || !(world instanceof ServerWorld)) {
            return;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = UnderlayPersistenceHandler.loadOverlays(world);

        OVERLAYS.put(dimensionKey, worldOverlays);
        Underlay.LOGGER.info("Loaded " + worldOverlays.size() + " overlays for dimension " + dimensionKey);
    }

    private static String getDimensionKey(World world) {
        return world.getRegistryKey().getValue().toString();
    }
}
