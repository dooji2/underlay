package com.dooji.underlay;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.AbstractRailBlock;

public class UnderlayApi {
	private static final Set<Block> CUSTOM_BLOCKS = ConcurrentHashMap.newKeySet();

	public static void registerOverlayBlock(Block block) {
		if (block == null) {
			return;
		}
        
		CUSTOM_BLOCKS.add(block);
	}

	public static boolean isOverlayBlock(Block block) {
		if (block == null) {
			return false;
		}

		if (block instanceof CarpetBlock
                || block instanceof ButtonBlock
                || block instanceof TrapdoorBlock
                || block instanceof PressurePlateBlock
                || block instanceof SlabBlock
                || block instanceof AbstractRailBlock) {
			return true;
		}
        
		return CUSTOM_BLOCKS.contains(block);
	}
}
