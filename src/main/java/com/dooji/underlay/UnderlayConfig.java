package com.dooji.underlay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;

public class UnderlayConfig {
	public static final String OVERLAY_BLOCKS_KEY = "overlay_blocks";
	public static final String EXCLUDE_BLOCKS_KEY = "exclude_blocks";
	public static final String TARGET_EXCLUDE_BLOCKS_KEY = "target_exclude_blocks";
	public static final String PLACE_ON_REPLACEABLE_BLOCKS_KEY = "place_on_replaceable_blocks";
	private static final String COMMANDS_OP_LEVEL_KEY = "commands_op_level";
	private static final String OPTIONS_KEY = "options";
	private static final String CONFIG_FILE_NAME = "underlay.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static UnderlayConfig current = new UnderlayConfig();

	private final List<String> overlayBlocks = new ArrayList<>();
	private final List<String> excludeBlocks = new ArrayList<>();
	private final List<String> targetExcludeBlocks = new ArrayList<>();
	private int commandsOpLevel = 4;
	private boolean placeOnReplaceableBlocks;

	public static void load(ServerWorld world) {
		if (world == null || world.isClient()) {
			return;
		}

		UnderlayRegistry.clearLoadedBlocks();

		current = readConfig();
		loadDatapackOverlayBlocks(world);
		loadDatapackExcludedBlocks(world);
		registerBlocks(world, current.getOverlayBlocks(), UnderlayRegistry::registerOverlayBlock);
		registerBlocks(world, current.getExcludeBlocks(), UnderlayRegistry::registerExcludedBlock);
		registerBlocks(world, current.getTargetExcludeBlocks(), UnderlayRegistry::registerTargetExcludedBlock);
	}

	public static int getCommandsOpLevel() {
		return current.commandsOpLevel;
	}

	public static boolean canPlaceOnReplaceableBlocks() {
		return current.placeOnReplaceableBlocks;
	}

	public static void setPlaceOnReplaceableBlocks(boolean value) {
		current.placeOnReplaceableBlocks = value;
		writeConfig(current);
	}

	public List<String> getOverlayBlocks() {
		return overlayBlocks;
	}

	public void addOverlayBlock(String blockId) {
		if (blockId == null || blockId.isBlank()) {
			return;
		}

		overlayBlocks.add(blockId);
	}

	public List<String> getExcludeBlocks() {
		return excludeBlocks;
	}

	public void addExcludeBlock(String blockId) {
		if (blockId == null || blockId.isBlank()) {
			return;
		}

		excludeBlocks.add(blockId);
	}

	public List<String> getTargetExcludeBlocks() {
		return targetExcludeBlocks;
	}

	public void addTargetExcludeBlock(String blockId) {
		if (blockId == null || blockId.isBlank()) {
			return;
		}

		targetExcludeBlocks.add(blockId);
	}

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		JsonArray blocks = new JsonArray();
		JsonArray excludeBlocks = new JsonArray();
		JsonArray targetExcludeBlocks = new JsonArray();
		JsonObject options = new JsonObject();

		for (String blockId : overlayBlocks) {
			blocks.add(blockId);
		}

		root.add(OVERLAY_BLOCKS_KEY, blocks);
		for (String blockId : this.excludeBlocks) {
			excludeBlocks.add(blockId);
		}

		root.add(EXCLUDE_BLOCKS_KEY, excludeBlocks);
		for (String blockId : this.targetExcludeBlocks) {
			targetExcludeBlocks.add(blockId);
		}

