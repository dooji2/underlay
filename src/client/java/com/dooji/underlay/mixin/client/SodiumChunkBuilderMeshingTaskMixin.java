package com.dooji.underlay.mixin.client;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.UnderlayRenderer;
import com.dooji.underlay.compat.UnderlaySodiumCompat;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public class SodiumChunkBuilderMeshingTaskMixin {
    @Shadow
    @Final
    private ChunkRenderContext renderContext;

    @Inject(
        method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
        at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;release()V")
    )
    private void onExecute(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        Map<BlockPos, BlockState> overlays = UnderlayRenderer.getSectionOverlays(renderContext.getOrigin().asLong());
        if (overlays.isEmpty()) {
            return;
        }

        BlockRenderCache cache = buildContext.cache;
        UnderlaySodiumCompat.renderSectionOverlays(overlays, cache, cache.getBlockRenderer());
    }
}
