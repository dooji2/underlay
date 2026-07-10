package com.dooji.underlay.mixin.client;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.dooji.underlay.UnderlayRenderer;

import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

@Mixin(SectionBuilder.class)
public class SectionBuilderMixin {
    @Inject(
        method = "build",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;entrySet()Ljava/util/Set;",
            ordinal = 0
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onBuild(
        ChunkSectionPos sectionPos,
        ChunkRendererRegion renderRegion,
        VertexSorter vertexSorter,
        BlockBufferAllocatorStorage allocatorStorage,
        CallbackInfoReturnable<SectionBuilder.RenderData> cir,
        SectionBuilder.RenderData renderData,
        BlockPos blockPos,
        BlockPos blockPos2,
        ChunkOcclusionDataBuilder chunkOcclusionDataBuilder,
        MatrixStack matrixStack,
        Map<RenderLayer, BufferBuilder> map
    ) {
        Map<BlockPos, BlockState> overlays = UnderlayRenderer.getSectionOverlays(ChunkSectionPos.toLong(blockPos));
        if (overlays.isEmpty()) {
            return;
        }

        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        UnderlayRenderer.renderSectionOverlays(renderRegion, matrixStack, blockRenderManager, overlays, renderLayer -> {
            BufferBuilder bufferBuilder = map.get(renderLayer);
            if (bufferBuilder == null) {
                bufferBuilder = new BufferBuilder(allocatorStorage.get(renderLayer), VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                map.put(renderLayer, bufferBuilder);
            }

            return bufferBuilder;
        });
    }
}
