package com.dooji.underlay.main;

import com.dooji.underlay.main.events.BlockInteractionEvents;
import com.dooji.underlay.main.events.PlayerEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Underlay.MOD_ID)
public class Underlay {
    public static final String MOD_ID = "underlay";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final TagKey<Block> OVERLAY_TAG = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "overlay"));
    public static final TagKey<Block> EXCLUDE_TAG = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "exclude"));

    public Underlay() {
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new BlockInteractionEvents());
        NeoForge.EVENT_BUS.register(new PlayerEvents());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        for (ServerLevel world : event.getServer().getAllLevels()) {
            LOGGER.info("Loading overlays for world: " + world.dimension().location());
            UnderlayManager.loadOverlays(world);
            UnderlayConfig.load(world);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        UnderlayPersistenceHandler.flushPendingSaves();
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel world) {
            UnderlayPersistenceHandler.flushPendingSave(world);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        UnderlayPersistenceHandler.flushAllPendingSaves();
    }
}
