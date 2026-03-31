package com.dooji.underlay.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.opengl.GL11;

public class UnderlayRenderer {
    private static final Map<BlockPos, IBlockState> RENDER_CACHE = new ConcurrentHashMap<BlockPos, IBlockState>();

    public static void registerOverlay(BlockPos pos, IBlockState state) {
        RENDER_CACHE.put(pos, state);
    }

    public static void unregisterOverlay(BlockPos pos) {
        RENDER_CACHE.remove(pos);
    }

    public static void clearAllOverlays() {
        RENDER_CACHE.clear();
    }

    public static void forceRefresh() {
        clearAllOverlays();
        UnderlayManagerClient.getAll().forEach(UnderlayRenderer::registerOverlay);
    }

    public static void renderLayer(BlockRenderLayer layer, double partialTicks) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world == null || client.player == null) {
            return;
        }

        double dx = client.player.lastTickPosX + (client.player.posX - client.player.lastTickPosX) * partialTicks;
        double dy = client.player.lastTickPosY + (client.player.posY - client.player.lastTickPosY) * partialTicks;
        double dz = client.player.lastTickPosZ + (client.player.posZ - client.player.lastTickPosZ) * partialTicks;

        int chunks = client.gameSettings.renderDistanceChunks;
        int blocks = chunks * 16;
        double maxDistSq = (double) blocks * (double) blocks;

        BlockRendererDispatcher dispatcher = client.getBlockRendererDispatcher();

        int prevShade = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        ForgeHooksClient.setRenderLayer(layer);
        client.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
        client.entityRenderer.enableLightmap();
        GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        List<BlockPos> stalePositions = new ArrayList<BlockPos>();
        boolean renderedAny = false;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        buffer.setTranslation(-dx, -dy, -dz);
        for (Map.Entry<BlockPos, IBlockState> entry : RENDER_CACHE.entrySet()) {
            BlockPos pos = entry.getKey();
            IBlockState state = entry.getValue();

            if (!UnderlayManagerClient.hasOverlay(pos)) {
                stalePositions.add(pos);
                continue;
            }

            double distanceSq = pos.distanceSq(dx, dy, dz);
            if (distanceSq > maxDistSq) {
                continue;
            }

            if (state.getRenderType() == EnumBlockRenderType.INVISIBLE) {
                continue;
            }

            Block block = state.getBlock();
            if (!block.canRenderInLayer(state, layer)) {
                continue;
            }

            renderedAny |= dispatcher.renderBlock(state, pos, client.world, buffer);
        }

        if (renderedAny) {
            tessellator.draw();
        } else {
            buffer.finishDrawing();
        }
        buffer.setTranslation(0, 0, 0);

        for (BlockPos stalePos : stalePositions) {
            unregisterOverlay(stalePos);
        }

        client.entityRenderer.disableLightmap();
        ForgeHooksClient.setRenderLayer(null);
        GlStateManager.shadeModel(prevShade);
    }
}
