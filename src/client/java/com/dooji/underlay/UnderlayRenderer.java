package com.dooji.underlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import org.jetbrains.annotations.Nullable;

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
        MinecraftClient client = MinecraftClient.getInstance();
        if (world == null || client.player == null) {
            return;
        }

        BlockRenderManager blockRenderer = client.getBlockRenderManager();
        Vec3d cameraPos = camera.getPos();
        boolean useEntityRendering = isShadersActive();

        checkForFullRefresh();

        matrices.push();

        for (Map.Entry<BlockPos, BlockState> entry : RENDER_CACHE.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            int chunks = client.options.getViewDistance().getValue();
            int blocks = chunks * 16;
            double maxDistSq = (double)blocks * blocks;
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

            BlockStateModel model = blockRenderer.getModels().getModel(state);
            List<BlockModelPart> parts = new ArrayList<>();
            RANDOM.setSeed(state.getRenderingSeed(pos));
            model.addParts(RANDOM, parts);

            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getCutoutMipped());
            int light = WorldRenderer.getLightmapCoordinates(world, pos);
            if (useEntityRendering) {
                blockRenderer.renderBlockAsEntity(
                        state,
                        matrices,
                        vertexConsumers,
                        light,
                        OverlayTexture.DEFAULT_UV
                );
            } else {
                blockRenderer.renderBlock(state, pos, world, matrices, buffer, true, parts);
            }

            matrices.pop();
        }

        matrices.pop();
        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private static class IrisHelper {
        public static boolean isShaderPackInUse() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
