package com.dooji.underlay.client;

import java.util.Map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
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

        for (Map.Entry<BlockPos, IBlockState> entry : UnderlayManagerClient.getAll().entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.distanceSq(eye.x, eye.y, eye.z) > reach * reach) continue;

            IBlockState state = entry.getValue();
            AxisAlignedBB box = state.getBoundingBox(client.world, pos).offset(pos);

            RayTraceResult hit = box.calculateIntercept(eye, end);
            if (hit == null) continue;

            double distanceSquared = hit.hitVec.squareDistanceTo(eye);
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
