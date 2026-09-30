package com.dooji.underlay.mixin.client;

import java.util.Map;
import java.util.List;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.UnderlayRenderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
    @Shadow
    @Final
    private boolean ambientOcclusion;

    @Shadow
    @Final
    private BlockStateModelSet blockModelSet;

    @Shadow
    @Final
    private BlockColors blockColors;

    @Invoker("getOrBeginLayer")
    protected abstract BufferBuilder invokeGetOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack buffers, ChunkSectionLayer layer);

    @Inject(
        method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;entrySet()Ljava/util/Set;",
            ordinal = 0
        )
    )
    private void onCompile(SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack builders, List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers, CallbackInfoReturnable<SectionCompiler.Results> cir, @Local(ordinal = 0) Map<ChunkSectionLayer, BufferBuilder> startedLayers) {
        Map<BlockPos, BlockState> overlays = UnderlayRenderer.getSectionOverlays(sectionPos.asLong());
        if (overlays.isEmpty()) {
            return;
        }

        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(ambientOcclusion, true, blockColors);
        BlockQuadOutput quadEmitter = (x, y, z, quad, quadInstance) -> {
            quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
            invokeGetOrBeginLayer(startedLayers, builders, quad.materialInfo().layer()).putBlockBakedQuad(x, y, z, quad, quadInstance);
        };

        UnderlayRenderer.renderSectionOverlays(region, blockRenderer, quadEmitter, blockModelSet, overlays);
    }
}
