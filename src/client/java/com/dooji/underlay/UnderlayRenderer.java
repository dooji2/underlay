package com.dooji.underlay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class UnderlayRenderer {
    private static final Random RANDOM = Random.create();
    private static final Map<BlockPos, BlockState> RENDER_CACHE = new ConcurrentHashMap<>();

    private static long lastFullRefreshTime = 0;
    private static final long FULL_REFRESH_INTERVAL = 500;

    public static void init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(UnderlayRenderer::renderOverlays);
    }

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

    static {
        IS_IRIS_INSTALLED = FabricLoader.getInstance().isModLoaded("iris");
    }
    private static final boolean IS_IRIS_INSTALLED;
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

    private static void renderOverlays(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockRenderManager blockRenderer = client.getBlockRenderManager();
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider vertexConsumers = context.consumers();
        Vec3d cameraPos = context.camera().getPos();
        boolean useEntityRendering = isShadersActive();

        if (vertexConsumers == null || context.world() == null || client.player == null) {
            return;
        }

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
            matrices.translate(0.5, 0.5, 0.5);
            matrices.scale(1.0001f, 1.0001f, 1.0001f);
            matrices.translate(-0.5, -0.5, -0.5);

            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getCutoutMipped());
            int light = WorldRenderer.getLightmapCoordinates(context.world(), pos);
            if (useEntityRendering) {
                blockRenderer.renderBlockAsEntity(
                        state,
                        matrices,
                        vertexConsumers,
                        light,
                        OverlayTexture.DEFAULT_UV
                );
            } else blockRenderer.renderBlock(state, pos, context.world(), matrices, buffer, true, RANDOM);

            matrices.pop();
        }

        matrices.pop();
    }
}
