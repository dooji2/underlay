package com.dooji.underlay.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

public class UnderlayRenderer {
    private static final Map<BlockPos, BlockState> RENDER_CACHE = new ConcurrentHashMap<>();

    private static long lastFullRefreshTime = 0;
    private static final long FULL_REFRESH_INTERVAL = 500;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(UnderlayRenderer::onRenderLevel);
    }

    public static void registerOverlay(BlockPos pos, BlockState state) {
        RENDER_CACHE.put(pos.immutable(), state);
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

        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null) {
            BlockPos playerPos = client.player.blockPosition();
            int radius = 64;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -16; y <= 16; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = playerPos.offset(x, y, z);
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

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = client.getBlockRenderer();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();

        if (bufferSource == null || client.level == null || client.player == null) {
            return;
        }

        checkForFullRefresh();

        poseStack.pushPose();

        for (Map.Entry<BlockPos, BlockState> entry : RENDER_CACHE.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            int chunks = client.options.renderDistance().get();
            int blocks = chunks * 16;
            double maxDistSq = (double)blocks * blocks;
            double distanceSq = pos.distSqr(client.player.blockPosition());
            if (distanceSq > maxDistSq) {
                continue;
            }

            if (!UnderlayManagerClient.hasOverlay(pos)) {
                RENDER_CACHE.remove(pos);
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());
            int packedLight = LevelRenderer.getLightColor(client.level, pos);
            int packedOverlay = OverlayTexture.NO_OVERLAY;

            blockRenderer.renderSingleBlock(state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }

        bufferSource.endBatch(RenderType.cutoutMipped());
        poseStack.popPose();
    }
}
