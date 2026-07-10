package com.dooji.underlay.mixin.client;

import java.util.Map;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.UnderlayRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
    @Inject(
        method = "compile",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;entrySet()Ljava/util/Set;",
            ordinal = 0
        )
    )
    private void onCompile(SectionPos sectionPos, RenderSectionRegion renderSectionRegion, VertexSorting vertexSorting, SectionBufferBuilderPack sectionBufferBuilderPack, CallbackInfoReturnable<SectionCompiler.Results> cir, @Local(ordinal = 0) PoseStack poseStack) {
        Map<BlockPos, BlockState> overlays = UnderlayRenderer.getSectionOverlays(sectionPos.asLong());
        if (overlays.isEmpty()) {
            return;
        }

        BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
        UnderlayRenderer.renderSectionOverlays(renderSectionRegion, poseStack, blockRenderDispatcher, overlays);
    }
}
