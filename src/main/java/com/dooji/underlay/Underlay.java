package com.dooji.underlay;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dooji.underlay.network.UnderlayNetworking;

@Mod(Underlay.MOD_ID)
public class Underlay {
	public static final String MOD_ID = "underlay";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final TagKey<Block> OVERLAY_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "overlay"));
	public static final TagKey<Block> EXCLUDE_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "exclude"));

	public Underlay(IEventBus modEventBus) {
		modEventBus.addListener(UnderlayNetworking::registerPayloads);
		onInitialize();
	}

	public void onInitialize() {
		UnderlayNetworking.init();
		UnderlayCommands.register();
		
		NeoForge.EVENT_BUS.addListener((LevelEvent.Load event) -> {
			if (!(event.getLevel() instanceof ServerLevel world)) {
				return;
			}

			LOGGER.info("Loading overlays for world: " + world.dimension().identifier());
			UnderlayManager.loadOverlays(world);
			UnderlayConfig.load(world);
		});

		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post server) -> UnderlayPersistenceHandler.flushPendingSaves());
		NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
			if (event.getLevel() instanceof ServerLevel world) {
				UnderlayPersistenceHandler.flushPendingSave(world);
			}
		});

		NeoForge.EVENT_BUS.addListener((ServerStoppingEvent server) -> UnderlayPersistenceHandler.flushAllPendingSaves());
		NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() != null) {
				return;
			}

            for (ServerLevel world : event.getPlayerList().getServer().getAllLevels()) {
                UnderlayConfig.load(world);
            }
        });
	}
}
