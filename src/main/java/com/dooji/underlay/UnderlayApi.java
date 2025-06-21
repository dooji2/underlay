package com.dooji.underlay;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.AbstractRailBlock;

import static com.dooji.underlay.Underlay.EXCLUDE_TAG;

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

	public static boolean isOverlayBlock(ServerWorld world, Block block) {
		if (block == null) {
			return false;
		}

		if (CUSTOM_BLOCKS.contains(block) || CUSTOM_BLOCKS_DP.contains(block)) {
            return true;
        }

		Registry<Block> blocks = world.getRegistryManager().getOrThrow(RegistryKeys.BLOCK);
		if (blocks.getEntry(block).isIn(EXCLUDE_TAG)) {
			return false;
		}

		return block instanceof CarpetBlock
                || block instanceof ButtonBlock
                || block instanceof TrapdoorBlock
                || block instanceof PressurePlateBlock
                || block instanceof SlabBlock
                || block instanceof AbstractRailBlock;
	}
}
