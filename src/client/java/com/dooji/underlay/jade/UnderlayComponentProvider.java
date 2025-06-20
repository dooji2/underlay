package com.dooji.underlay.jade;

import com.dooji.underlay.Underlay;
import com.dooji.underlay.UnderlayRaycast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum UnderlayComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public Identifier getUid() {
        return Identifier.of(Underlay.MOD_ID, "overlay");
    }

    @Override
    public int getDefaultPriority() {
        return -100;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockPos pos = accessor.getPosition();
        MinecraftClient client = MinecraftClient.getInstance();

        BlockHitResult hit = UnderlayRaycast.trace(client.player, client.player.getBlockInteractionRange(), client.getRenderTickCounter().getDynamicDeltaTicks());
        if (hit != null && hit.getBlockPos().equals(pos)) {
            tooltip.append(Text.translatable("block.underlay.overlay"));
        }
    }
}
