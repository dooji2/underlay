package com.dooji.underlay.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class UnderlayManagerClient {
    private static final Map<BlockPos, BlockState> OVERLAYS = new ConcurrentHashMap<>();

    public static void sync(Map<BlockPos, BlockState> stateMap) {
        OVERLAYS.clear();
        OVERLAYS.putAll(stateMap);
    }

    public static void syncAdd(BlockPos pos, BlockState state) {
        OVERLAYS.put(pos, state);
    }

    public static void syncRemove(BlockPos pos) {
        OVERLAYS.remove(pos);
    }

    public static boolean hasOverlay(BlockPos pos) {
        return OVERLAYS.containsKey(pos);
    }

    public static BlockState getOverlay(BlockPos pos) {
        return OVERLAYS.get(pos);
    }

    public static Map<BlockPos, BlockState> getAll() {
        return OVERLAYS;
    }

    public static void removeAll() {
        OVERLAYS.clear();
    }
}
