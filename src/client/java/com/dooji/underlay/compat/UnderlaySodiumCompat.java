package com.dooji.underlay.compat;

import java.util.Map;
import java.util.function.Supplier;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public final class UnderlaySodiumCompat {
    private UnderlaySodiumCompat() {
    }

    public static void renderSectionOverlays(Map<BlockPos, BlockState> overlays, BlockRenderCache cache, BlockRenderer blockRenderer) {
        BlockPos.Mutable modelOffset = new BlockPos.Mutable();

        for (Map.Entry<BlockPos, BlockState> overlay : overlays.entrySet()) {
            BlockPos pos = overlay.getKey();
            BlockState state = overlay.getValue();
            BakedModel model = new ScaledBakedModel(cache.getBlockModels().getModel(state));

            modelOffset.set(
                ChunkSectionPos.getLocalCoord(pos.getX()),
                ChunkSectionPos.getLocalCoord(pos.getY()),
                ChunkSectionPos.getLocalCoord(pos.getZ())
            );
            blockRenderer.renderModel(model, state, pos, modelOffset);
        }
    }

    private static class ScaledBakedModel extends ForwardingBakedModel {
        private ScaledBakedModel(BakedModel model) {
            wrapped = model;
        }

        @Override
        public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
            context.pushTransform(quad -> {
                for (int vertex = 0; vertex < 4; vertex++) {
                    quad.pos(
                        vertex,
                        (quad.x(vertex) - 0.5F) * 1.0001F + 0.5F,
                        (quad.y(vertex) - 0.5F) * 1.0001F + 0.5F,
                        (quad.z(vertex) - 0.5F) * 1.0001F + 0.5F
                    );
                }

                return true;
            });

            try {
                wrapped.emitBlockQuads(blockView, state, pos, randomSupplier, context);
            } finally {
                context.popTransform();
            }
        }
    }
}
