package com.dooji.underlay.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.UnderlayClient;
import com.dooji.underlay.UnderlayManagerClient;
import com.dooji.underlay.UnderlayRaycast;
import com.dooji.underlay.network.payloads.PickItemFromOverlayPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Shadow public HitResult hitResult;
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void overlayCrosshair(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        HitResult originalTarget = this.hitResult;
        Player player = client.player;

        if (player == null) {
            return;
        }

        BlockHitResult overlayHit = UnderlayRaycast.trace(player, player.blockInteractionRange(), client.getDeltaTracker().getGameTimeDeltaTicks());
        HitResult chosenTarget = originalTarget;

        if (overlayHit != null) {
            Vec3 eye = player.getEyePosition(client.getDeltaTracker().getGameTimeDeltaTicks());
            double overlayDistanceSq = overlayHit.getLocation().distanceToSqr(eye);
            double originalDistanceSq = (originalTarget == null) ? Double.MAX_VALUE : originalTarget.getLocation().distanceToSqr(eye);

            boolean sameBlockPos = originalTarget instanceof BlockHitResult originalBlockHit && originalBlockHit.getBlockPos().equals(overlayHit.getBlockPos());
            if (overlayDistanceSq < originalDistanceSq && !sameBlockPos) chosenTarget = overlayHit;
        }

        this.hitResult = chosenTarget;
    }

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

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void onItemPick(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        BlockPos overlayPos = UnderlayClient.findOverlayUnderCrosshair(client);

        if (overlayPos != null && UnderlayManagerClient.hasOverlay(overlayPos)) {
            ClientPlayNetworking.send(new PickItemFromOverlayPayload(overlayPos));
            ci.cancel();
        }
    }
}
