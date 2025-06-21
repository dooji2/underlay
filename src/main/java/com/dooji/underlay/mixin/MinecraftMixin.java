package com.dooji.underlay.mixin;

import com.dooji.underlay.client.UnderlayClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void handleInitialBreaking(CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        BlockPos overlayPos = UnderlayClient.findOverlayUnderCrosshair(client);

        if (overlayPos != null) {
            UnderlayClient.breakOverlay(client, overlayPos);
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
