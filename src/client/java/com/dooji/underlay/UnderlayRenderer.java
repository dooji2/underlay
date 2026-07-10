package com.dooji.underlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockEntityProvider;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class UnderlayRenderer {
    private static final Random RANDOM = Random.create();
    private static final Map<BlockPos, BlockState> RENDER_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, CachedBlockEntity> BLOCK_ENTITY_CACHE = new ConcurrentHashMap<>();
    private static final Set<BlockPos> BLOCK_ENTITY_OVERLAYS = ConcurrentHashMap.newKeySet();

    private static final boolean IS_IRIS_INSTALLED = FabricLoader.getInstance().isModLoaded("iris");

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(UnderlayRenderer::renderOverlayBlockEntities);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(UnderlayRenderer::renderOverlayBlocks);
    }

    public static void registerOverlay(BlockPos pos, BlockState state) {
        BlockPos immutablePos = pos.toImmutable();
        RENDER_CACHE.put(immutablePos, state);

        if (state.getBlock() instanceof BlockEntityProvider) {
            BLOCK_ENTITY_OVERLAYS.add(immutablePos);
        } else {
            BLOCK_ENTITY_OVERLAYS.remove(immutablePos);
        }

        BLOCK_ENTITY_CACHE.remove(immutablePos);
    }

    public static void unregisterOverlay(BlockPos pos) {
        RENDER_CACHE.remove(pos);
        BLOCK_ENTITY_CACHE.remove(pos);
        BLOCK_ENTITY_OVERLAYS.remove(pos);
    }

    public static void clearAllOverlays() {
        RENDER_CACHE.clear();
        BLOCK_ENTITY_CACHE.clear();
        BLOCK_ENTITY_OVERLAYS.clear();
    }

    public static void forceRefresh() {
        clearAllOverlays();
        UnderlayManagerClient.getAll().forEach(UnderlayRenderer::registerOverlay);
    }

    private static boolean isShadersActive() {
        if (IS_IRIS_INSTALLED) {
            return IrisHelper.isShaderPackInUse();
        }

        return false;
    }

    private static class IrisHelper {
        public static boolean isShaderPackInUse() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
    }

    private static MatrixStack getMatrices(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        return matrices != null ? matrices : new MatrixStack();
    }

    private static void renderOverlayBlocks(WorldRenderContext context) {
        if (RENDER_CACHE.isEmpty()) {
            return;
        }

        renderBlocks(context);
    }

    private static void renderOverlayBlockEntities(WorldRenderContext context) {
        if (BLOCK_ENTITY_OVERLAYS.isEmpty()) {
            return;
        }

        renderBlockEntities(context);
    }

    private static void renderBlocks(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider vertexConsumers = context.consumers();
        Vec3d cameraPos = context.camera().getPos();
        if (matrices == null || vertexConsumers == null || context.world() == null || client.player == null) {
            return;
        }

        int chunks = client.options.getViewDistance().getValue();
        int blocks = chunks * 16;
        double maxDistSq = (double)blocks * blocks;
        BlockRenderManager blockRenderer = client.getBlockRenderManager();
        boolean useEntityRendering = isShadersActive();

        matrices.push();
        List<BlockPos> stalePositions = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : RENDER_CACHE.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            double distanceSq = pos.getSquaredDistance(client.player.getBlockPos());
            if (distanceSq > maxDistSq) {
                continue;
            }

            if (!UnderlayManagerClient.hasOverlay(pos)) {
                stalePositions.add(pos);
                continue;
            }

            matrices.push();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            matrices.translate(0.5, 0.5, 0.5);
            matrices.scale(1.0001f, 1.0001f, 1.0001f);
            matrices.translate(-0.5, -0.5, -0.5);

            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getCutoutMipped());
            int light = WorldRenderer.getLightmapCoordinates(context.world(), pos);
            if (useEntityRendering) {
                blockRenderer.renderBlockAsEntity(state, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
            } else {
                blockRenderer.renderBlock(state, pos, context.world(), matrices, buffer, true, RANDOM);
            }

            matrices.pop();
        }

        matrices.pop();

        for (BlockPos stalePos : stalePositions) {
            unregisterOverlay(stalePos);
        }
    }

    private static void renderBlockEntities(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        MatrixStack matrices = getMatrices(context);
        VertexConsumerProvider vertexConsumers = context.consumers();
        Vec3d cameraPos = context.camera().getPos();
        if (vertexConsumers == null || context.world() == null || client.player == null) {
            return;
        }

        int chunks = client.options.getViewDistance().getValue();
        int blocks = chunks * 16;
        double maxDistSq = (double)blocks * blocks;

        matrices.push();
        for (BlockPos pos : BLOCK_ENTITY_OVERLAYS) {
            BlockState state = RENDER_CACHE.get(pos);
            if (state == null) {
                BLOCK_ENTITY_OVERLAYS.remove(pos);
                continue;
            }

            double distanceSq = pos.getSquaredDistance(client.player.getBlockPos());
            if (distanceSq > maxDistSq) {
                continue;
            }

            if (!(state.getBlock() instanceof BlockEntityProvider provider)) {
                BLOCK_ENTITY_OVERLAYS.remove(pos);
                continue;
            }

            BlockEntity blockEntity = getOrCreateBlockEntity(context.world(), pos, state, provider);
            if (blockEntity == null) {
                continue;
            }

            matrices.push();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            client.getBlockEntityRenderDispatcher().render(blockEntity, client.getRenderTickCounter().getTickDelta(true), matrices, vertexConsumers);
            matrices.pop();
        }

        matrices.pop();
    }

    private static BlockEntity getOrCreateBlockEntity(ClientWorld world, BlockPos pos, BlockState state, BlockEntityProvider provider) {
        CachedBlockEntity cached = BLOCK_ENTITY_CACHE.get(pos);
        if (cached != null && cached.state.equals(state)) {
            if (cached.blockEntity.getWorld() != world) {
                cached.blockEntity.setWorld(world);
            }

            return cached.blockEntity;
        }

        BlockEntity blockEntity = provider.createBlockEntity(pos, state);
        if (blockEntity == null) {
            BLOCK_ENTITY_CACHE.remove(pos);
            return null;
        }

        blockEntity.setWorld(world);
        BLOCK_ENTITY_CACHE.put(pos.toImmutable(), new CachedBlockEntity(state, blockEntity));
        return blockEntity;
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
