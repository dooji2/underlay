package com.dooji.underlay.main;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dooji.underlay.main.network.UnderlayNetworking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public class UnderlayManager {
    private static final Map<String, Map<BlockPos, BlockState>> OVERLAYS = new ConcurrentHashMap<>();

    public static void addOverlay(ServerPlayer player, Level world, BlockPos pos, BlockState blockState) {
        if (world == null || pos == null || blockState == null) {
            return;
        }

        if (!UnderlayApi.isOverlayBlock(blockState.getBlock())) {
            return;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = OVERLAYS.computeIfAbsent(dimensionKey, k -> new ConcurrentHashMap<>());

        if (worldOverlays.containsKey(pos)) {
            BlockState old = worldOverlays.get(pos);
            if (!player.isCreative()) {
                world.addFreshEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(old.getBlock())));
            }
        }

        try {
            worldOverlays.put(pos.immutable(), blockState);
            UnderlayNetworking.broadcastAdd((ServerLevel)world, pos);

            if (!world.isClientSide() && world instanceof ServerLevel) {
                UnderlayPersistenceHandler.saveOverlays(world, worldOverlays);
            }
        } catch (Exception e) {
            Underlay.LOGGER.error("Failed to add overlay at " + pos, e);
        }
    }

    public static void addOverlayFromStructure(Level world, BlockPos pos, BlockState blockState) {
        if (world == null || pos == null || blockState == null) {
            return;
        }

        if (!UnderlayApi.isOverlayBlock(blockState.getBlock())) {
            return;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = OVERLAYS.computeIfAbsent(dimensionKey, k -> new ConcurrentHashMap<>());

        try {
            worldOverlays.put(pos.immutable(), blockState);
            UnderlayNetworking.broadcastAdd((ServerLevel)world, pos);
            UnderlayPersistenceHandler.saveOverlays(world, worldOverlays);
        } catch (Exception e) {
            Underlay.LOGGER.error("Failed to add structure overlay at " + pos, e);
        }
    }

    public static void addOverlayFromContraption(Level world, BlockPos pos, BlockState blockState) {
        if (world == null || pos == null || blockState == null) {
            return;
        }

        if (!UnderlayApi.isOverlayBlock(blockState.getBlock())) {
            return;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = OVERLAYS.computeIfAbsent(dimensionKey, k -> new ConcurrentHashMap<>());

        try {
            worldOverlays.put(pos.immutable(), blockState);

            if (!world.isClientSide() && world instanceof ServerLevel serverLevel) {
                UnderlayNetworking.broadcastAdd(serverLevel, pos);
                UnderlayPersistenceHandler.saveOverlays(world, worldOverlays);
            }
        } catch (Exception e) {
            Underlay.LOGGER.error("Failed to add contraption overlay at " + pos, e);
        }
    }

    public static boolean removeOverlay(Level world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = OVERLAYS.get(dimensionKey);

        try {
            if (worldOverlays != null && worldOverlays.containsKey(pos)) {
                worldOverlays.remove(pos);

                if (!world.isClientSide() && world instanceof ServerLevel) {
                    UnderlayPersistenceHandler.saveOverlays(world, worldOverlays);
                }

                return true;
            }
        } catch (Exception e) {
            Underlay.LOGGER.error("Failed to remove overlay at " + pos, e);
        }

        return false;
    }

    public static boolean removeOverlayFromContraption(Level world, BlockPos pos) {
        boolean removed = removeOverlay(world, pos);

        if (removed && !world.isClientSide() && world instanceof ServerLevel serverLevel) {
            UnderlayNetworking.broadcastRemove(serverLevel, pos);
        }

        return removed;
    }

    public static boolean hasOverlay(Level world, BlockPos pos) {
        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = OVERLAYS.get(dimensionKey);

        return worldOverlays != null && worldOverlays.containsKey(pos);
    }

    public static BlockState getOverlay(Level world, BlockPos pos) {
        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = OVERLAYS.get(dimensionKey);

        if (worldOverlays != null && worldOverlays.containsKey(pos)) {
            return worldOverlays.get(pos);
        }

        return Blocks.AIR.defaultBlockState();
    }

    public static Map<BlockPos, BlockState> getOverlaysFor(Level world) {
        String key = world.dimension().location().toString();
        return OVERLAYS.getOrDefault(key, Map.of());
    }

    public static void loadOverlays(Level world) {
        if (world.isClientSide() || !(world instanceof ServerLevel)) {
            return;
        }

        String dimensionKey = getDimensionKey(world);
        Map<BlockPos, BlockState> worldOverlays = UnderlayPersistenceHandler.loadOverlays(world);

        OVERLAYS.put(dimensionKey, worldOverlays);
        Underlay.LOGGER.info("Loaded " + worldOverlays.size() + " overlays for dimension " + dimensionKey);
    }

    private static String getDimensionKey(Level world) {
        return world.dimension().location().toString();
    }
}
