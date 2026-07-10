package com.dooji.underlay.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
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
            BlockStateModel model = new ScaledBlockStateModel(cache.getBlockModels().get(state));

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
        public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
            int firstPart = parts.size();
            model.collectParts(random, parts);
            for (int i = firstPart; i < parts.size(); i++) {
                parts.set(i, new ScaledBlockStateModelPart(parts.get(i)));
            }
        }

        @Override
        public Material.Baked particleMaterial() {
            return model.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return model.materialFlags();
        }
    }

    private static class ScaledBlockStateModelPart implements BlockStateModelPart {
        private final BlockStateModelPart part;

        private ScaledBlockStateModelPart(BlockStateModelPart part) {
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
                    quad.direction(),
                    quad.materialInfo()
                ));
            }

            return scaledQuads;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return part.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return part.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return part.materialFlags();
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
