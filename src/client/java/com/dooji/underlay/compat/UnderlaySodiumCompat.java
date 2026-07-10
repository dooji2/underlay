package com.dooji.underlay.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public final class UnderlaySodiumCompat {
    private UnderlaySodiumCompat() {
    }

    public static void renderSectionOverlays(Map<BlockPos, BlockState> overlays, BlockRenderCache cache, BlockRenderer blockRenderer) {
        BlockPos.MutableBlockPos modelOffset = new BlockPos.MutableBlockPos();

        for (Map.Entry<BlockPos, BlockState> overlay : overlays.entrySet()) {
            BlockPos pos = overlay.getKey();
            BlockState state = overlay.getValue();
            BlockStateModel model = new ScaledBlockStateModel(cache.getBlockModels().getBlockModel(state));

            modelOffset.set(
                SectionPos.sectionRelative(pos.getX()),
                SectionPos.sectionRelative(pos.getY()),
                SectionPos.sectionRelative(pos.getZ())
            );
            blockRenderer.renderModel(model, state, pos, modelOffset);
        }
    }

    private static class ScaledBlockStateModel implements BlockStateModel {
        private final BlockStateModel model;

        private ScaledBlockStateModel(BlockStateModel model) {
            this.model = model;
        }

        @Override
        public void collectParts(RandomSource random, List<BlockModelPart> parts) {
            int firstPart = parts.size();
            model.collectParts(random, parts);
            for (int i = firstPart; i < parts.size(); i++) {
                parts.set(i, new ScaledBlockModelPart(parts.get(i)));
            }
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return model.particleIcon();
        }
    }

    private static class ScaledBlockModelPart implements BlockModelPart {
        private final BlockModelPart part;

        private ScaledBlockModelPart(BlockModelPart part) {
            this.part = part;
        }

        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            List<BakedQuad> quads = part.getQuads(direction);
            List<BakedQuad> scaledQuads = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                scaledQuads.add(new BakedQuad(
                    scale(quad.position0()),
                    scale(quad.position1()),
                    scale(quad.position2()),
                    scale(quad.position3()),
                    quad.packedUV0(),
                    quad.packedUV1(),
                    quad.packedUV2(),
                    quad.packedUV3(),
                    quad.tintIndex(),
                    quad.direction(),
                    quad.sprite(),
                    quad.shade(),
                    quad.lightEmission()
                ));
            }

            return scaledQuads;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return part.useAmbientOcclusion();
        }

        @Override
        public TextureAtlasSprite particleIcon() {
            return part.particleIcon();
        }

        private static Vector3fc scale(Vector3fc pos) {
            return new Vector3f(
                (pos.x() - 0.5F) * 1.0001F + 0.5F,
                (pos.y() - 0.5F) * 1.0001F + 0.5F,
                (pos.z() - 0.5F) * 1.0001F + 0.5F
            );
        }
    }
}
