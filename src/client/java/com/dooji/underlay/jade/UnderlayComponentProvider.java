package com.dooji.underlay.jade;

import com.dooji.underlay.Underlay;
import com.dooji.underlay.UnderlayClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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

        if (UnderlayClient.isLookingDirectlyAtOverlay(client) && pos.equals(UnderlayClient.getDirectlyTargetedOverlay(client))) {
            tooltip.append(Text.translatable("block.underlay.overlay"));
        }
    }
}
