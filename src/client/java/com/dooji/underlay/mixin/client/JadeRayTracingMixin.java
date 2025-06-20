package com.dooji.underlay.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;

import com.dooji.underlay.UnderlayRaycast;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import snownee.jade.overlay.RayTracing;

@Mixin(RayTracing.class)
public class JadeRayTracingMixin {
    @Shadow @Nullable private HitResult target;
    @Shadow private Vec3d hitLocation;

    @Inject(method = "rayTrace", at = @At("RETURN"), remap = false)
    private void overlayPriority(Entity entity, double reach, double tickDelta, CallbackInfo ci) {
        HitResult originalHit = this.target;
        Vec3d eye = entity.getCameraPosVec((float)tickDelta);

        double originalDistanceSq = (originalHit == null) ? Double.MAX_VALUE : originalHit.getPos().squaredDistanceTo(eye);
        BlockHitResult overlayHit = UnderlayRaycast.trace(entity, reach, (float)tickDelta);
        if (overlayHit == null) return;

        boolean sameBlockPos = originalHit instanceof BlockHitResult originalBlockHit && originalBlockHit.getBlockPos().equals(overlayHit.getBlockPos());
        if (sameBlockPos) return;

        double overlayDistanceSq = overlayHit.getPos().squaredDistanceTo(eye);
        if (overlayDistanceSq < originalDistanceSq) {
            this.target = overlayHit;
            this.hitLocation = overlayHit.getPos();
        }
    }
}
