package com.dooji.underlay;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.server.world.ServerWorld;

public class UnderlayApi {
	static final Set<Block> CUSTOM_BLOCKS = ConcurrentHashMap.newKeySet();
	static final Set<Block> CUSTOM_BLOCKS_DP = ConcurrentHashMap.newKeySet();
	static final Set<Block> CUSTOM_BLOCKS_EXCLUDE = ConcurrentHashMap.newKeySet();
	static final Set<Block> CUSTOM_BLOCKS_EXCLUDE_DP = ConcurrentHashMap.newKeySet();

	static void registerOverlayBlock(Block block) {
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

	static void clearLoadedBlocks() {
		CUSTOM_BLOCKS.clear();
		CUSTOM_BLOCKS_DP.clear();
		CUSTOM_BLOCKS_EXCLUDE.clear();
		CUSTOM_BLOCKS_EXCLUDE_DP.clear();
	}

	static void registerExcludedBlock(Block block) {
		if (block == null) {
			return;
		}

		CUSTOM_BLOCKS_EXCLUDE.add(block);
	}

	static void registerDatapackExcludedBlock(Block block) {
		if (block == null) {
			return;
		}

		CUSTOM_BLOCKS_EXCLUDE_DP.add(block);
	}

	public static boolean isOverlayBlock(ServerWorld world, Block block) {
		if (block == null) {
			return false;
		}

		if (CUSTOM_BLOCKS_EXCLUDE.contains(block)) {
			return false;
		}

		if (CUSTOM_BLOCKS_EXCLUDE_DP.contains(block)) {
			return false;
		}

		if (CUSTOM_BLOCKS.contains(block)) {
			return true;
		}

		if (CUSTOM_BLOCKS_DP.contains(block)) {
			return true;
		}

		return block instanceof CarpetBlock
                || block instanceof ButtonBlock
                || block instanceof TrapdoorBlock
                || block instanceof PressurePlateBlock
                || block instanceof SlabBlock
                || block instanceof AbstractRailBlock;
	}
}
