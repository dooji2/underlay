package com.dooji.underlay.mixin.client;

import java.util.Map;

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
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
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
        method = "compile",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;entrySet()Ljava/util/Set;",
            ordinal = 0
        )
    )
    private void onCompile(SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack builders, CallbackInfoReturnable<SectionCompiler.Results> cir, @Local(ordinal = 0) Map<ChunkSectionLayer, BufferBuilder> startedLayers) {
        Map<BlockPos, BlockState> overlays = UnderlayRenderer.getSectionOverlays(sectionPos.asLong());
        if (overlays.isEmpty()) {
            return;
        }

        Renderer renderer = Renderer.get();
        AltModelBlockRenderer blockRenderer = renderer.altModelBlockRenderer(ambientOcclusion, true, blockColors);
        QuadEmitter quadEmitter = renderer.quadEmitter(quad -> quad.buffer(OverlayTexture.NO_OVERLAY, invokeGetOrBeginLayer(startedLayers, builders, quad.chunkLayer())));
        UnderlayRenderer.renderSectionOverlays(region, blockRenderer, quadEmitter, blockModelSet, overlays);
    }
}
