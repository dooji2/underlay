package com.dooji.underlay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

public class UnderlayPersistenceHandler {
    private static final int MAX_OVERLAY_SAVE_ATTEMPTS = 3;

    private static String getSaveFileName(Level world) {
        String dimensionId = world.dimension().identifier().toString();
        if ("minecraft:overworld".equals(dimensionId)) {
            return "underlays.dat";
        }

        return "underlays_" + dimensionId.replace(':', '_') + ".dat";
    }

    public static void saveOverlays(Level world, Map<BlockPos, BlockState> overlays) {
        if (world == null || overlays == null) {
            Underlay.LOGGER.warn("Attempted to save overlays with null parameters");
            return;
        }

        if (world.isClientSide() || !(world instanceof ServerLevel)) {
            return;
        }

        for (int attempt = 0; attempt < MAX_OVERLAY_SAVE_ATTEMPTS; attempt++) {
            try {
                CompoundTag rootTag = new CompoundTag();
                ListTag overlayList = new ListTag();

                for (Map.Entry<BlockPos, BlockState> entry : overlays.entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockState state = entry.getValue();

                    CompoundTag overlayTag = new CompoundTag();
                    overlayTag.putInt("x", pos.getX());
                    overlayTag.putInt("y", pos.getY());
                    overlayTag.putInt("z", pos.getZ());
                    overlayTag.put("state", NbtUtils.writeBlockState(state));

                    Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    overlayTag.putString("block", blockId.toString());

                    overlayList.add(overlayTag);
                }

                rootTag.put("overlays", overlayList);

                ServerLevel serverWorld = (ServerLevel) world;
                Path saveDir = serverWorld.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
                String fileName = getSaveFileName(world);
                File saveFile = saveDir.resolve(fileName).toFile();

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

    public static Map<BlockPos, BlockState> loadOverlays(Level world) {
        Map<BlockPos, BlockState> overlays = new HashMap<>();
        
        if (world == null) {
            Underlay.LOGGER.warn("Attempted to load overlays with null world");
            return overlays;
        }

        if (world.isClientSide() || !(world instanceof ServerLevel)) {
            return overlays;
        }

        try {
            ServerLevel serverWorld = (ServerLevel) world;
            HolderGetter<Block> lookup = serverWorld.registryAccess().lookupOrThrow(Registries.BLOCK);
            
            Path saveDir = serverWorld.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
            String fileName = getSaveFileName(world);
            File saveFile = saveDir.resolve(fileName).toFile();
            
            if (!saveFile.exists()) {
                Underlay.LOGGER.info("No existing overlay save file found");
                return overlays;
            }

            try (FileInputStream fis = new FileInputStream(saveFile)) {
                CompoundTag rootTag = NbtIo.readCompressed(fis, NbtAccounter.unlimitedHeap());
                
                if (rootTag.contains("overlays")) {
                    ListTag overlayList = rootTag.getListOrEmpty("overlays");

                    for (int i = 0; i < overlayList.size(); i++) {
                        CompoundTag overlayTag = overlayList.getCompoundOrEmpty(i);
                        
                        int x = overlayTag.getIntOr("x", 0);
                        int y = overlayTag.getIntOr("y", 0);
                        int z = overlayTag.getIntOr("z", 0);
                        Optional<CompoundTag> optNbt = overlayTag.getCompound("state");
                        CompoundTag stateTag = optNbt.orElse(new CompoundTag());
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state;

                        if (!stateTag.isEmpty()) {
                            state = NbtUtils.readBlockState(lookup, stateTag);
                        } else {
                            String blockIdString = overlayTag.getStringOr("block", "");
                            Identifier blockId = Identifier.tryParse(blockIdString);

                            if (blockId != null) {
                                state = BuiltInRegistries.BLOCK.getValue(blockId).defaultBlockState();
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
