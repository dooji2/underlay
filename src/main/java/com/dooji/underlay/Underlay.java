package com.dooji.underlay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dooji.underlay.network.UnderlayNetworking;

public class Underlay implements ModInitializer {
	public static final String MOD_ID = "underlay";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		UnderlayNetworking.init();
		
		ServerWorldEvents.LOAD.register((server, world) -> {
			LOGGER.info("Loading overlays for world: " + world.getRegistryKey().getValue());
			UnderlayManager.loadOverlays(world);
		});
	}
}
