package com.dooji.underlay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UnderlayPersistenceHandler {
    private static final String SAVE_FILE_NAME = "underlays.dat";
    private static final int MAX_OVERLAY_SAVE_ATTEMPTS = 3;

    public static void saveOverlays(World world, Map<BlockPos, BlockState> overlays) {
        if (world == null || overlays == null) {
            Underlay.LOGGER.warn("Attempted to save overlays with null parameters");
            return;
        }

        if (world.isClient() || !(world instanceof ServerWorld)) {
            return;
        }

        for (int attempt = 0; attempt < MAX_OVERLAY_SAVE_ATTEMPTS; attempt++) {
            try {
                NbtCompound rootTag = new NbtCompound();
                NbtList overlayList = new NbtList();

                for (Map.Entry<BlockPos, BlockState> entry : overlays.entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockState state = entry.getValue();

                    NbtCompound overlayTag = new NbtCompound();
                    overlayTag.putInt("x", pos.getX());
                    overlayTag.putInt("y", pos.getY());
                    overlayTag.putInt("z", pos.getZ());
                    overlayTag.put("state", NbtHelper.fromBlockState(state));

                    Identifier blockId = Registries.BLOCK.getId(state.getBlock());
                    overlayTag.putString("block", blockId.toString());

                    overlayList.add(overlayTag);
                }

                rootTag.put("overlays", overlayList);

                ServerWorld serverWorld = (ServerWorld) world;
                Path saveDir = serverWorld.getServer().getSavePath(WorldSavePath.ROOT).resolve("data");
                File saveFile = saveDir.resolve(SAVE_FILE_NAME).toFile();

                saveDir.toFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    NbtIo.writeCompressed(rootTag, fos);
                    break;
                }
            } catch (IOException e) {
                Underlay.LOGGER.error("Failed to save overlays (Attempt " + (attempt +1) + ")", e);

                try {
                    Thread.sleep(100 * (attempt + 1));
                } catch (InterruptedException ie) {
                    Underlay.LOGGER.warn("Overlay save interrupted", ie);
                    break;
                }
            }
        }
    }

    public static Map<BlockPos, BlockState> loadOverlays(World world) {
        Map<BlockPos, BlockState> overlays = new HashMap<>();
        
        if (world == null) {
            Underlay.LOGGER.warn("Attempted to load overlays with null world");
            return overlays;
        }

        if (world.isClient() || !(world instanceof ServerWorld)) {
            return overlays;
        }

        try {
            ServerWorld serverWorld = (ServerWorld) world;
            RegistryEntryLookup<Block> lookup = serverWorld.getRegistryManager().getOrThrow(RegistryKeys.BLOCK);
            
            Path saveDir = serverWorld.getServer().getSavePath(WorldSavePath.ROOT).resolve("data");
            File saveFile = saveDir.resolve(SAVE_FILE_NAME).toFile();
            
            if (!saveFile.exists()) {
                Underlay.LOGGER.info("No existing overlay save file found");
                return overlays;
            }

            try (FileInputStream fis = new FileInputStream(saveFile)) {
                NbtCompound rootTag = NbtIo.readCompressed(fis, NbtSizeTracker.ofUnlimitedBytes());
                
                if (rootTag.contains("overlays")) {
                    NbtList overlayList = rootTag.getList("overlays", 10);

                    for (int i = 0; i < overlayList.size(); i++) {
                        NbtCompound overlayTag = overlayList.getCompound(i);
                        
                        int x = overlayTag.getInt("x");
                        int y = overlayTag.getInt("y");
                        int z = overlayTag.getInt("z");
                        NbtCompound stateTag = overlayTag.getCompound("state");
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state;

                        if (!stateTag.isEmpty()) {
                            state = NbtHelper.toBlockState(lookup, stateTag);
                        } else {
                            String blockIdString = overlayTag.getString("block");
                            Identifier blockId = Identifier.tryParse(blockIdString);

                            if (blockId != null) {
                                state = Registries.BLOCK.get(blockId).getDefaultState();
                            } else {
                                Underlay.LOGGER.warn("Invalid block ID in overlay save: " + blockIdString);
                                continue;
                            }
                        }

                        overlays.put(pos, state);
                    }

                    Underlay.LOGGER.info("Loaded " + overlays.size() + " overlays");
                }
            }
        } catch (IOException e) {
            Underlay.LOGGER.error("Failed to load overlays", e);
        }

        return overlays;
    }
}
