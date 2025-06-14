package com.dooji.underlay.main;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.*;

import static com.dooji.underlay.main.Underlay.EXCLUDE_TAG;

public class UnderlayApi {
    static final Set<Block> CUSTOM_BLOCKS = ConcurrentHashMap.newKeySet();
    static final Set<Block> CUSTOM_BLOCKS_DP = ConcurrentHashMap.newKeySet();

    public static void registerOverlayBlock(Block block) {
        if (block == null) {
            return;
        }

        CUSTOM_BLOCKS.add(block);
    }

    static void registerDatapackOverlayBlock(Block block) {
        if (block == null) {
            return;
        }

        CUSTOM_BLOCKS_DP.add(block);
    }

    public static boolean isOverlayBlock(Block block) {
        if (block == null) {
            return false;
        }

        if (CUSTOM_BLOCKS.contains(block) || CUSTOM_BLOCKS_DP.contains(block)) {
            return true;
        }

        Optional<HolderSet.Named<Block>> maybeExcluded = BuiltInRegistries.BLOCK.getTag(EXCLUDE_TAG);
        if (maybeExcluded.map(named ->
                named.stream().anyMatch(holder -> holder.value() == block)
        ).orElse(false)) {
            return false;
        }

        return block instanceof CarpetBlock
                || block instanceof ButtonBlock
                || block instanceof TrapDoorBlock
                || block instanceof PressurePlateBlock
                || block instanceof SlabBlock
                || block instanceof RailBlock;
    }
}
