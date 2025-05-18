package com.dooji.underlay.main;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;

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

        if (block instanceof CarpetBlock
                || block instanceof ButtonBlock
                || block instanceof TrapDoorBlock
                || block instanceof PressurePlateBlock
                || block instanceof SlabBlock
                || block instanceof RailBlock) {
            return true;
        }

        return CUSTOM_BLOCKS.contains(block) || CUSTOM_BLOCKS_DP.contains(block);
    }
}
