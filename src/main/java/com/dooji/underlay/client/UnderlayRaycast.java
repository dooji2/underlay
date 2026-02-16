package com.dooji.underlay.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class UnderlayRaycast {
    public static RayTraceResult trace(Entity viewer, double reach, float tickDelta) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world == null) {
            return null;
        }

        Vec3d eye = viewer.getPositionEyes(tickDelta);
        Vec3d look = viewer.getLook(tickDelta);
        Vec3d end = eye.addVector(look.x * reach, look.y * reach, look.z * reach);

        double best = Double.MAX_VALUE;
        RayTraceResult bestHit = null;

        for (BlockPos pos : UnderlayManagerClient.getAll().keySet()) {
            IBlockState state = UnderlayManagerClient.getOverlay(pos);

            RayTraceResult hit = state.collisionRayTrace(client.world, pos, eye, end);
            if (hit == null) continue;

            double distanceSquared = hit.hitVec.squareDistanceTo(eye);
            if (distanceSquared > reach * reach) continue;

            RayTraceResult worldHit = client.world.rayTraceBlocks(eye, hit.hitVec, false, true, false);
            if (worldHit != null && worldHit.typeOfHit == RayTraceResult.Type.BLOCK && worldHit.hitVec.squareDistanceTo(eye) < distanceSquared) continue;

            if (distanceSquared < best) {
                best = distanceSquared;
                bestHit = new RayTraceResult(hit.hitVec, hit.sideHit, pos);
            }
        }

        return bestHit;
    }
}
