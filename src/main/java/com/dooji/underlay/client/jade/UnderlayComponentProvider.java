package com.dooji.underlay.client.jade;

import com.dooji.underlay.client.UnderlayRaycast;
import com.dooji.underlay.main.Underlay;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

@OnlyIn(Dist.CLIENT)
public enum UnderlayComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(Underlay.MOD_ID, "overlay");
    }

    @Override
    public int getDefaultPriority() {
        return -100;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null) {
            return;
        }

        BlockPos targetPos = accessor.getPosition();
        double reach = Player.DEFAULT_BLOCK_INTERACTION_RANGE;
        float partialTicks = (float)(client.getFrameTimeNs() / 50_000_000L);

        BlockHitResult hit = UnderlayRaycast.trace(player, reach, partialTicks);
        if (hit != null && hit.getBlockPos().equals(targetPos)) {
            tooltip.append(Component.translatable("block." + Underlay.MOD_ID + ".overlay"));
        }
    }
}
