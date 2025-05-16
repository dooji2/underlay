package com.dooji.underlay.util;

import net.minecraft.util.math.BlockPos;

public class MinecartTracker {
    private static final ThreadLocal<BlockPos> ACTIVE_MINECRAFT_POS = new ThreadLocal<>();

    public static void setActiveMinecartPos(BlockPos pos) {
        ACTIVE_MINECRAFT_POS.set(pos);
    }
    
    public static void clearActiveMinecartPos() {
        ACTIVE_MINECRAFT_POS.remove();
    }

    public static BlockPos getActiveMinecartPos() {
        return ACTIVE_MINECRAFT_POS.get();
    }

    public static boolean isNearby(BlockPos pos, int maxDistance) {
        BlockPos minecartPos = ACTIVE_MINECRAFT_POS.get();
        return minecartPos != null && pos.isWithinDistance(minecartPos, maxDistance);
    }
}
