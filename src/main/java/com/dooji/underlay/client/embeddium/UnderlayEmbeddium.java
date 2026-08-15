package com.dooji.underlay.client.embeddium;

import java.util.Map;

import com.dooji.underlay.client.UnderlayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import org.embeddedt.embeddium.api.ChunkMeshEvent;

public final class UnderlayEmbeddium {
    private static boolean initialized;

    private UnderlayEmbeddium() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        ChunkMeshEvent.BUS.addListener(UnderlayEmbeddium::addOverlays);
    }

    private static void addOverlays(ChunkMeshEvent event) {
        Map<BlockPos, BlockState> overlays = UnderlayRenderer.getSectionOverlays(event.getSectionOrigin().asLong());
        if (overlays.isEmpty()) {
            return;
        }

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        event.addMeshAppender(context -> UnderlayRenderer.renderSectionOverlays(
            context.blockRenderView(),
            new PoseStack(),
            blockRenderer,
            overlays,
            context.vertexConsumerProvider()
        ));
    }
}
