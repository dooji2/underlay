package com.dooji.underlay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dooji.underlay.network.UnderlayNetworking;

public class Underlay implements ModInitializer {
	public static final String MOD_ID = "underlay";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final TagKey<Block> OVERLAY_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "overlay"));
	public static final TagKey<Block> EXCLUDE_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "exclude"));

	@Override
	public void onInitialize() {
		UnderlayNetworking.init();
		
		ServerWorldEvents.LOAD.register((server, world) -> {
			LOGGER.info("Loading overlays for world: " + world.dimension().identifier());
			UnderlayManager.loadOverlays(world);
			UnderlayConfig.load(world);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> UnderlayPersistenceHandler.flushPendingSaves());
		ServerWorldEvents.UNLOAD.register((server, world) -> UnderlayPersistenceHandler.flushPendingSave(world));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> UnderlayPersistenceHandler.flushAllPendingSaves());

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) {
				return;
			}

            for (ServerLevel world : server.getAllLevels()) {
                UnderlayConfig.load(world);
            }
        });
	}
}
