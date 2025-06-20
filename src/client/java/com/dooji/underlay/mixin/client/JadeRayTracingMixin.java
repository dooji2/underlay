package com.dooji.underlay.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.UnderlayRaycast;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import snownee.jade.overlay.RayTracing;

@Mixin(RayTracing.class)
public class JadeRayTracingMixin {
    @Inject(method = "rayTrace", at = @At("RETURN"), cancellable = true, remap = false)
    private void overlayPriority(Entity entity, double reach, double tickDelta, CallbackInfoReturnable<HitResult> cir) {
        HitResult originalHit = cir.getReturnValue();
        Vec3d eye = entity.getCameraPosVec((float)tickDelta);
        
        double originalDistanceSq = (originalHit == null) ? Double.MAX_VALUE : originalHit.getPos().squaredDistanceTo(eye);
        BlockHitResult overlayHit = UnderlayRaycast.trace(entity, reach, (float)tickDelta);
        if (overlayHit == null) return;

        boolean sameBlockPos = originalHit instanceof BlockHitResult originalBlockHit && originalBlockHit.getBlockPos().equals(overlayHit.getBlockPos());
        if (sameBlockPos) return;

        double overlayDistanceSq = overlayHit.getPos().squaredDistanceTo(eye);
        if (overlayDistanceSq < originalDistanceSq) cir.setReturnValue(overlayHit);
    }
}
