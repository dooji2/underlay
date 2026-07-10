package com.dooji.underlay;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.impl.client.indigo.renderer.accessor.AccessChunkRendererRegion;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UnderlayRenderer {
    private static final Map<Long, Map<BlockPos, BlockState>> SECTION_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, BlockState> BE_STATES = new ConcurrentHashMap<>();
    private static final Map<BlockPos, CachedBlockEntity> BE_CACHE = new ConcurrentHashMap<>();

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(UnderlayRenderer::renderOverlayBlockEntities);
    }

    public static void registerOverlay(BlockPos pos, BlockState state) {
        BlockPos immutablePos = pos.immutable();
        long section = SectionPos.asLong(immutablePos);
        SECTION_CACHE.computeIfAbsent(section, key -> new ConcurrentHashMap<>()).put(immutablePos, state);

        if (state.getBlock() instanceof EntityBlock) {
            BE_STATES.put(immutablePos, state);
        } else {
            BE_STATES.remove(immutablePos);
        }

        BE_CACHE.remove(immutablePos);
        markSectionDirty(section);
    }

    public static void unregisterOverlay(BlockPos pos) {
        long section = SectionPos.asLong(pos);
        Map<BlockPos, BlockState> overlays = SECTION_CACHE.get(section);
        if (overlays != null) {
            overlays.remove(pos);
            if (overlays.isEmpty()) {
                SECTION_CACHE.remove(section, overlays);
            }
        }

        BE_CACHE.remove(pos);
        BE_STATES.remove(pos);
        markSectionDirty(section);
    }

    public static void clearAllOverlays() {
        SECTION_CACHE.clear();
        BE_STATES.clear();
        BE_CACHE.clear();
    }

    public static void forceRefresh() {
        Set<Long> dirtySections = new HashSet<>(SECTION_CACHE.keySet());
        clearAllOverlays();
        UnderlayManagerClient.getAll().forEach((pos, state) -> {
            registerOverlay(pos, state);
            dirtySections.remove(SectionPos.asLong(pos));
        });
        dirtySections.forEach(UnderlayRenderer::markSectionDirty);
    }

    public static Map<BlockPos, BlockState> getSectionOverlays(long section) {
        Map<BlockPos, BlockState> overlays = SECTION_CACHE.get(section);
        if (overlays == null || overlays.isEmpty()) {
            return Map.of();
        }

        Map<BlockPos, BlockState> sectionOverlays = new HashMap<>();
        overlays.forEach((pos, state) -> {
            if (state.getRenderShape() == RenderShape.MODEL) {
                sectionOverlays.put(pos, state);
            }
        });
        return sectionOverlays;
    }

    public static void renderSectionOverlays(RenderSectionRegion region, PoseStack poseStack, BlockRenderDispatcher blockRenderer, Map<BlockPos, BlockState> overlays) {
        TerrainRenderContext renderer = ((AccessChunkRendererRegion) region).fabric_getRenderer();

        for (Map.Entry<BlockPos, BlockState> overlay : overlays.entrySet()) {
            BlockPos pos = overlay.getKey();
            BlockState state = overlay.getValue();
            int x = SectionPos.sectionRelative(pos.getX());
            int y = SectionPos.sectionRelative(pos.getY());
            int z = SectionPos.sectionRelative(pos.getZ());

            poseStack.pushPose();

            try {
                poseStack.translate(x + 0.5D, y + 0.5D, z + 0.5D);
                poseStack.scale(1.0001F, 1.0001F, 1.0001F);
                poseStack.translate(-x - 0.5D, -y - 0.5D, -z - 0.5D);
                renderer.bufferModel(blockRenderer.getBlockModel(state), state, pos);
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static PoseStack getMatrices(WorldRenderContext context) {
        PoseStack matrices = context.matrices();
        return matrices != null ? matrices : new PoseStack();
    }

    private static void renderOverlayBlockEntities(WorldRenderContext context) {
        if (BE_STATES.isEmpty()) {
            return;
        }

        renderBlockEntities(context);
    }

    private static void renderBlockEntities(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = getMatrices(context);
        LevelRenderState worldState = context.worldState();
        SubmitNodeCollector commandQueue = context.commandQueue();
        ClientLevel world = client.level;
        if (worldState == null || commandQueue == null || world == null || client.player == null) {
            return;
        }

        Vec3 cameraPos = worldState.cameraRenderState.pos;
        int chunks = client.options.renderDistance().get();
        int blocks = chunks * 16;
        double maxDistSq = (double)blocks * blocks;
        BlockEntityRenderDispatcher blockEntityRenderer = client.getBlockEntityRenderDispatcher();
        blockEntityRenderer.prepare(client.gameRenderer.getMainCamera());

        matrices.pushPose();
        for (Map.Entry<BlockPos, BlockState> overlay : BE_STATES.entrySet()) {
            BlockPos pos = overlay.getKey();
            BlockState state = overlay.getValue();
            if (pos.distSqr(client.player.blockPosition()) > maxDistSq) {
                continue;
            }

            if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
                BE_STATES.remove(pos, state);
                continue;
            }

            BlockEntity blockEntity = getOrCreateBlockEntity(world, pos, state, entityBlock);
            if (blockEntity == null) {
                continue;
            }

            matrices.pushPose();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            BlockEntityRenderState blockEntityRenderState = blockEntityRenderer.tryExtractRenderState(blockEntity, client.getDeltaTracker().getGameTimeDeltaTicks(), null);
            if (blockEntityRenderState != null) {
                blockEntityRenderer.submit(blockEntityRenderState, matrices, commandQueue, worldState.cameraRenderState);
            }
            matrices.popPose();
        }

        matrices.popPose();
    }

    private static BlockEntity getOrCreateBlockEntity(ClientLevel world, BlockPos pos, BlockState state, EntityBlock entityBlock) {
        CachedBlockEntity cached = BE_CACHE.get(pos);
        if (cached != null && cached.state.equals(state)) {
            if (cached.blockEntity.getLevel() != world) {
                cached.blockEntity.setLevel(world);
            }

            return cached.blockEntity;
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(pos, state);
        if (blockEntity == null) {
            BE_CACHE.remove(pos);
            return null;
        }

        blockEntity.setLevel(world);
        BE_CACHE.put(pos.immutable(), new CachedBlockEntity(state, blockEntity));
        return blockEntity;
    }

    private static void markSectionDirty(long section) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        BlockPos playerPos = client.player.blockPosition();
        int sectionX = SectionPos.x(section);
        int sectionY = SectionPos.y(section);
        int sectionZ = SectionPos.z(section);
        int playerSectionX = SectionPos.blockToSectionCoord(playerPos.getX());
        int playerSectionZ = SectionPos.blockToSectionCoord(playerPos.getZ());
        int renderDistance = client.options.renderDistance().get();
        if (sectionY < client.level.getMinSectionY() || sectionY >= client.level.getMaxSectionY()) {
            return;
        }

        if (Math.abs(sectionX - playerSectionX) > renderDistance || Math.abs(sectionZ - playerSectionZ) > renderDistance) {
            return;
        }

        client.levelRenderer.setSectionDirty(sectionX, sectionY, sectionZ);
    }

    private static class CachedBlockEntity {
        private final BlockState state;
        private final BlockEntity blockEntity;

        private CachedBlockEntity(BlockState state, BlockEntity blockEntity) {
            this.state = state;
            this.blockEntity = blockEntity;
        }
    }
}
