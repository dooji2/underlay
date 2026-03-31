package com.dooji.underlay.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class UnderlayPersistenceHandler {
    private static final int MAX_OVERLAY_SAVE_ATTEMPTS = 3;
    private static final int SAVE_DEBOUNCE_TICKS = 20;
    private static final Map<String, PendingSave> PENDING_SAVES = new ConcurrentHashMap<String, PendingSave>();

    private static String getSaveFileName(World world) {
        int dimensionId = world.provider.getDimension();
        if (dimensionId == 0) {
            return "underlays.dat";
        }

        return "underlays_" + dimensionId + ".dat";
    }

    public static void saveOverlays(World world, Map<BlockPos, IBlockState> overlays) {
        if (world == null || overlays == null) {
            return;
        }

        if (world.isRemote || !(world instanceof WorldServer)) {
            return;
        }

        for (int attempt = 0; attempt < MAX_OVERLAY_SAVE_ATTEMPTS; attempt++) {
            try {
                NBTTagCompound rootTag = new NBTTagCompound();
                NBTTagList overlayList = new NBTTagList();

                for (Map.Entry<BlockPos, IBlockState> entry : overlays.entrySet()) {
                    BlockPos pos = entry.getKey();
                    IBlockState state = entry.getValue();

                    NBTTagCompound overlayTag = new NBTTagCompound();
                    overlayTag.setInteger("x", pos.getX());
                    overlayTag.setInteger("y", pos.getY());
                    overlayTag.setInteger("z", pos.getZ());
                    overlayTag.setTag("state", NBTUtil.writeBlockState(new NBTTagCompound(), state));
                    overlayTag.setInteger("stateId", Block.getStateId(state));

                    overlayList.appendTag(overlayTag);
                }

                rootTag.setTag("overlays", overlayList);

                WorldServer serverWorld = (WorldServer) world;
                WorldServer rootWorld = serverWorld.getMinecraftServer().getWorld(0);
                File worldDir = rootWorld != null ? rootWorld.getSaveHandler().getWorldDirectory() : serverWorld.getSaveHandler().getWorldDirectory();
                File saveDir = new File(worldDir, "data");
                String fileName = getSaveFileName(world);
                File saveFile = new File(saveDir, fileName);

                saveDir.mkdirs();

                FileOutputStream fos = new FileOutputStream(saveFile);
                CompressedStreamTools.writeCompressed(rootTag, fos);
                fos.close();
                break;
            } catch (IOException e) {
                Underlay.LOGGER.error("Failed to save overlays (Attempt " + (attempt + 1) + ")", e);

                try {
                    Thread.sleep(100 * (attempt + 1));
                } catch (InterruptedException ie) {
                    Underlay.LOGGER.warn("Overlay save interrupted", ie);
                    break;
                }
            }
        }
    }

    public static void markDirty(World world, Map<BlockPos, IBlockState> overlays) {
        if (world == null || overlays == null) {
            return;
        }

        if (world.isRemote || !(world instanceof WorldServer)) {
            return;
        }

        PENDING_SAVES.put(getDimensionKey(world), new PendingSave((WorldServer) world, new HashMap<BlockPos, IBlockState>(overlays), SAVE_DEBOUNCE_TICKS));
    }

    public static void flushPendingSaves() {
        for (Map.Entry<String, PendingSave> entry : PENDING_SAVES.entrySet()) {
            PendingSave pendingSave = entry.getValue();
            if (pendingSave == null) {
                continue;
            }

            pendingSave.ticksUntilSave--;
            if (pendingSave.ticksUntilSave <= 0) {
                if (PENDING_SAVES.remove(entry.getKey(), pendingSave)) {
                    saveOverlays(pendingSave.world, pendingSave.overlays);
                }
            }
        }
    }

    public static void flushPendingSave(World world) {
        if (world == null) {
            return;
        }

        PendingSave pendingSave = PENDING_SAVES.remove(getDimensionKey(world));
        if (pendingSave != null) {
            saveOverlays(pendingSave.world, pendingSave.overlays);
        }
    }

    public static void flushAllPendingSaves() {
        for (PendingSave pendingSave : PENDING_SAVES.values()) {
            saveOverlays(pendingSave.world, pendingSave.overlays);
        }

        PENDING_SAVES.clear();
    }

    public static Map<BlockPos, IBlockState> loadOverlays(World world) {
        Map<BlockPos, IBlockState> overlays = new HashMap<BlockPos, IBlockState>();

        if (world == null) {
            return overlays;
        }

        if (world.isRemote || !(world instanceof WorldServer)) {
            return overlays;
        }

        try {
            WorldServer serverWorld = (WorldServer) world;
            WorldServer rootWorld = serverWorld.getMinecraftServer().getWorld(0);
            File worldDir = rootWorld != null ? rootWorld.getSaveHandler().getWorldDirectory() : serverWorld.getSaveHandler().getWorldDirectory();
            File saveDir = new File(worldDir, "data");
            String fileName = getSaveFileName(world);
            File saveFile = new File(saveDir, fileName);

            if (!saveFile.exists()) {
                return overlays;
            }

            FileInputStream fis = new FileInputStream(saveFile);
            NBTTagCompound rootTag = CompressedStreamTools.readCompressed(fis);
            fis.close();

            if (rootTag.hasKey("overlays")) {
                NBTTagList overlayList = rootTag.getTagList("overlays", 10);

                for (int i = 0; i < overlayList.tagCount(); i++) {
                    NBTTagCompound overlayTag = overlayList.getCompoundTagAt(i);

                    int x = overlayTag.getInteger("x");
                    int y = overlayTag.getInteger("y");
                    int z = overlayTag.getInteger("z");
                    BlockPos pos = new BlockPos(x, y, z);
                    IBlockState state = null;

                    if (overlayTag.hasKey("stateId")) {
                        state = Block.getStateById(overlayTag.getInteger("stateId"));
                    }

                    if (state == null && overlayTag.hasKey("state")) {
                        try {
                            NBTTagCompound stateTag = overlayTag.getCompoundTag("state");
                            state = NBTUtil.readBlockState(stateTag);
                        } catch (Exception e) {
                            Underlay.LOGGER.warn("Failed to read block state from NBT", e);
                        }
                    }

                    if (state != null) {
                        overlays.put(pos, state);
                    }
                }
            }
        } catch (IOException e) {
            Underlay.LOGGER.error("Failed to load overlays", e);
        }

        return overlays;
    }

    private static String getDimensionKey(World world) {
        return Integer.toString(world.provider.getDimension());
    }

    private static class PendingSave {
        private final WorldServer world;
        private final Map<BlockPos, IBlockState> overlays;
        private int ticksUntilSave;

        private PendingSave(WorldServer world, Map<BlockPos, IBlockState> overlays, int ticksUntilSave) {
            this.world = world;
            this.overlays = overlays;
            this.ticksUntilSave = ticksUntilSave;
        }
    }
}
