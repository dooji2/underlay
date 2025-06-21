package com.dooji.underlay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dooji.underlay.network.UnderlayNetworking;

public class Underlay implements ModInitializer {
	public static final String MOD_ID = "underlay";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final TagKey<Block> OVERLAY_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "overlay"));
	public static final TagKey<Block> EXCLUDE_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "exclude"));

	@Override
	public void onInitialize() {
		UnderlayNetworking.init();
		
		ServerWorldEvents.LOAD.register((server, world) -> {
			LOGGER.info("Loading overlays for world: " + world.getRegistryKey().getValue());
			UnderlayManager.loadOverlays(world);
			reloadDatapackBlocks(world);
		});

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) {
				return;
			}

            for (ServerWorld world : server.getWorlds()) {
                reloadDatapackBlocks(world);
            }
        });
	}

	private static void reloadDatapackBlocks(ServerWorld world) {
		UnderlayApi.CUSTOM_BLOCKS_DP.clear();
		var blocks = world.getRegistryManager().get(RegistryKeys.BLOCK);

		blocks.getEntryList(OVERLAY_TAG).ifPresent(list -> {
			list.stream()
				.filter(entry -> !entry.isIn(EXCLUDE_TAG))
				.map(RegistryEntry::value)
				.forEach(UnderlayApi::registerDatapackOverlayBlock);
		});
	}
}
