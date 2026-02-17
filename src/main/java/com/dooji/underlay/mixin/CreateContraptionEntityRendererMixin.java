package com.dooji.underlay.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dooji.underlay.util.HasUnderlayOverlays;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ClientContraption;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.createmod.catnip.render.ShadedBlockSbbBuilder;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

@Pseudo
@Mixin(value = ContraptionEntityRenderer.class, remap = false)
public abstract class CreateContraptionEntityRendererMixin {
    private static final Map<Contraption, Integer> RENDER_CACHE_VERSION = new WeakHashMap<>();
    private static final Map<Contraption, Map<RenderType, SuperByteBuffer>> RENDER_CACHE = new WeakHashMap<>();

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/render/ContraptionMatrices;clear()V"))
    private void renderOverlays(AbstractContraptionEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int overlay, CallbackInfo ci) {
        Contraption contraption = entity.getContraption();
        if (!(contraption instanceof HasUnderlayOverlays movingOverlays)) {
            return;
        }

        Map<BlockPos, BlockState> overlays = movingOverlays.getMovingOverlays();
        if (overlays.isEmpty()) {
            RENDER_CACHE.remove(contraption);
            RENDER_CACHE_VERSION.remove(contraption);
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        ClientContraption clientContraption = contraption.getOrCreateClientContraptionLazy();
        VirtualRenderWorld renderWorld = clientContraption.getRenderLevel();
        BlockRenderDispatcher blockRenderer = client.getBlockRenderer();
        int overlaysVersion = movingOverlays.getMovingOverlaysVersion();

        Map<RenderType, SuperByteBuffer> cachedBuffers = RENDER_CACHE.computeIfAbsent(contraption, key -> new HashMap<>());
        int cachedVersion = RENDER_CACHE_VERSION.getOrDefault(contraption, -1);
        if (cachedVersion != overlaysVersion || cachedBuffers.isEmpty()) {
            cachedBuffers.clear();

            ModelBlockRenderer modelRenderer = blockRenderer.getModelRenderer();
            RandomSource renderTypeRandom = RandomSource.create();
            // similar impl to com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer, this seems to be deprecated but is still in use so I suppose it's fine? >.<
            ShadedBlockSbbBuilder sbbBuilder = ShadedBlockSbbBuilder.create();

            for (RenderType layer : RenderType.chunkBufferLayers()) {
                sbbBuilder.begin();
                ModelBlockRenderer.enableCaching();

                try {
                    PoseStack localPoseStack = new PoseStack();

                    for (Map.Entry<BlockPos, BlockState> entry : overlays.entrySet()) {
                        BlockPos localPos = entry.getKey();
                        BlockState state = entry.getValue();
                        BakedModel model = blockRenderer.getBlockModel(state);
                        ModelData modelData = renderWorld.getModelData(localPos);
                        modelData = model.getModelData(renderWorld, localPos, state, modelData);
                        long randomSeed = state.getSeed(localPos);
                        renderTypeRandom.setSeed(randomSeed);

                        if (!model.getRenderTypes(state, renderTypeRandom, modelData).contains(layer)) {
                            continue;
                        }

                        localPoseStack.pushPose();
                        localPoseStack.translate(localPos.getX(), localPos.getY(), localPos.getZ());
                        localPoseStack.translate(0.5, 0.5, 0.5);
                        localPoseStack.scale(1.0001f, 1.0001f, 1.0001f);
                        localPoseStack.translate(-0.5, -0.5, -0.5);
                        modelRenderer.tesselateBlock(renderWorld, model, state, localPos, localPoseStack, sbbBuilder, true, renderTypeRandom, randomSeed, 0, modelData, layer);
                        localPoseStack.popPose();
                    }
                } finally {
                    ModelBlockRenderer.clearCache();
                }

                SuperByteBuffer sbb = sbbBuilder.end();
                if (!sbb.isEmpty()) {
                    cachedBuffers.put(layer, sbb);
                }
            }

            RENDER_CACHE_VERSION.put(contraption, overlaysVersion);
        }

        for (RenderType layer : RenderType.chunkBufferLayers()) {
            SuperByteBuffer sbb = cachedBuffers.get(layer);
            if (sbb == null || sbb.isEmpty()) {
                continue;
            }

            sbb.transform(clientContraption.getMatrices().getModel())
                .useLevelLight(client.level, clientContraption.getMatrices().getWorld())
                .renderInto(poseStack, buffers.getBuffer(layer));
        }
    }
}
