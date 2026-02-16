package com.dooji.underlay.mixin;

import com.dooji.underlay.client.UnderlayRenderer;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin {
    @Inject(method = "renderBlockLayer", at = @At("RETURN"))
    private void renderLayer(BlockRenderLayer layer, double partialTicks, int pass, Entity entity, CallbackInfoReturnable<Integer> cir) {
        UnderlayRenderer.renderLayer(layer, partialTicks);
    }
}
