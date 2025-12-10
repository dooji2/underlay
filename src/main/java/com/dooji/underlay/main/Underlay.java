package com.dooji.underlay.main;

import com.dooji.underlay.main.network.UnderlayNetworking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.world.WorldServer;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = Underlay.MOD_ID, name = "Underlay", version = "0.9.9")
public class Underlay {
    public static final String MOD_ID = "underlay";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        UnderlayNetworking.init();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        for (WorldServer world : event.getServer().worlds) {
            UnderlayManager.loadOverlays(world);
        }
    }
}
