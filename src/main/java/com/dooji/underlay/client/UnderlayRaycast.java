package com.dooji.underlay.client;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class UnderlayRaycast {
    private static World cachedLevel;
    private static int cachedViewerId = Integer.MIN_VALUE;
    private static long cachedLevelTime = Long.MIN_VALUE;
    private static long cachedOverlayVersion = Long.MIN_VALUE;
    private static long cachedReachBits = Long.MIN_VALUE;
    private static int cachedTickDeltaBits = Integer.MIN_VALUE;
    private static RayTraceResult cachedHit;

    public static RayTraceResult trace(Entity viewer, double reach, float tickDelta) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world == null) {
            return null;
        }

        long levelTime = client.world.getTotalWorldTime();
        long overlayVersion = UnderlayManagerClient.getVersion();
        long reachBits = Double.doubleToLongBits(reach);
        int tickDeltaBits = Float.floatToIntBits(tickDelta);
        if (viewer.getEntityId() == cachedViewerId && client.world == cachedLevel && levelTime == cachedLevelTime && overlayVersion == cachedOverlayVersion && reachBits == cachedReachBits && tickDeltaBits == cachedTickDeltaBits) {
            return cachedHit;
        }

        Vec3d eye = viewer.getPositionEyes(tickDelta);
        Vec3d look = viewer.getLook(tickDelta);
        Vec3d end = eye.addVector(look.x * reach, look.y * reach, look.z * reach);
        AxisAlignedBB rayBounds = new AxisAlignedBB(
                Math.min(eye.x, end.x), Math.min(eye.y, end.y), Math.min(eye.z, end.z),
                Math.max(eye.x, end.x), Math.max(eye.y, end.y), Math.max(eye.z, end.z)
        );

        double best = Double.MAX_VALUE;
        RayTraceResult bestHit = null;

        for (Map.Entry<BlockPos, IBlockState> entry : UnderlayManagerClient.getAll().entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.distanceSq(eye.x, eye.y, eye.z) > reach * reach) continue;

            IBlockState state = entry.getValue();
            AxisAlignedBB box = state.getBoundingBox(client.world, pos).offset(pos);
            if (box == Block.NULL_AABB || !box.intersects(rayBounds)) continue;

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

        cachedLevel = client.world;
        cachedViewerId = viewer.getEntityId();
        cachedLevelTime = levelTime;
        cachedOverlayVersion = overlayVersion;
        cachedReachBits = reachBits;
        cachedTickDeltaBits = tickDeltaBits;
        cachedHit = bestHit;
        return bestHit;
    }
}