		root.add(TARGET_EXCLUDE_BLOCKS_KEY, targetExcludeBlocks);
		root.addProperty(COMMANDS_OP_LEVEL_KEY, commandsOpLevel);
		options.addProperty(PLACE_ON_REPLACEABLE_BLOCKS_KEY, placeOnReplaceableBlocks);
		root.add(OPTIONS_KEY, options);
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
			JsonArray excludeBlocks = root.getAsJsonArray(EXCLUDE_BLOCKS_KEY);
			for (JsonElement entry : excludeBlocks) {
				if (!entry.isJsonPrimitive()) {
					continue;
				}

				config.addExcludeBlock(entry.getAsString());
			}
		}

		if (root.has(TARGET_EXCLUDE_BLOCKS_KEY) && root.get(TARGET_EXCLUDE_BLOCKS_KEY).isJsonArray()) {
			JsonArray targetExcludeBlocks = root.getAsJsonArray(TARGET_EXCLUDE_BLOCKS_KEY);
			for (JsonElement entry : targetExcludeBlocks) {
				if (!entry.isJsonPrimitive()) {
					continue;
				}

				config.addTargetExcludeBlock(entry.getAsString());
			}
		}

		if (root.has(COMMANDS_OP_LEVEL_KEY) && root.get(COMMANDS_OP_LEVEL_KEY).isJsonPrimitive()
				&& root.getAsJsonPrimitive(COMMANDS_OP_LEVEL_KEY).isNumber()) {
			config.commandsOpLevel = root.get(COMMANDS_OP_LEVEL_KEY).getAsInt();
		}

		if (root.has(OPTIONS_KEY) && root.get(OPTIONS_KEY).isJsonObject()) {
			JsonObject options = root.getAsJsonObject(OPTIONS_KEY);
			if (options.has(PLACE_ON_REPLACEABLE_BLOCKS_KEY)
					&& options.get(PLACE_ON_REPLACEABLE_BLOCKS_KEY).isJsonPrimitive()
					&& options.getAsJsonPrimitive(PLACE_ON_REPLACEABLE_BLOCKS_KEY).isBoolean()) {
				config.placeOnReplaceableBlocks = options.get(PLACE_ON_REPLACEABLE_BLOCKS_KEY).getAsBoolean();
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

	private static void loadDatapackOverlayBlocks(ServerWorld world) {
		RegistryEntryLookup<Block> blocks = world.getRegistryManager().getWrapperOrThrow(RegistryKeys.BLOCK);

		blocks.getOptional(Underlay.OVERLAY_TAG).ifPresent(list -> {
			list.stream()
				.filter(entry -> !entry.isIn(Underlay.EXCLUDE_TAG))
				.map(RegistryEntry::value)
				.forEach(UnderlayRegistry::registerDatapackOverlayBlock);
		});
	}

	private static void loadDatapackExcludedBlocks(ServerWorld world) {
		RegistryEntryLookup<Block> blocks = world.getRegistryManager().getWrapperOrThrow(RegistryKeys.BLOCK);

		blocks.getOptional(Underlay.EXCLUDE_TAG).ifPresent(list -> {
			list.stream()
				.map(RegistryEntry::value)
				.forEach(UnderlayRegistry::registerDatapackExcludedBlock);
		});
	}

	private static void registerBlocks(ServerWorld world, List<String> blockIds, Consumer<Block> register) {
		RegistryEntryLookup<Block> blocks = world.getRegistryManager().getWrapperOrThrow(RegistryKeys.BLOCK);

		for (String blockIdString : blockIds) {
			if (blockIdString.startsWith("#")) {
				Identifier tagId = Identifier.tryParse(blockIdString.substring(1));
				if (tagId == null) {
					Underlay.LOGGER.warn("Invalid block tag in underlay config: " + blockIdString);
					continue;
				}

				blocks.getOptional(TagKey.of(RegistryKeys.BLOCK, tagId)).ifPresent(list -> {
					list.stream()
						.map(RegistryEntry::value)
						.forEach(register);
				});
				continue;
			}

			Identifier blockId = Identifier.tryParse(blockIdString);

			if (blockId == null) {
				Underlay.LOGGER.warn("Invalid block ID in underlay config: " + blockIdString);
				continue;
			}

			Block block = Registries.BLOCK.get(blockId);
			if (!Registries.BLOCK.getId(block).equals(blockId)) {
				Underlay.LOGGER.warn("Missing block in underlay config: " + blockId);
				continue;
			}

			register.accept(block);
		}
	}

	private static File getConfigFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("Underlay").resolve(CONFIG_FILE_NAME).toFile();
	}
}
