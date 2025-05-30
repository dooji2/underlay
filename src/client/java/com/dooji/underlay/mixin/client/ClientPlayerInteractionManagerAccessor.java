package com.dooji.underlay.mixin.client;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("blockBreakingCooldown")
    void setBlockBreakingCooldown(int value);

    @Accessor("blockBreakingCooldown")
    int getBlockBreakingCooldown();
}
