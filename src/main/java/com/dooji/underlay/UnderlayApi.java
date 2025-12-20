package com.dooji.underlay;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;

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

	public static boolean isOverlayBlock(ServerLevel world, Block block) {
		if (block == null) {
			return false;
		}

		if (CUSTOM_BLOCKS.contains(block) || CUSTOM_BLOCKS_DP.contains(block)) {
			return true;
		}

		var blocks = world.registryAccess().lookupOrThrow(Registries.BLOCK);
		if (blocks.wrapAsHolder(block).is(EXCLUDE_TAG)) {
			return false;
		}

		return block instanceof CarpetBlock
                || block instanceof ButtonBlock
                || block instanceof TrapDoorBlock
                || block instanceof PressurePlateBlock
                || block instanceof SlabBlock
                || block instanceof BaseRailBlock;
	}
}
