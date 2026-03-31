package com.dooji.underlay.main;

import com.dooji.underlay.main.network.UnderlayNetworking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.world.WorldServer;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;

@Mod(modid = Underlay.MOD_ID, name = "Underlay", version = "1.0.0")
@Mod.EventBusSubscriber(modid = Underlay.MOD_ID)
public class Underlay {
    public static final String MOD_ID = "underlay";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        UnderlayConfig.load();
        UnderlayNetworking.init();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        for (WorldServer world : event.getServer().worlds) {
            UnderlayManager.loadOverlays(world);
        }
    }

    @Mod.EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        UnderlayPersistenceHandler.flushAllPendingSaves();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            UnderlayPersistenceHandler.flushPendingSaves();
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote && event.getWorld() instanceof WorldServer) {
            UnderlayPersistenceHandler.flushPendingSave(event.getWorld());
        }
    }
}
