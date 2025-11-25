package com.dooji.underlay.mixin.client;

import org.jetbrains.annotations.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dooji.underlay.UnderlayRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Nullable
    private ClientWorld world;

    @Redirect(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/debug/DebugRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Frustum;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;DDDZ)V"))
    private void beforeDebugRenderUnderlay(DebugRenderer renderer, MatrixStack matrices, Frustum frustum, VertexConsumerProvider.Immediate consumers, double cameraX, double cameraY, double cameraZ, boolean renderBlockOutline) {
        Camera camera = this.client.gameRenderer.getCamera();
        UnderlayRenderer.renderOverlays(matrices, consumers, camera, this.world);
        renderer.render(matrices, frustum, consumers, cameraX, cameraY, cameraZ, renderBlockOutline);
    }
}
