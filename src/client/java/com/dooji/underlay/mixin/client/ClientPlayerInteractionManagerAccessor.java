package com.dooji.underlay.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("destroyDelay")
    void setBlockBreakingCooldown(int value);

    @Accessor("destroyDelay")
    int getBlockBreakingCooldown();
}
