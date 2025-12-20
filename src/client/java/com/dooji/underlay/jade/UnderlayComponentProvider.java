package com.dooji.underlay.jade;

import com.dooji.underlay.Underlay;
import com.dooji.underlay.UnderlayRaycast;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum UnderlayComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath(Underlay.MOD_ID, "overlay");
    }

    @Override
    public int getDefaultPriority() {
        return -100;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockPos pos = accessor.getPosition();
        Minecraft client = Minecraft.getInstance();

        BlockHitResult hit = UnderlayRaycast.trace(client.player, client.player.blockInteractionRange(), client.getDeltaTracker().getGameTimeDeltaTicks());
        if (hit != null && hit.getBlockPos().equals(pos)) {
            tooltip.append(Component.translatable("block.underlay.overlay"));
        }
    }
}
