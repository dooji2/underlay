package com.dooji.underlay.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;

import com.dooji.underlay.UnderlayRaycast;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import snownee.jade.overlay.RayTracing;

@Mixin(RayTracing.class)
public class JadeRayTracingMixin {
    @Shadow @Nullable private HitResult target;
    @Shadow private Vec3 hitLocation;

    @Inject(method = "rayTrace", at = @At("RETURN"), remap = false)
    private void overlayPriority(Entity entity, double reach, double tickDelta, CallbackInfo ci) {
        HitResult originalHit = this.target;
        Vec3 eye = entity.getEyePosition((float)tickDelta);

        double originalDistanceSq = (originalHit == null) ? Double.MAX_VALUE : originalHit.getLocation().distanceToSqr(eye);
        BlockHitResult overlayHit = UnderlayRaycast.trace(entity, reach, (float)tickDelta);
        if (overlayHit == null) return;

        boolean sameBlockPos = originalHit instanceof BlockHitResult originalBlockHit && originalBlockHit.getBlockPos().equals(overlayHit.getBlockPos());
        if (sameBlockPos) return;

        double overlayDistanceSq = overlayHit.getLocation().distanceToSqr(eye);
        if (overlayDistanceSq < originalDistanceSq) {
            this.target = overlayHit;
            this.hitLocation = overlayHit.getLocation();
        }
    }
}
