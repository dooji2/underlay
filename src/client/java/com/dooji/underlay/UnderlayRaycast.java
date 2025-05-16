package com.dooji.underlay;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

public class UnderlayRaycast {
    public static BlockHitResult trace(Entity viewer, double reach, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();

        Vec3d eye = viewer.getCameraPosVec(tickDelta);
        Vec3d look = viewer.getRotationVec(tickDelta);
        Vec3d end = eye.add(look.multiply(reach));

        double best = Double.MAX_VALUE;
        BlockHitResult bestHit = null;

        for (BlockPos pos : UnderlayManagerClient.getAll().keySet()) {
            if (pos.getSquaredDistance(eye) > reach * reach) continue;

            BlockState state = UnderlayManagerClient.getOverlay(pos).getBlock().getDefaultState();
            VoxelShape shape = state.getOutlineShape(client.world, pos, ShapeContext.of((PlayerEntity)viewer));

            BlockHitResult hit = shape.raycast(eye, end, pos);
            if (hit == null) continue;

            Vec3d hitPos = hit.getPos();
            double distanceSquared = hitPos.squaredDistanceTo(eye);

            RaycastContext context = new RaycastContext(eye, hitPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, viewer);
            BlockHitResult worldHit = client.world.raycast(context);

            if (worldHit.getType() == HitResult.Type.BLOCK && worldHit.getPos().squaredDistanceTo(eye) < distanceSquared) continue;
            if (distanceSquared < best) {
                best = distanceSquared;
                bestHit = hit;
            }
        }

        return bestHit;
    }
}
