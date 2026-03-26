package com.dooji.underlay;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.TrapDoorBlock;

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

	public static boolean isOverlayBlock(ServerLevel world, Block block) {
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
                || block instanceof TrapDoorBlock
                || block instanceof PressurePlateBlock
                || block instanceof SlabBlock
                || block instanceof BaseRailBlock;
	}
}
