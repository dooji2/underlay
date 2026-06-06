package com.dooji.underlay.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class UnderlayConfig {
    public static final String OVERLAY_BLOCKS_KEY = "overlay_blocks";
    public static final String EXCLUDE_BLOCKS_KEY = "exclude_blocks";
    public static final String TARGET_EXCLUDE_BLOCKS_KEY = "target_exclude_blocks";
    private static final String CONFIG_FILE_NAME = "underlay.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<String> overlayBlocks = new ArrayList<>();
    private final List<String> excludeBlocks = new ArrayList<>();
    private final List<String> targetExcludeBlocks = new ArrayList<>();

    public static void load() {
        UnderlayRegistry.clearLoadedBlocks();

        UnderlayConfig config = readConfig();
        registerExcludedBlocks(config);
        registerOverlayBlocks(config);
        registerTargetExcludedBlocks(config);
    }

    public List<String> getOverlayBlocks() {
        return overlayBlocks;
    }

    public void addOverlayBlock(String blockId) {
        if (blockId == null || blockId.trim().isEmpty()) {
            return;
        }

        overlayBlocks.add(blockId);
    }

    public List<String> getExcludeBlocks() {
        return excludeBlocks;
    }

    public void addExcludeBlock(String blockId) {
        if (blockId == null || blockId.trim().isEmpty()) {
            return;
        }

        excludeBlocks.add(blockId);
    }

    public List<String> getTargetExcludeBlocks() {
        return targetExcludeBlocks;
    }

    public void addTargetExcludeBlock(String blockId) {
        if (blockId == null || blockId.trim().isEmpty()) {
            return;
        }

        targetExcludeBlocks.add(blockId);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonArray blocks = new JsonArray();
        JsonArray excludes = new JsonArray();
        JsonArray targetExcludes = new JsonArray();

        for (String blockId : overlayBlocks) {
            blocks.add(blockId);
        }

        root.add(OVERLAY_BLOCKS_KEY, blocks);
        for (String blockId : excludeBlocks) {
            excludes.add(blockId);
        }

        root.add(EXCLUDE_BLOCKS_KEY, excludes);
        for (String blockId : targetExcludeBlocks) {
            targetExcludes.add(blockId);
        }

        root.add(TARGET_EXCLUDE_BLOCKS_KEY, targetExcludes);
        return root;
    }

    public static UnderlayConfig fromJson(JsonObject root) {
        UnderlayConfig config = new UnderlayConfig();

        if (root == null) {
            return config;
        }

        if (root.has(OVERLAY_BLOCKS_KEY) && root.get(OVERLAY_BLOCKS_KEY).isJsonArray()) {
            JsonArray blocks = root.getAsJsonArray(OVERLAY_BLOCKS_KEY);
            for (JsonElement entry : blocks) {
                if (!entry.isJsonPrimitive()) {
                    continue;
                }

                config.addOverlayBlock(entry.getAsString());
            }
        }

        if (root.has(EXCLUDE_BLOCKS_KEY) && root.get(EXCLUDE_BLOCKS_KEY).isJsonArray()) {
            JsonArray excludes = root.getAsJsonArray(EXCLUDE_BLOCKS_KEY);
            for (JsonElement entry : excludes) {
                if (!entry.isJsonPrimitive()) {
                    continue;
                }

                config.addExcludeBlock(entry.getAsString());
            }
        }

        if (root.has(TARGET_EXCLUDE_BLOCKS_KEY) && root.get(TARGET_EXCLUDE_BLOCKS_KEY).isJsonArray()) {
            JsonArray targetExcludes = root.getAsJsonArray(TARGET_EXCLUDE_BLOCKS_KEY);
            for (JsonElement entry : targetExcludes) {
                if (!entry.isJsonPrimitive()) {
                    continue;
                }

                config.addTargetExcludeBlock(entry.getAsString());
            }
        }

        return config;
    }

    private static UnderlayConfig readConfig() {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            writeConfig(new UnderlayConfig());
            return new UnderlayConfig();
        }

        try (FileInputStream fis = new FileInputStream(configFile);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            JsonElement element = new JsonParser().parse(reader);

            if (!element.isJsonObject()) {
                Underlay.LOGGER.warn("Invalid underlay config");
                return new UnderlayConfig();
            }

            return UnderlayConfig.fromJson(element.getAsJsonObject());
        } catch (IOException e) {
            Underlay.LOGGER.error("Failed to load underlay config", e);
            return new UnderlayConfig();
        }
    }

    private static void writeConfig(UnderlayConfig config) {
        File configFile = getConfigFile();
        File parent = configFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(configFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            GSON.toJson(config.toJson(), writer);
        } catch (IOException e) {
            Underlay.LOGGER.error("Failed to create underlay config", e);
        }
    }

    private static void registerOverlayBlocks(UnderlayConfig config) {
        for (String blockIdString : config.getOverlayBlocks()) {
            ResourceLocation blockId;

            try {
                blockId = new ResourceLocation(blockIdString);
            } catch (Exception e) {
                Underlay.LOGGER.warn("Invalid block ID in underlay config: " + blockIdString);
                continue;
            }

            Block block = GameRegistry.findRegistry(Block.class).getValue(blockId);
            if (block != null) {
                UnderlayRegistry.registerOverlayBlock(block);
            } else {
                Underlay.LOGGER.warn("Unknown overlay block in config: " + blockIdString);
            }
        }
    }

    private static void registerExcludedBlocks(UnderlayConfig config) {
        for (String blockIdString : config.getExcludeBlocks()) {
            ResourceLocation blockId;

            try {
                blockId = new ResourceLocation(blockIdString);
            } catch (Exception e) {
                Underlay.LOGGER.warn("Invalid block ID in underlay config: " + blockIdString);
                continue;
            }

            Block block = GameRegistry.findRegistry(Block.class).getValue(blockId);
            if (block != null) {
                UnderlayRegistry.registerExcludedBlock(block);
            } else {
                Underlay.LOGGER.warn("Unknown excluded block in config: " + blockIdString);
            }
        }
    }

    private static void registerTargetExcludedBlocks(UnderlayConfig config) {
        for (String blockIdString : config.getTargetExcludeBlocks()) {
            ResourceLocation blockId;

            try {
                blockId = new ResourceLocation(blockIdString);
            } catch (Exception e) {
                Underlay.LOGGER.warn("Invalid block ID in underlay config: " + blockIdString);
                continue;
            }

            Block block = GameRegistry.findRegistry(Block.class).getValue(blockId);
            if (block != null) {
                UnderlayRegistry.registerTargetExcludedBlock(block);
            } else {
                Underlay.LOGGER.warn("Unknown target block in config: " + blockIdString);
            }
        }
    }

    private static File getConfigFile() {
        File configDir = new File(Loader.instance().getConfigDir(), "Underlay");
        return new File(configDir, CONFIG_FILE_NAME);
    }
}
