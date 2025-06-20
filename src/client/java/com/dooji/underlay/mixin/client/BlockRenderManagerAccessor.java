package com.dooji.underlay.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;

@Mixin(BlockRenderManager.class)
public interface BlockRenderManagerAccessor {
    @Accessor("blockModelRenderer")
    BlockModelRenderer getBlockModelRenderer();
}
