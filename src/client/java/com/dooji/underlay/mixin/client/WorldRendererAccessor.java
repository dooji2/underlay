package com.dooji.underlay.mixin.client;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.state.WorldRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor("entityRenderDispatcher")
    RenderDispatcher getEntityRenderDispatcher();

    @Accessor("worldRenderState")
    WorldRenderState getWorldRenderState();
}
