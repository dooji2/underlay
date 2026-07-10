package com.dooji.underlay.compat;

import java.util.List;
import java.util.Map;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public final class UnderlaySodiumCompat {
    private UnderlaySodiumCompat() {
    }

    public static void renderSectionOverlays(Map<BlockPos, BlockState> overlays, BlockRenderCache cache, BlockRenderContext context, ChunkBuildBuffers buffers) {
        BlockPos.Mutable modelOffset = new BlockPos.Mutable();

        for (Map.Entry<BlockPos, BlockState> overlay : overlays.entrySet()) {
            BlockPos pos = overlay.getKey();
            BlockState state = overlay.getValue();
            BakedModel model = new ScaledBakedModel(cache.getBlockModels().getModel(state));
            long seed = state.getRenderingSeed(pos);

            modelOffset.set(
                ChunkSectionPos.getLocalCoord(pos.getX()),
                ChunkSectionPos.getLocalCoord(pos.getY()),
                ChunkSectionPos.getLocalCoord(pos.getZ())
            );
            context.update(pos, modelOffset, state, model, seed);
            cache.getBlockRenderer().renderModel(context, buffers);
        }
    }

    private static BakedQuad scale(BakedQuad quad) {
        int[] vertexData = quad.getVertexData().clone();
        int stride = vertexData.length / 4;

        for (int offset = 0; offset < vertexData.length; offset += stride) {
            for (int axis = 0; axis < 3; axis++) {
                float value = Float.intBitsToFloat(vertexData[offset + axis]);
                vertexData[offset + axis] = Float.floatToRawIntBits((value - 0.5F) * 1.0001F + 0.5F);
            }
        }

        return new BakedQuad(vertexData, quad.getColorIndex(), quad.getFace(), quad.getSprite(), quad.hasShade());
    }

    private static class ScaledBakedModel extends ForwardingBakedModel {
        private ScaledBakedModel(BakedModel model) {
            wrapped = model;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
            return wrapped.getQuads(state, face, random).stream().map(UnderlaySodiumCompat::scale).toList();
        }
    }
}
