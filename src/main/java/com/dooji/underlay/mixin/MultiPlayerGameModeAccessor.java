package com.dooji.underlay.mixin;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerControllerMP.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("blockHitDelay")
    void setBlockBreakingCooldown(int value);

    @Accessor("blockHitDelay")
    int getBlockBreakingCooldown();
}
