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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.NeoForge;

public class UnderlayRenderer {
    private static final Map<BlockPos, BlockState> RENDER_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, CachedBlockEntity> BLOCK_ENTITY_CACHE = new ConcurrentHashMap<>();
    private static final Set<BlockPos> BLOCK_ENTITY_OVERLAYS = ConcurrentHashMap.newKeySet();

    public static void init() {
        NeoForge.EVENT_BUS.addListener(UnderlayRenderer::onRenderLevel);
    }

    public static void registerOverlay(BlockPos pos, BlockState state) {
        BlockPos immutablePos = pos.immutable();
        RENDER_CACHE.put(immutablePos, state);

        if (state.getBlock() instanceof EntityBlock) {
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

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (RENDER_CACHE.isEmpty()) {
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            renderBlocks(event);
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES && !BLOCK_ENTITY_OVERLAYS.isEmpty()) {
            renderBlockEntities(event);
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

    private static void renderBlocks(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        PoseStack poseStack = getPoseStack(event, true);

        if (poseStack == null || client.level == null || client.player == null) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
        BlockRenderDispatcher blockRenderer = client.getBlockRenderer();
        RandomSource randomSource = RandomSource.create();
        RandomSource renderTypeRandom = RandomSource.create();
        Set<RenderType> usedRenderTypes = new HashSet<>();
        BlockPos playerPos = client.player.blockPosition();
        int blocks = client.options.renderDistance().get() * 16;
        double maxDistSq = (double)blocks * blocks;

        poseStack.pushPose();

        for (Map.Entry<BlockPos, BlockState> entry : RENDER_CACHE.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.distSqr(playerPos) > maxDistSq) {
                continue;
            }

            BlockState state = entry.getValue();
            BakedModel model = blockRenderer.getBlockModel(state);
            ModelData modelData = ModelData.EMPTY;
            long seed = state.getSeed(pos);
            renderTypeRandom.setSeed(seed);

            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.scale(1.0001F, 1.0001F, 1.0001F);
            poseStack.translate(-0.5D, -0.5D, -0.5D);

            for (RenderType chunkRenderType : model.getRenderTypes(state, renderTypeRandom, modelData)) {
                RenderType movingRenderType = RenderTypeHelper.getMovingBlockRenderType(chunkRenderType);
                VertexConsumer vertexConsumer = bufferSource.getBuffer(movingRenderType);

                blockRenderer.renderBatched(state, pos, client.level, poseStack, vertexConsumer, false, randomSource, modelData, chunkRenderType);
                usedRenderTypes.add(movingRenderType);
            }

            poseStack.popPose();
        }

        for (RenderType usedType : usedRenderTypes) {
            bufferSource.endBatch(usedType);
        }

        poseStack.popPose();
    }

    private static void renderBlockEntities(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        PoseStack poseStack = getPoseStack(event, false);

        if (poseStack == null || client.level == null || client.player == null) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
        BlockEntityRenderDispatcher blockEntityRenderer = client.getBlockEntityRenderDispatcher();
        BlockPos playerPos = client.player.blockPosition();
        int blocks = client.options.renderDistance().get() * 16;
        double maxDistSq = (double)blocks * blocks;
        boolean rendered = false;

        poseStack.pushPose();

        for (BlockPos pos : BLOCK_ENTITY_OVERLAYS) {
            BlockState state = RENDER_CACHE.get(pos);
            if (state == null) {
                BLOCK_ENTITY_OVERLAYS.remove(pos);
                continue;
            }

            if (pos.distSqr(playerPos) > maxDistSq) {
                continue;
            }

            if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
                BLOCK_ENTITY_OVERLAYS.remove(pos);
                continue;
            }

            BlockEntity blockEntity = getOrCreateBlockEntity(pos, state, entityBlock);
            if (blockEntity == null) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            blockEntityRenderer.render(blockEntity, 0.0F, poseStack, bufferSource);
            poseStack.popPose();
            rendered = true;
        }

        if (rendered) {
            bufferSource.endBatch();
        }

        poseStack.popPose();
    }

    private static BlockEntity getOrCreateBlockEntity(BlockPos pos, BlockState state, EntityBlock entityBlock) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }

        CachedBlockEntity cached = BLOCK_ENTITY_CACHE.get(pos);
        if (cached != null && cached.state.equals(state)) {
            if (cached.blockEntity.getLevel() != client.level) {
                cached.blockEntity.setLevel(client.level);
            }

            return cached.blockEntity;
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(pos, state);
        if (blockEntity == null) {
            BLOCK_ENTITY_CACHE.remove(pos);
            return null;
        }

        blockEntity.setLevel(client.level);
        BLOCK_ENTITY_CACHE.put(pos.immutable(), new CachedBlockEntity(state, blockEntity));
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
