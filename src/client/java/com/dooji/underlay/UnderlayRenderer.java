package com.dooji.underlay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class UnderlayRenderer {
    private static final Map<Long, Map<BlockPos, BlockState>> SECTION_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, BlockState> BE_STATES = new ConcurrentHashMap<>();
    private static final Map<BlockPos, CachedBlockEntity> BE_CACHE = new ConcurrentHashMap<>();

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(UnderlayRenderer::renderOverlayBlockEntities);
    }

    public static void registerOverlay(BlockPos pos, BlockState state) {
        BlockPos immutablePos = pos.toImmutable();
        long section = ChunkSectionPos.toLong(immutablePos);
        SECTION_CACHE.computeIfAbsent(section, key -> new ConcurrentHashMap<>()).put(immutablePos, state);

        if (state.getBlock() instanceof BlockEntityProvider) {
            BE_STATES.put(immutablePos, state);
        } else {
            BE_STATES.remove(immutablePos);
        }

        BE_CACHE.remove(immutablePos);
        markSectionDirty(section);
    }

    public static void unregisterOverlay(BlockPos pos) {
        long section = ChunkSectionPos.toLong(pos);
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
            dirtySections.remove(ChunkSectionPos.toLong(pos));
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
            if (state.getRenderType() == BlockRenderType.MODEL) {
                sectionOverlays.put(pos, state);
            }
        });
        return sectionOverlays;
    }

    public static void renderSectionOverlays(BlockRenderView region, MatrixStack matrices, BlockRenderManager blockRenderer, Map<BlockPos, BlockState> overlays, Function<RenderLayer, VertexConsumer> buffers) {
        Random random = Random.create();

        for (Map.Entry<BlockPos, BlockState> overlay : overlays.entrySet()) {
            BlockPos pos = overlay.getKey();
            BlockState state = overlay.getValue();
            long seed = state.getRenderingSeed(pos);
            random.setSeed(seed);

            matrices.push();

            try {
                matrices.translate(
                    ChunkSectionPos.getLocalCoord(pos.getX()),
                    ChunkSectionPos.getLocalCoord(pos.getY()),
                    ChunkSectionPos.getLocalCoord(pos.getZ())
                );
                matrices.translate(0.5D, 0.5D, 0.5D);
                matrices.scale(1.0001F, 1.0001F, 1.0001F);
                matrices.translate(-0.5D, -0.5D, -0.5D);

                RenderLayer renderLayer = RenderLayers.getBlockLayer(state);
                VertexConsumer vertexConsumer = buffers.apply(renderLayer);
                blockRenderer.renderBlock(state, pos, region, matrices, vertexConsumer, true, random);
            } finally {
                matrices.pop();
            }
        }
    }

    private static MatrixStack getMatrices(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        return matrices != null ? matrices : new MatrixStack();
    }

    private static void renderOverlayBlockEntities(WorldRenderContext context) {
        if (BE_STATES.isEmpty()) {
            return;
        }

        renderBlockEntities(context);
    }

    private static void renderBlockEntities(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        MatrixStack matrices = getMatrices(context);
        VertexConsumerProvider vertexConsumers = context.consumers();
        Vec3d cameraPos = context.camera().getPos();
        ClientWorld world = context.world();
        if (vertexConsumers == null || world == null || client.player == null) {
            return;
        }

        int chunks = client.options.getViewDistance().getValue();
        int blocks = chunks * 16;
        double maxDistSq = (double)blocks * blocks;
        BlockEntityRenderDispatcher blockEntityRenderer = client.getBlockEntityRenderDispatcher();

        matrices.push();
        for (Map.Entry<BlockPos, BlockState> overlay : BE_STATES.entrySet()) {
            BlockPos pos = overlay.getKey();
            BlockState state = overlay.getValue();
            if (pos.getSquaredDistance(client.player.getBlockPos()) > maxDistSq) {
                continue;
            }

            if (!(state.getBlock() instanceof BlockEntityProvider provider)) {
                BE_STATES.remove(pos, state);
                continue;
            }

            BlockEntity blockEntity = getOrCreateBlockEntity(world, pos, state, provider);
            if (blockEntity == null) {
                continue;
            }

            matrices.push();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            blockEntityRenderer.render(blockEntity, client.getTickDelta(), matrices, vertexConsumers);
            matrices.pop();
        }

        matrices.pop();
    }

    private static BlockEntity getOrCreateBlockEntity(ClientWorld world, BlockPos pos, BlockState state, BlockEntityProvider provider) {
        CachedBlockEntity cached = BE_CACHE.get(pos);
        if (cached != null && cached.state.equals(state)) {
            if (cached.blockEntity.getWorld() != world) {
                cached.blockEntity.setWorld(world);
            }

            return cached.blockEntity;
        }

        BlockEntity blockEntity = provider.createBlockEntity(pos, state);
        if (blockEntity == null) {
            BE_CACHE.remove(pos);
            return null;
        }

        blockEntity.setWorld(world);
        BE_CACHE.put(pos.toImmutable(), new CachedBlockEntity(state, blockEntity));
        return blockEntity;
    }

    private static void markSectionDirty(long section) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        BlockPos playerPos = client.player.getBlockPos();
        int sectionX = ChunkSectionPos.unpackX(section);
        int sectionY = ChunkSectionPos.unpackY(section);
        int sectionZ = ChunkSectionPos.unpackZ(section);
        int playerSectionX = ChunkSectionPos.getSectionCoord(playerPos.getX());
        int playerSectionZ = ChunkSectionPos.getSectionCoord(playerPos.getZ());
        int renderDistance = client.options.getViewDistance().getValue();
        if (sectionY < client.world.getBottomSectionCoord() || sectionY >= client.world.getTopSectionCoord()) {
            return;
        }

        if (Math.abs(sectionX - playerSectionX) > renderDistance || Math.abs(sectionZ - playerSectionZ) > renderDistance) {
            return;
        }

        client.worldRenderer.scheduleBlockRenders(sectionX, sectionY, sectionZ);
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
