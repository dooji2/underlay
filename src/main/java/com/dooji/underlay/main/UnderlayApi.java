package com.dooji.underlay.main;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButtonStone;
import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockPressurePlateWeighted;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockTrapDoor;

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

        return block instanceof BlockCarpet
                || block instanceof BlockButtonStone
                || block instanceof BlockButtonWood
                || block instanceof BlockTrapDoor
                || block instanceof BlockPressurePlate
                || block instanceof BlockPressurePlateWeighted
                || block instanceof BlockSlab
                || block instanceof BlockRailBase;
    }
}
