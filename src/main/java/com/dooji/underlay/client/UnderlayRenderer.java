package com.dooji.underlay.client;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
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
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            render(event, false);
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            render(event, true);
            return;
        }
    }

    private static PoseStack getPoseStack(RenderLevelStageEvent event, boolean requireEventPoseStack) {
        PoseStack poseStack = event.getPoseStack();
        if (poseStack != null || requireEventPoseStack) {
            return poseStack;
        }

        return new PoseStack();
    }

    private static void render(RenderLevelStageEvent event, boolean renderBlockEntities) {
        Minecraft client = Minecraft.getInstance();
        PoseStack poseStack = getPoseStack(event, !renderBlockEntities);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
        BlockRenderDispatcher blockRenderer = client.getBlockRenderer();
        BlockEntityRenderDispatcher blockEntityRenderer = client.getBlockEntityRenderDispatcher();
        RandomSource randomSource = RandomSource.create();
        RandomSource renderTypeRandom = RandomSource.create();
        Set<RenderType> usedRenderTypes = new HashSet<>();

        if (poseStack == null || bufferSource == null || client.level == null || client.player == null) {
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
            if (!renderBlockEntities) {
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(1.0001f, 1.0001f, 1.0001f);
                poseStack.translate(-0.5, -0.5, -0.5);

                BakedModel model = blockRenderer.getBlockModel(state);
                ModelData modelData = ModelData.EMPTY;
                renderTypeRandom.setSeed(42L);

                for (RenderType chunkRenderType : model.getRenderTypes(state, renderTypeRandom, modelData)) {
                    RenderType movingRenderType = RenderTypeHelper.getMovingBlockRenderType(chunkRenderType);
                    VertexConsumer vertexConsumer = bufferSource.getBuffer(movingRenderType);

                    blockRenderer.renderBatched(state, pos, client.level, poseStack, vertexConsumer, false, randomSource, modelData, chunkRenderType);
                    usedRenderTypes.add(movingRenderType);
                }
            } else if (state.getBlock() instanceof EntityBlock entityBlock) {
                var blockEntity = entityBlock.newBlockEntity(pos, state);
                if (blockEntity != null) {
                    blockEntity.setLevel(client.level);
                    blockEntityRenderer.render(blockEntity, 0.0F, poseStack, bufferSource);
                }
            }

            poseStack.popPose();
        }

        if (!renderBlockEntities) {
            for (RenderType usedType : usedRenderTypes) {
                bufferSource.endBatch(usedType);
            }
        } else {
            bufferSource.endBatch();
        }

        poseStack.popPose();
    }
}
