package com.dooji.underlay.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dooji.underlay.UnderlayRaycast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow public HitResult crosshairTarget;
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void overlayCrosshair(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        HitResult originalTarget = this.crosshairTarget;
        PlayerEntity player = client.player;

        if (player == null) {
            this.crosshairTarget = originalTarget;
            return;
        }

        BlockHitResult overlayHit = UnderlayRaycast.trace(player, player.getBlockInteractionRange(), client.getRenderTickCounter().getTickDelta(true));
        HitResult chosenTarget = originalTarget;

        if (overlayHit != null) {
            Vec3d eye = player.getCameraPosVec(client.getRenderTickCounter().getTickDelta(true));
            double overlayDistanceSq = overlayHit.getPos().squaredDistanceTo(eye);
            double originalDistanceSq = (originalTarget == null) ? Double.MAX_VALUE : originalTarget.getPos().squaredDistanceTo(eye);

            boolean sameBlockPos = originalTarget instanceof BlockHitResult originalBlockHit && originalBlockHit.getBlockPos().equals(overlayHit.getBlockPos());
            if (overlayDistanceSq < originalDistanceSq && !sameBlockPos) chosenTarget = overlayHit;
        }

        this.crosshairTarget = chosenTarget;
    }
}
