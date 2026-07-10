package com.dooji.underlay.mixin.client;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.dooji.underlay.UnderlayRenderer;
import com.dooji.underlay.compat.UnderlaySodiumCompat;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;
import me.jellysquid.mods.sodium.client.world.WorldSlice;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.BlockPos;

@Mixin(ChunkBuilderMeshingTask.class)
public class SodiumChunkBuilderMeshingTaskMixin {
    @Shadow
    @Final
    private RenderSection render;

    @Inject(
        method = "execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
        at = @At(value = "NEW", target = "it/unimi/dsi/fastutil/objects/Reference2ReferenceOpenHashMap"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onExecute(
        ChunkBuildContext buildContext,
        CancellationToken cancellationToken,
        CallbackInfoReturnable<ChunkBuildOutput> cir,
        BuiltSectionInfo.Builder renderData,
        ChunkOcclusionDataBuilder occluder,
        ChunkBuildBuffers buffers,
        BlockRenderCache cache,
        WorldSlice slice,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        BlockPos.Mutable blockPos,
        BlockPos.Mutable modelOffset,
        BlockRenderContext context
    ) {
        Map<BlockPos, BlockState> overlays = UnderlayRenderer.getSectionOverlays(render.getPosition().asLong());
        if (overlays.isEmpty()) {
            return;
        }

        UnderlaySodiumCompat.renderSectionOverlays(overlays, cache, context, buffers);
    }
}
