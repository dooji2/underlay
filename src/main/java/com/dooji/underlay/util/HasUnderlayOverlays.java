package com.dooji.underlay.util;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface HasUnderlayOverlays {
    Map<BlockPos, BlockState> getMovingOverlays();
    int getMovingOverlaysVersion();
}
