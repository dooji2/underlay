package com.dooji.underlay.mixin;

import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.dooji.underlay.client.UnderlayRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public class ChunkRebuildTaskMixin {
    @Inject(
        method = "compile",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;translucent()Lnet/minecraft/client/renderer/RenderType;",
            ordinal = 0
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onCompile(
        float p_234468_,
        float p_234469_,
        float p_234470_,
        ChunkBufferBuilderPack buffers,
        CallbackInfoReturnable<?> cir,
        @Coerce Object results,
        int radius,
        BlockPos blockPos1,
        BlockPos blockPos2,
        VisGraph visGraph,
        RenderChunkRegion region,
        PoseStack poseStack,
        Set<RenderType> usedRenderTypes,
        RandomSource randomSource,
        BlockRenderDispatcher blockRenderer
    ) {
        Map<BlockPos, BlockState> overlays = UnderlayRenderer.getSectionOverlays(SectionPos.asLong(blockPos1));
        if (overlays.isEmpty()) {
            return;
        }

        UnderlayRenderer.renderSectionOverlays(region, poseStack, blockRenderer, overlays, chunkRenderType -> {
            BufferBuilder bufferBuilder = buffers.builder(chunkRenderType);
            if (usedRenderTypes.add(chunkRenderType)) {
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            }

            return bufferBuilder;
        });
    }
}
