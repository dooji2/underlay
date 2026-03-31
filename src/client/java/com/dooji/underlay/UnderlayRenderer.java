package com.dooji.underlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import com.dooji.underlay.mixin.client.WorldRendererAccessor;

public class UnderlayRenderer {
    private static final Random RANDOM = Random.create();
    private static final Map<BlockPos, BlockState> RENDER_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, CachedBlockEntity> BLOCK_ENTITY_CACHE = new ConcurrentHashMap<>();

    private static final boolean IS_IRIS_INSTALLED = FabricLoader.getInstance().isModLoaded("iris");

    public static void registerOverlay(BlockPos pos, BlockState state) {
        RENDER_CACHE.put(pos.toImmutable(), state);
        BLOCK_ENTITY_CACHE.remove(pos);
    }

    public static void unregisterOverlay(BlockPos pos) {
        RENDER_CACHE.remove(pos);
        BLOCK_ENTITY_CACHE.remove(pos);
    }

    public static void clearAllOverlays() {
        RENDER_CACHE.clear();
        BLOCK_ENTITY_CACHE.clear();
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

    public static void renderOverlays(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, @Nullable ClientWorld world) {
        renderOverlayBlocks(matrices, vertexConsumers, camera, world);
        renderOverlayBlockEntities(matrices, camera, world);

        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private static void renderOverlayBlocks(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, @Nullable ClientWorld world) {
        render(matrices, vertexConsumers, camera, world, false);
    }

    private static void renderOverlayBlockEntities(MatrixStack matrices, Camera camera, @Nullable ClientWorld world) {
        render(matrices, null, camera, world, true);
    }

    private static void render(MatrixStack matrices, @Nullable VertexConsumerProvider vertexConsumers, Camera camera, @Nullable ClientWorld world, boolean renderBlockEntities) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (world == null || client.player == null) {
            return;
        }

        if (!renderBlockEntities && vertexConsumers == null) {
            return;
        }

        Vec3d cameraPos = camera.getPos();
        int chunks = client.options.getViewDistance().getValue();
        int blocks = chunks * 16;
        double maxDistSq = (double)blocks * blocks;

        if (renderBlockEntities) {
            if (!(client.worldRenderer instanceof WorldRendererAccessor accessor)) {
                return;
            }

            RenderDispatcher renderDispatcher = accessor.getEntityRenderDispatcher();
            WorldRenderState worldRenderState = accessor.getWorldRenderState();
            CameraRenderState cameraRenderState = worldRenderState.cameraRenderState;
            OrderedRenderCommandQueueImpl queue = renderDispatcher.getQueue();
            BlockEntityRenderManager blockEntityRenderManager = client.getBlockEntityRenderDispatcher();
            blockEntityRenderManager.configure(camera);
            float tickProgress = client.getRenderTickCounter().getTickProgress(true);

            matrices.push();
            for (Map.Entry<BlockPos, BlockState> entry : RENDER_CACHE.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();

                double distanceSq = pos.getSquaredDistance(client.player.getBlockPos());
                if (distanceSq > maxDistSq) {
                    continue;
                }

                if (!(state.getBlock() instanceof BlockEntityProvider provider)) {
                    continue;
                }

                BlockEntity blockEntity = getOrCreateBlockEntity(world, pos, state, provider);
                if (blockEntity == null) {
                    continue;
                }

                BlockEntityRenderState blockEntityRenderState = blockEntityRenderManager.getRenderState(blockEntity, tickProgress, null);
                if (blockEntityRenderState == null) {
                    continue;
                }

                matrices.push();
                matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
                blockEntityRenderManager.render(blockEntityRenderState, matrices, queue, cameraRenderState);
                matrices.pop();
            }

            matrices.pop();
            renderDispatcher.render();
            return;
        }

        BlockRenderManager blockRenderer = client.getBlockRenderManager();
        boolean useEntityRendering = isShadersActive();

        matrices.push();
        List<BlockModelPart> parts = useEntityRendering ? null : new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : RENDER_CACHE.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            double distanceSq = pos.getSquaredDistance(client.player.getBlockPos());
            if (distanceSq > maxDistSq) {
                continue;
            }

            matrices.push();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            matrices.translate(0.5, 0.5, 0.5);
            matrices.scale(1.0001f, 1.0001f, 1.0001f);
            matrices.translate(-0.5, -0.5, -0.5);

            int light = WorldRenderer.getLightmapCoordinates(world, pos);
            if (useEntityRendering) {
                blockRenderer.renderBlockAsEntity(state, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
            } else {
                BlockStateModel model = blockRenderer.getModels().getModel(state);
                parts.clear();
                RANDOM.setSeed(state.getRenderingSeed(pos));
                model.addParts(RANDOM, parts);

                VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getCutoutMipped());
                blockRenderer.renderBlock(state, pos, world, matrices, buffer, true, parts);
            }

            matrices.pop();
        }

        matrices.pop();
    }

    private static class IrisHelper {
        public static boolean isShaderPackInUse() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
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
