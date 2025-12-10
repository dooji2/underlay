package com.dooji.underlay.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnderlayManagerClient {
    private static final Map<BlockPos, IBlockState> OVERLAYS = new ConcurrentHashMap<BlockPos, IBlockState>();

    public static void sync(Map<BlockPos, IBlockState> stateMap) {
        OVERLAYS.clear();
        OVERLAYS.putAll(stateMap);
    }

    public static void syncAdd(BlockPos pos, IBlockState state) {
        OVERLAYS.put(pos, state);
    }

    public static void syncRemove(BlockPos pos) {
        OVERLAYS.remove(pos);
    }

    public static boolean hasOverlay(BlockPos pos) {
        return OVERLAYS.containsKey(pos);
    }

    public static IBlockState getOverlay(BlockPos pos) {
        return OVERLAYS.get(pos);
    }

    public static Map<BlockPos, IBlockState> getAll() {
        return OVERLAYS;
    }

    public static void removeAll() {
        OVERLAYS.clear();
    }
}
