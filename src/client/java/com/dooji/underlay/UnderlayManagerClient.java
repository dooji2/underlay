package com.dooji.underlay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class UnderlayManagerClient {
    private static final Map<BlockPos, BlockState> OVERLAYS = new ConcurrentHashMap<>();

    public static void sync(Map<BlockPos, BlockState> stateMap) {
        OVERLAYS.clear();
        stateMap.forEach((pos, state) ->
            OVERLAYS.put(pos, state)
        );
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
