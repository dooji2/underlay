package com.dooji.underlay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.client.UnderlayRaycast;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import snownee.jade.overlay.RayTracing;

@Pseudo
@Mixin(value = RayTracing.class, remap = false)
public abstract class JadeRayTracingMixin {
    @Inject(method = "rayTrace", at = @At("RETURN"), cancellable = true)
    private void overlayPriority(Entity entity, double blockReach, double entityReach, float partialTicks, CallbackInfoReturnable<HitResult> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return;
        }

        double reach = blockReach;
        double tickDelta = client.getFrameTimeNs() / 50_000_000D;
        HitResult originalHit = cir.getReturnValue();
        Vec3 eye = entity.getEyePosition((float)tickDelta);

        double originalDistanceSq = originalHit == null ? Double.MAX_VALUE : originalHit.getLocation().distanceToSqr(eye);
        BlockHitResult overlayHit = UnderlayRaycast.trace(entity, reach, (float)tickDelta);
        if (overlayHit == null) return;

        boolean sameBlockPos = originalHit instanceof BlockHitResult originalBlockHit && originalBlockHit.getBlockPos().equals(overlayHit.getBlockPos());
        if (sameBlockPos) return;

        double overlayDistanceSq = overlayHit.getLocation().distanceToSqr(eye);
        if (overlayDistanceSq < originalDistanceSq) cir.setReturnValue(overlayHit);
    }
}
