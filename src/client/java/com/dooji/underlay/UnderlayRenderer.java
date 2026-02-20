package com.dooji.underlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static long lastFullRefreshTime = 0;
    private static final long FULL_REFRESH_INTERVAL = 500;

    private static final boolean IS_IRIS_INSTALLED = FabricLoader.getInstance().isModLoaded("iris");

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(UnderlayRenderer::renderOverlayBlockEntities);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(UnderlayRenderer::renderOverlayBlocks);
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
        render(context, true, false);
    }

    private static void renderOverlayBlockEntities(WorldRenderContext context) {
        render(context, false, true);
    }

    private static void render(WorldRenderContext context, boolean requireContextMatrices, boolean renderBlockEntities) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = requireContextMatrices ? context.matrices() : getMatrices(context);
        MultiBufferSource vertexConsumers = context.consumers();
        LevelRenderState worldState = context.worldState();
        Vec3 cameraPos = worldState != null ? worldState.cameraRenderState.pos : client.gameRenderer.getMainCamera().position();

        if ((requireContextMatrices && matrices == null) || vertexConsumers == null || client.level == null || client.player == null) {
            return;
        }

        if (renderBlockEntities && worldState == null) {
            return;
        }

        BlockRenderDispatcher blockRenderer = client.getBlockRenderer();
        boolean useEntityRendering = isShadersActive();

        ClientLevel world = client.level;
        checkForFullRefresh();
        SubmitNodeCollector commandQueue = context.commandQueue();
        BlockEntityRenderDispatcher blockEntityRenderDispatcher = client.getBlockEntityRenderDispatcher();
        if (renderBlockEntities) {
            blockEntityRenderDispatcher.prepare(client.gameRenderer.getMainCamera());
        }

        matrices.pushPose();

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

            matrices.pushPose();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            if (!renderBlockEntities) {
                matrices.translate(0.5, 0.5, 0.5);
                matrices.scale(1.0001f, 1.0001f, 1.0001f);
                matrices.translate(-0.5, -0.5, -0.5);

                BlockStateModel model = blockRenderer.getBlockModelShaper().getBlockModel(state);
                List<BlockModelPart> parts = new ArrayList<>();
                RANDOM.setSeed(state.getSeed(pos));
                model.collectParts(RANDOM, parts);

                RenderType layer = ItemBlockRenderTypes.getMovingBlockRenderType(state);
                VertexConsumer buffer = vertexConsumers.getBuffer(layer);
                int light = LevelRenderer.getLightColor(world, pos);
                if (useEntityRendering) {
                    blockRenderer.renderSingleBlock(state, matrices, vertexConsumers, light, OverlayTexture.NO_OVERLAY);
                } else {
                    blockRenderer.renderBatched(state, pos, world, matrices, buffer, true, parts);
                }
            } else if (commandQueue != null && state.getBlock() instanceof EntityBlock entityBlock) {
                BlockEntity blockEntity = entityBlock.newBlockEntity(pos, state);
                if (blockEntity != null) {
                    blockEntity.setLevel(world);
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
}
