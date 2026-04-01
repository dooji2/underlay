package com.dooji.underlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class UnderlayRenderer {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<BlockPos, BlockState> RENDER_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, CachedBlockEntity> BLOCK_ENTITY_CACHE = new ConcurrentHashMap<>();

    private static final boolean IS_IRIS_INSTALLED = FabricLoader.getInstance().isModLoaded("iris");

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(UnderlayRenderer::renderOverlayBlockEntities);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(UnderlayRenderer::renderOverlayBlocks);
    }

    public static void registerOverlay(BlockPos pos, BlockState state) {
        RENDER_CACHE.put(pos.immutable(), state);
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

    private static PoseStack getMatrices(WorldRenderContext context) {
        PoseStack matrices = context.matrices();
        return matrices != null ? matrices : new PoseStack();
    }

    private static void renderOverlayBlocks(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = getMatrices(context);
        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        LevelRenderState worldState = context.worldState();
        Set<RenderType> usedRenderTypes = new HashSet<>();

        render(matrices, bufferSource, worldState, false, null, usedRenderTypes);

        for (RenderType usedRenderType : usedRenderTypes) {
            bufferSource.endBatch(usedRenderType);
        }
    }

    private static void renderOverlayBlockEntities(WorldRenderContext context) {
        render(context, false, true);
    }

    private static void render(WorldRenderContext context, boolean requireContextMatrices, boolean renderBlockEntities) {
        PoseStack matrices = requireContextMatrices ? context.matrices() : getMatrices(context);
        MultiBufferSource vertexConsumers = context.consumers();
        LevelRenderState worldState = context.worldState();
        render(matrices, vertexConsumers, worldState, renderBlockEntities, context.commandQueue(), null);
    }

    private static void render(PoseStack matrices, MultiBufferSource vertexConsumers, LevelRenderState worldState, boolean renderBlockEntities, SubmitNodeCollector commandQueue, Set<RenderType> usedRenderTypes) {
        Minecraft client = Minecraft.getInstance();
        Vec3 cameraPos = worldState != null ? worldState.cameraRenderState.pos : client.gameRenderer.getMainCamera().position();

        if (matrices == null || vertexConsumers == null || client.level == null || client.player == null) {
            return;
        }

        if (renderBlockEntities && worldState == null) {
            return;
        }

        BlockRenderDispatcher blockRenderer = client.getBlockRenderer();
        boolean useEntityRendering = isShadersActive();

        ClientLevel world = client.level;
        BlockEntityRenderDispatcher blockEntityRenderDispatcher = client.getBlockEntityRenderDispatcher();
        if (renderBlockEntities) {
            blockEntityRenderDispatcher.prepare(client.gameRenderer.getMainCamera());
        }

        matrices.pushPose();
        List<BlockModelPart> parts = useEntityRendering ? null : new ArrayList<>();

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

            matrices.pushPose();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            if (!renderBlockEntities) {
                matrices.translate(0.5, 0.5, 0.5);
                matrices.scale(1.0001f, 1.0001f, 1.0001f);
                matrices.translate(-0.5, -0.5, -0.5);

                int light = LevelRenderer.getLightColor(world, pos);
                if (useEntityRendering) {
                    RenderType layer = ItemBlockRenderTypes.getRenderType(state);
                    blockRenderer.renderSingleBlock(state, matrices, vertexConsumers, light, OverlayTexture.NO_OVERLAY);
                    if (usedRenderTypes != null) {
                        usedRenderTypes.add(layer);
                    }
                } else {
                    BlockStateModel model = blockRenderer.getBlockModelShaper().getBlockModel(state);
                    parts.clear();
                    RANDOM.setSeed(state.getSeed(pos));
                    model.collectParts(RANDOM, parts);

                    RenderType layer = ItemBlockRenderTypes.getMovingBlockRenderType(state);
                    VertexConsumer buffer = vertexConsumers.getBuffer(layer);
                    blockRenderer.renderBatched(state, pos, world, matrices, buffer, true, parts);
                    if (usedRenderTypes != null) {
                        usedRenderTypes.add(layer);
                    }
                }
            } else if (commandQueue != null && state.getBlock() instanceof EntityBlock entityBlock) {
                BlockEntity blockEntity = getOrCreateBlockEntity(world, pos, state, entityBlock);
                if (blockEntity != null) {
                    BlockEntityRenderState blockEntityRenderState = blockEntityRenderDispatcher.tryExtractRenderState(blockEntity, client.getDeltaTracker().getGameTimeDeltaTicks(), null);
                    if (blockEntityRenderState != null) {
                        blockEntityRenderDispatcher.submit(blockEntityRenderState, matrices, commandQueue, worldState.cameraRenderState);
                    }
                }
            }

            matrices.popPose();
        }

        matrices.popPose();
    }

    private static class IrisHelper {
        public static boolean isShaderPackInUse() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
    }

    private static BlockEntity getOrCreateBlockEntity(ClientLevel world, BlockPos pos, BlockState state, EntityBlock entityBlock) {
        CachedBlockEntity cached = BLOCK_ENTITY_CACHE.get(pos);
        if (cached != null && cached.state.equals(state)) {
            if (cached.blockEntity.getLevel() != world) {
                cached.blockEntity.setLevel(world);
            }
            
            return cached.blockEntity;
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(pos, state);
        if (blockEntity == null) {
            BLOCK_ENTITY_CACHE.remove(pos);
            return null;
        }

        blockEntity.setLevel(world);
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
