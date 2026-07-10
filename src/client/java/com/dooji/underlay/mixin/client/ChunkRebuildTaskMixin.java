package com.dooji.underlay.mixin.client;

import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.dooji.underlay.UnderlayRenderer;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

@Mixin(targets = "net.minecraft.client.render.chunk.ChunkBuilder$BuiltChunk$RebuildTask")
public class ChunkRebuildTaskMixin {
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayer;getTranslucent()Lnet/minecraft/client/render/RenderLayer;",
            ordinal = 0
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onRender(
        float cameraX,
        float cameraY,
        float cameraZ,
        BlockBufferBuilderStorage storage,
        CallbackInfoReturnable<?> cir,
        @Coerce Object renderData,
        int i,
        BlockPos blockPos,
        BlockPos blockPos2,
        ChunkOcclusionDataBuilder chunkOcclusionDataBuilder,
        ChunkRendererRegion chunkRendererRegion,
        MatrixStack matrixStack,
        Set<RenderLayer> set
    ) {
        Map<BlockPos, BlockState> overlays = UnderlayRenderer.getSectionOverlays(ChunkSectionPos.toLong(blockPos));
        if (overlays.isEmpty()) {
            return;
        }

        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        UnderlayRenderer.renderSectionOverlays(chunkRendererRegion, matrixStack, blockRenderManager, overlays, renderLayer -> {
            BufferBuilder bufferBuilder = storage.get(renderLayer);
            if (set.add(renderLayer)) {
                bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
            }

            return bufferBuilder;
        });
    }
}
