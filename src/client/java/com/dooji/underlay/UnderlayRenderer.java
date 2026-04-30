package com.dooji.underlay;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class UnderlayRenderer {
    private static final Map<BlockPos, BlockState> RENDER_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, CachedBlockEntity> BLOCK_ENTITY_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, OverlayMovingBlockRenderState> MOVING_BLOCK_CACHE = new ConcurrentHashMap<>();

    private static ModelBlockRenderer blockRenderer;
    private static boolean cachedAmbientOcclusion;
    private static boolean cachedCutoutLeaves;

    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(UnderlayRenderer::renderOverlays);
    }

    public static void registerOverlay(BlockPos pos, BlockState state) {
        RENDER_CACHE.put(pos.immutable(), state);
        BLOCK_ENTITY_CACHE.remove(pos);
        MOVING_BLOCK_CACHE.remove(pos);
    }

    public static void unregisterOverlay(BlockPos pos) {
        RENDER_CACHE.remove(pos);
        BLOCK_ENTITY_CACHE.remove(pos);
        MOVING_BLOCK_CACHE.remove(pos);
    }

    public static void clearAllOverlays() {
        RENDER_CACHE.clear();
        BLOCK_ENTITY_CACHE.clear();
        MOVING_BLOCK_CACHE.clear();
    }

    public static void forceRefresh() {
        clearAllOverlays();
        UnderlayManagerClient.getAll().forEach(UnderlayRenderer::registerOverlay);
    }

    private static PoseStack getMatrices(LevelRenderContext context) {
        PoseStack matrices = context.poseStack();
        return matrices != null ? matrices : new PoseStack();
    }

    private static void renderOverlays(LevelRenderContext context) {
        render(getMatrices(context), context.levelState(), context.submitNodeCollector());
    }

    private static void render(PoseStack matrices, LevelRenderState worldState, SubmitNodeCollector commandQueue) {
        Minecraft client = Minecraft.getInstance();

        if (RENDER_CACHE.isEmpty() || worldState == null || commandQueue == null || client.level == null || client.player == null) {
            return;
        }

        Vec3 cameraPos = worldState.cameraRenderState.pos;
        ClientLevel world = client.level;
        BlockStateModelSet blockStateModelSet = client.getModelManager().getBlockStateModelSet();
        OrderedSubmitNodeCollector orderedCommandQueue = commandQueue.order(0);
        BlockEntityRenderDispatcher blockEntityRenderDispatcher = client.getBlockEntityRenderDispatcher();
        blockEntityRenderDispatcher.prepare(cameraPos);
        boolean cutoutLeaves = client.options.cutoutLeaves().get();
        ModelBlockRenderer blockRenderer = getBlockRenderer(client);
        int chunks = client.options.renderDistance().get();
        int blocks = chunks * 16;
        double maxDistSq = (double) blocks * blocks;

        for (Map.Entry<BlockPos, BlockState> entry : RENDER_CACHE.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            double distanceSq = pos.distSqr(client.player.blockPosition());
            if (distanceSq > maxDistSq) {
                continue;
            }

            MovingBlockRenderState movingBlockRenderState = getOrCreateMovingBlockRenderState(world, pos, state);
            BlockStateModel model = blockStateModelSet.get(state);
            boolean forceOpaque = ModelBlockRenderer.forceOpaque(cutoutLeaves, state);
            matrices.pushPose();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            matrices.translate(0.5, 0.5, 0.5);
            matrices.scale(1.0001f, 1.0001f, 1.0001f);
            matrices.translate(-0.5, -0.5, -0.5);
            submitOverlayBlockLayer(orderedCommandQueue, matrices, blockRenderer, movingBlockRenderState, pos, state, model, ChunkSectionLayer.SOLID, RenderTypes.solidMovingBlock(), forceOpaque);
            submitOverlayBlockLayer(orderedCommandQueue, matrices, blockRenderer, movingBlockRenderState, pos, state, model, ChunkSectionLayer.CUTOUT, RenderTypes.cutoutMovingBlock(), forceOpaque);
            submitOverlayBlockLayer(orderedCommandQueue, matrices, blockRenderer, movingBlockRenderState, pos, state, model, ChunkSectionLayer.TRANSLUCENT, RenderTypes.translucentMovingBlock(), false);
            matrices.popPose();

            if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
                continue;
            }

            BlockEntity blockEntity = getOrCreateBlockEntity(world, pos, state, entityBlock);
            if (blockEntity == null) {
                continue;
            }

            BlockEntityRenderState blockEntityRenderState = blockEntityRenderDispatcher.tryExtractRenderState(
                blockEntity,
                client.getDeltaTracker().getGameTimeDeltaTicks(),
                null,
                false
            );
            if (blockEntityRenderState == null) {
                continue;
            }

            matrices.pushPose();
            matrices.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            blockEntityRenderDispatcher.submit(blockEntityRenderState, matrices, commandQueue, worldState.cameraRenderState);
            matrices.popPose();
        }
    }

    private static ModelBlockRenderer getBlockRenderer(Minecraft client) {
        boolean ambientOcclusion = client.options.ambientOcclusion().get();
        boolean cutoutLeaves = client.options.cutoutLeaves().get();

        if (blockRenderer == null || cachedAmbientOcclusion != ambientOcclusion || cachedCutoutLeaves != cutoutLeaves) {
            blockRenderer = new ModelBlockRenderer(ambientOcclusion, false, client.getBlockColors());
            cachedAmbientOcclusion = ambientOcclusion;
            cachedCutoutLeaves = cutoutLeaves;
        }

        return blockRenderer;
    }

    private static void submitOverlayBlockLayer(OrderedSubmitNodeCollector orderedCommandQueue, PoseStack matrices, ModelBlockRenderer blockRenderer, MovingBlockRenderState movingBlockRenderState, BlockPos pos, BlockState state, BlockStateModel model, ChunkSectionLayer targetLayer, RenderType renderType, boolean forceOpaque) {
        orderedCommandQueue.submitCustomGeometry(matrices, renderType, (pose, buffer) -> {
            PoseStack.Pose quadPose = new PoseStack.Pose();
            quadPose.set(pose);
            blockRenderer.tesselateBlock((x, y, z, quad, instance) -> {
                ChunkSectionLayer quadLayer = forceOpaque ? ChunkSectionLayer.SOLID : quad.materialInfo().layer();
                if (quadLayer != targetLayer) {
                    return;
                }

                quadPose.translate(x, y, z);
                buffer.putBakedQuad(quadPose, quad, instance);
                quadPose.translate(-x, -y, -z);
            }, 0.0F, 0.0F, 0.0F, movingBlockRenderState, pos, state, model, state.getSeed(pos));
        });
    }

    private static MovingBlockRenderState getOrCreateMovingBlockRenderState(ClientLevel world, BlockPos pos, BlockState state) {
        OverlayMovingBlockRenderState cached = MOVING_BLOCK_CACHE.get(pos);
        if (cached != null && cached.blockState.equals(state)) {
            cached.update(world, pos, state);
            return cached;
        }

        OverlayMovingBlockRenderState renderState = new OverlayMovingBlockRenderState(world);
        renderState.update(world, pos, state);
        MOVING_BLOCK_CACHE.put(pos.immutable(), renderState);
        return renderState;
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

    private static class OverlayMovingBlockRenderState extends MovingBlockRenderState {
        private ClientLevel world;

        private OverlayMovingBlockRenderState(ClientLevel world) {
            this.world = world;
        }

        private void update(ClientLevel world, BlockPos pos, BlockState state) {
            this.world = world;
            this.blockPos = pos.immutable();
            this.randomSeedPos = pos.immutable();
            this.blockState = state;
            this.biome = world.getBiome(pos);
            this.cardinalLighting = world.cardinalLighting();
            this.lightEngine = world.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver color) {
            return this.world.getBlockTint(pos, color);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return pos.equals(this.blockPos) ? this.blockState : this.world.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return pos.equals(this.blockPos) ? this.blockState.getFluidState() : this.world.getFluidState(pos);
        }
    }
}
