package com.dooji.underlay.main;

import java.io.File;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class UnderlayConfig {
    private static final String CATEGORY = "general";
    private static final String KEY = "overlay_blocks";

    public static void load() {
        Configuration config = new Configuration(new File(Loader.instance().getConfigDir(), "underlay.cfg"));
        config.load();

        String[] entries = config.getStringList(KEY, CATEGORY, new String[0], "Additional blocks allowed as overlays (e.g. minecraft:tallgrass)");
        for (String entry : entries) {
            ResourceLocation id = new ResourceLocation(entry);
            Block block = GameRegistry.findRegistry(Block.class).getValue(id);
            if (block != null) {
                UnderlayApi.registerConfigOverlayBlock(block);
            } else {
                Underlay.LOGGER.warn("Unknown overlay block in config: " + entry);
            }
        }

        if (config.hasChanged()) {
            config.save();
        }
    }
}
