package com.dooji.underlay.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class UnderlayRaycast {
    public static BlockHitResult trace(Entity viewer, double reach, float tickDelta) {
        Minecraft client = Minecraft.getInstance();

        Vec3 eye = viewer.getEyePosition(tickDelta);
        Vec3 look = viewer.getViewVector(tickDelta);
        Vec3 end = eye.add(look.multiply(reach, reach, reach));

        double best = Double.MAX_VALUE;
        BlockHitResult bestHit = null;

        for (BlockPos pos : UnderlayManagerClient.getAll().keySet()) {
            BlockState state = UnderlayManagerClient.getOverlay(pos);
            VoxelShape shape = state.getShape(client.level, pos, CollisionContext.of(viewer));

            BlockHitResult hit = shape.clip(eye, end, pos);
            if (hit == null) continue;

            Vec3 hitPos = hit.getLocation();
            double distanceSquared = hitPos.distanceToSqr(eye);
            if (distanceSquared > reach * reach) continue;

            ClipContext context = new ClipContext(eye, hitPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, viewer);
            BlockHitResult worldHit = client.level.clip(context);

            if (worldHit.getType() == HitResult.Type.BLOCK && worldHit.getLocation().distanceToSqr(eye) < distanceSquared) continue;
            if (distanceSquared < best) {
                best = distanceSquared;
                bestHit = hit;
            }
        }

        return bestHit;
    }
}
