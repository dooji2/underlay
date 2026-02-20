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

    private static long lastFullRefreshTime = 0;
    private static final long FULL_REFRESH_INTERVAL = 500;

    private static final boolean IS_IRIS_INSTALLED = FabricLoader.getInstance().isModLoaded("iris");

    public static void registerOverlay(BlockPos pos, BlockState state) {
        RENDER_CACHE.put(pos.toImmutable(), state);
    }

    public static void unregisterOverlay(BlockPos pos) {
        RENDER_CACHE.remove(pos);
    }

    public static void clearAllOverlays() {
        RENDER_CACHE.clear();
    }

    public static void forceRefresh() {
        lastFullRefreshTime = System.currentTimeMillis();
        clearAllOverlays();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            BlockPos playerPos = client.player.getBlockPos();
            int radius = 64;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -16; y <= 16; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        if (UnderlayManagerClient.hasOverlay(pos)) {
                            registerOverlay(pos, UnderlayManagerClient.getOverlay(pos));
                        }
                    }
                }
            }
        }
    }

    private static void checkForFullRefresh() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFullRefreshTime > FULL_REFRESH_INTERVAL) {
            forceRefresh();
        }
    }

    private static boolean isShadersActive() {
        if (IS_IRIS_INSTALLED) {
            return IrisHelper.isShaderPackInUse();
        }

        return false;
    }

    public static void renderOverlays(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, @Nullable ClientWorld world) {
        checkForFullRefresh();
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

                if (!UnderlayManagerClient.hasOverlay(pos)) {
                    RENDER_CACHE.remove(pos);
                    continue;
                }

                if (!(state.getBlock() instanceof BlockEntityProvider provider)) {
                    continue;
                }

                BlockEntity blockEntity = provider.createBlockEntity(pos, state);
                if (blockEntity == null) {
                    continue;
                }

                blockEntity.setWorld(world);
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
        for (Map.Entry<BlockPos, BlockState> entry : RENDER_CACHE.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            double distanceSq = pos.getSquaredDistance(client.player.getBlockPos());
            if (distanceSq > maxDistSq) {
                continue;
            }

            if (!UnderlayManagerClient.hasOverlay(pos)) {
                RENDER_CACHE.remove(pos);
                continue;
            }

            matrices.push();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            matrices.translate(0.5, 0.5, 0.5);
            matrices.scale(1.0001f, 1.0001f, 1.0001f);
            matrices.translate(-0.5, -0.5, -0.5);

            BlockStateModel model = blockRenderer.getModels().getModel(state);
            List<BlockModelPart> parts = new ArrayList<>();
            RANDOM.setSeed(state.getRenderingSeed(pos));
            model.addParts(RANDOM, parts);

            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getCutoutMipped());
            int light = WorldRenderer.getLightmapCoordinates(world, pos);
            if (useEntityRendering) {
                blockRenderer.renderBlockAsEntity(state, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
            } else {
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
}
