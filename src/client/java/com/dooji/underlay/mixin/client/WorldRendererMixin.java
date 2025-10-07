package com.dooji.underlay.mixin.client;

import com.dooji.underlay.UnderlayRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow private DefaultFramebufferSet framebufferSet;
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/FrameGraphBuilder;run(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/FrameGraphBuilder$Profiler;)V", shift = At.Shift.BEFORE))
    private void insertOverlayPass(
        ObjectAllocator allocator,
        RenderTickCounter tickCounter,
        boolean renderBlockOutline,
        Camera camera,
        Matrix4f positionMatrix,
        Matrix4f matrix4f,
        Matrix4f projectionMatrix,
        GpuBufferSlice fogBuffer,
        Vector4f fogColor,
        boolean renderSky,
        CallbackInfo ci,
        @Local FrameGraphBuilder frameGraphBuilder
    ) {
        FramePass pass = frameGraphBuilder.createPass("underlay_overlays");
        if (this.framebufferSet.translucentFramebuffer != null) {
            this.framebufferSet.translucentFramebuffer = pass.transfer(this.framebufferSet.translucentFramebuffer);
        } else {
            this.framebufferSet.mainFramebuffer = pass.transfer(this.framebufferSet.mainFramebuffer);
        }

        pass.setRenderer(() -> {
            RenderSystem.setShaderFog(fogBuffer);
            UnderlayRenderer.renderOverlays();
        });
    }
}
