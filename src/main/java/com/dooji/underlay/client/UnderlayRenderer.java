package com.dooji.underlay.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import com.dooji.underlay.client.compat.UnderlayEmbeddiumCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

public class UnderlayRenderer {
    private static final Consumer<RenderLevelStageEvent> BE_RENDERER = UnderlayRenderer::onRenderLevel;
    private static final Map<Long, Map<BlockPos, BlockState>> SECTION_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, BlockState> BE_STATES = new ConcurrentHashMap<>();
    private static final Map<BlockPos, CachedBlockEntity> BE_CACHE = new ConcurrentHashMap<>();
    private static boolean berRegistered;

    public static void init() {
        if (ModList.get().isLoaded("embeddium")) {
            UnderlayEmbeddiumCompat.init();
        }
    }

    public static void registerOverlay(BlockPos pos, BlockState state) {
        BlockPos immutablePos = pos.immutable();
        long section = SectionPos.asLong(immutablePos);
        SECTION_CACHE.computeIfAbsent(section, key -> new ConcurrentHashMap<>()).put(immutablePos, state);

        if (state.getBlock() instanceof EntityBlock) {
            BE_STATES.put(immutablePos, state);
        } else {
            BE_STATES.remove(immutablePos);
        }

        updateBER();
        BE_CACHE.remove(immutablePos);
        markSectionDirty(section);
    }

    public static void unregisterOverlay(BlockPos pos) {
        long section = SectionPos.asLong(pos);
        Map<BlockPos, BlockState> overlays = SECTION_CACHE.get(section);
        if (overlays != null) {
            overlays.remove(pos);
            if (overlays.isEmpty()) {
                SECTION_CACHE.remove(section, overlays);
            }
        }

        BE_CACHE.remove(pos);
        BE_STATES.remove(pos);
        updateBER();
        markSectionDirty(section);
    }

    public static void clearAllOverlays() {
        SECTION_CACHE.clear();
        BE_STATES.clear();
        BE_CACHE.clear();
        updateBER();
    }

    public static void forceRefresh() {
        Set<Long> dirtySections = new HashSet<>(SECTION_CACHE.keySet());
        clearAllOverlays();
        UnderlayManagerClient.getAll().forEach((pos, state) -> {
            registerOverlay(pos, state);
            dirtySections.remove(SectionPos.asLong(pos));
        });
        dirtySections.forEach(UnderlayRenderer::markSectionDirty);
    }

    public static Map<BlockPos, BlockState> getSectionOverlays(long section) {
        Map<BlockPos, BlockState> overlays = SECTION_CACHE.get(section);
        if (overlays == null || overlays.isEmpty()) {
            return Map.of();
        }

        Map<BlockPos, BlockState> sectionOverlays = new HashMap<>();
        overlays.forEach((pos, state) -> {
            if (state.getRenderShape() == RenderShape.MODEL) {
                sectionOverlays.put(pos, state);
            }
        });
        return sectionOverlays;
    }

    public static void renderSectionOverlays(BlockAndTintGetter region, PoseStack poseStack, BlockRenderDispatcher blockRenderer, Map<BlockPos, BlockState> overlays, Function<RenderType, VertexConsumer> buffers) {
        RandomSource randomSource = RandomSource.create();

        for (Map.Entry<BlockPos, BlockState> overlay : overlays.entrySet()) {
            BlockPos pos = overlay.getKey();
            BlockState state = overlay.getValue();
            BakedModel model = blockRenderer.getBlockModel(state);
            ModelData modelData = ModelData.EMPTY;
            long seed = state.getSeed(pos);
            randomSource.setSeed(seed);

            poseStack.pushPose();

            try {
                poseStack.translate(
                    SectionPos.sectionRelative(pos.getX()),
                    SectionPos.sectionRelative(pos.getY()),
                    SectionPos.sectionRelative(pos.getZ())
                );
                poseStack.translate(0.5D, 0.5D, 0.5D);
                poseStack.scale(1.0001F, 1.0001F, 1.0001F);
                poseStack.translate(-0.5D, -0.5D, -0.5D);

                for (RenderType chunkRenderType : model.getRenderTypes(state, randomSource, modelData)) {
                    VertexConsumer vertexConsumer = buffers.apply(chunkRenderType);
                    blockRenderer.renderBatched(state, pos, region, poseStack, vertexConsumer, true, randomSource, modelData, chunkRenderType);
                }
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (BE_STATES.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        renderBlockEntities(event);
    }

    private static void updateBER() {
        if (BE_STATES.isEmpty()) {
            if (berRegistered) {
                MinecraftForge.EVENT_BUS.unregister(BE_RENDERER);
                berRegistered = false;
            }

            return;
        }

        if (!berRegistered) {
            MinecraftForge.EVENT_BUS.addListener(BE_RENDERER);
            berRegistered = true;
        }
    }

    private static void renderBlockEntities(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) {
            poseStack = new PoseStack();
        }

        if (client.level == null || client.player == null) {
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

        for (Map.Entry<BlockPos, BlockState> overlay : BE_STATES.entrySet()) {
            BlockPos pos = overlay.getKey();
            BlockState state = overlay.getValue();
            if (pos.distSqr(playerPos) > maxDistSq) {
                continue;
            }

            if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
                BE_STATES.remove(pos, state);
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
        updateBER();
    }

    private static BlockEntity getOrCreateBlockEntity(BlockPos pos, BlockState state, EntityBlock entityBlock) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }

        CachedBlockEntity cached = BE_CACHE.get(pos);
        if (cached != null && cached.state.equals(state)) {
            if (cached.blockEntity.getLevel() != client.level) {
                cached.blockEntity.setLevel(client.level);
            }

            return cached.blockEntity;
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(pos, state);
        if (blockEntity == null) {
            BE_CACHE.remove(pos);
            return null;
        }

        blockEntity.setLevel(client.level);
        BE_CACHE.put(pos.immutable(), new CachedBlockEntity(state, blockEntity));
        return blockEntity;
    }

    private static void markSectionDirty(long section) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        BlockPos playerPos = client.player.blockPosition();
        int sectionX = SectionPos.x(section);
        int sectionY = SectionPos.y(section);
        int sectionZ = SectionPos.z(section);
        int playerSectionX = SectionPos.blockToSectionCoord(playerPos.getX());
        int playerSectionZ = SectionPos.blockToSectionCoord(playerPos.getZ());
        int renderDistance = client.options.getEffectiveRenderDistance();
        if (sectionY < client.level.getMinSection() || sectionY >= client.level.getMaxSection()) {
            return;
        }

        if (Math.abs(sectionX - playerSectionX) > renderDistance || Math.abs(sectionZ - playerSectionZ) > renderDistance) {
            return;
        }

        client.levelRenderer.setSectionDirty(sectionX, sectionY, sectionZ);
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
