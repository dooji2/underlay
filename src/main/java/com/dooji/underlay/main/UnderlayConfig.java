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

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;

public class UnderlayConfig {
    public static final String OVERLAY_BLOCKS_KEY = "overlay_blocks";
    public static final String EXCLUDE_BLOCKS_KEY = "exclude_blocks";
    private static final String CONFIG_FILE_NAME = "underlay.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<String> overlayBlocks = new ArrayList<>();
    private final List<String> excludeBlocks = new ArrayList<>();

    public static void load(ServerLevel world) {
        if (world == null || world.isClientSide()) {
            return;
        }

        UnderlayApi.clearLoadedBlocks();

        UnderlayConfig config = readConfig();
        loadDatapackBlocks(world);
        loadDatapackExcludes(world);
        registerOverlayBlocks(config);
        registerExcludedBlocks(config);
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

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonArray blocks = new JsonArray();
        JsonArray excludes = new JsonArray();

        for (String blockId : overlayBlocks) {
            blocks.add(blockId);
        }

        root.add(OVERLAY_BLOCKS_KEY, blocks);
        for (String blockId : excludeBlocks) {
            excludes.add(blockId);
        }

        root.add(EXCLUDE_BLOCKS_KEY, excludes);
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
            JsonElement element = JsonParser.parseReader(reader);

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

    private static void loadDatapackBlocks(ServerLevel world) {
        Registry<Block> blocks = world.registryAccess().registryOrThrow(BuiltInRegistries.BLOCK.key());

        blocks.getTag(Underlay.OVERLAY_TAG).ifPresent(tag -> tag.forEach(entry -> {
            Block block = entry.value();
            if (!blocks.getOrCreateTag(Underlay.EXCLUDE_TAG).contains(entry)) {
                UnderlayApi.registerDatapackOverlayBlock(block);
            }
        }));
    }

    private static void loadDatapackExcludes(ServerLevel world) {
        Registry<Block> blocks = world.registryAccess().registryOrThrow(BuiltInRegistries.BLOCK.key());

        blocks.getTag(Underlay.EXCLUDE_TAG).ifPresent(tag -> tag.forEach(entry -> {
            Block block = entry.value();
            UnderlayApi.registerDatapackExcludedBlock(block);
        }));
    }

    private static void registerOverlayBlocks(UnderlayConfig config) {
        for (String blockIdString : config.getOverlayBlocks()) {
            ResourceLocation blockId = ResourceLocation.tryParse(blockIdString);

            if (blockId == null) {
                Underlay.LOGGER.warn("Invalid block ID in underlay config: " + blockIdString);
                continue;
            }

            Block block = BuiltInRegistries.BLOCK.get(blockId);
            if (!BuiltInRegistries.BLOCK.getKey(block).equals(blockId)) {
                Underlay.LOGGER.warn("Missing block in underlay config: " + blockId);
                continue;
            }

            UnderlayApi.registerOverlayBlock(block);
        }
    }

    private static void registerExcludedBlocks(UnderlayConfig config) {
        for (String blockIdString : config.getExcludeBlocks()) {
            ResourceLocation blockId = ResourceLocation.tryParse(blockIdString);

            if (blockId == null) {
                Underlay.LOGGER.warn("Invalid block ID in underlay config: " + blockIdString);
                continue;
            }

            Block block = BuiltInRegistries.BLOCK.get(blockId);
            if (!BuiltInRegistries.BLOCK.getKey(block).equals(blockId)) {
                Underlay.LOGGER.warn("Missing block in underlay config: " + blockId);
                continue;
            }

            UnderlayApi.registerExcludedBlock(block);
        }
    }

    private static File getConfigFile() {
        return FMLPaths.CONFIGDIR.get().resolve("Underlay").resolve(CONFIG_FILE_NAME).toFile();
    }
}
