package com.dooji.underlay.client;

import java.util.Map;

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
    private static Object cachedLevel = null;
    private static int cachedViewerId = Integer.MIN_VALUE;
    private static long cachedLevelTime = Long.MIN_VALUE;
    private static long cachedOverlayVersion = Long.MIN_VALUE;
    private static long cachedReachBits = Long.MIN_VALUE;
    private static int cachedTickDeltaBits = Integer.MIN_VALUE;
    private static BlockHitResult cachedHit = null;

    public static BlockHitResult trace(Entity viewer, double reach, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }

        long overlayVersion = UnderlayManagerClient.getVersion();
        long levelTime = client.level.getGameTime();
        long reachBits = Double.doubleToLongBits(reach);
        int tickDeltaBits = Float.floatToIntBits(tickDelta);

        if (viewer.getId() == cachedViewerId && client.level == cachedLevel && levelTime == cachedLevelTime && overlayVersion == cachedOverlayVersion && reachBits == cachedReachBits && tickDeltaBits == cachedTickDeltaBits) {
            return cachedHit;
        }

        Vec3 eye = viewer.getEyePosition(tickDelta);
        Vec3 look = viewer.getViewVector(tickDelta);
        Vec3 end = eye.add(look.multiply(reach, reach, reach));
        double minX = Math.min(eye.x, end.x) - 1.0;
        double minY = Math.min(eye.y, end.y) - 1.0;
        double minZ = Math.min(eye.z, end.z) - 1.0;
        double maxX = Math.max(eye.x, end.x) + 1.0;
        double maxY = Math.max(eye.y, end.y) + 1.0;
        double maxZ = Math.max(eye.z, end.z) + 1.0;
        double maxReachSq = reach * reach;

        double best = Double.MAX_VALUE;
        BlockHitResult bestHit = null;
        CollisionContext collisionContext = CollisionContext.of(viewer);

        for (Map.Entry<BlockPos, BlockState> entry : UnderlayManagerClient.getAll().entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getX() > maxX || pos.getX() + 1 < minX || pos.getY() > maxY || pos.getY() + 1 < minY || pos.getZ() > maxZ || pos.getZ() + 1 < minZ || pos.distToCenterSqr(eye) > maxReachSq) {
                continue;
            }

            BlockState state = entry.getValue();
            VoxelShape shape = state.getShape(client.level, pos, collisionContext);
            BlockHitResult hit = shape.clip(eye, end, pos);
            if (hit == null) {
                continue;
            }

            Vec3 hitPos = hit.getLocation();
            double distanceSquared = hitPos.distanceToSqr(eye);

            ClipContext context = new ClipContext(eye, hitPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, viewer);
            BlockHitResult worldHit = client.level.clip(context);

            if (worldHit.getType() == HitResult.Type.BLOCK && worldHit.getLocation().distanceToSqr(eye) < distanceSquared) continue;
            if (distanceSquared < best) {
                best = distanceSquared;
                bestHit = hit;
            }
        }

        cachedViewerId = viewer.getId();
        cachedLevel = client.level;
        cachedLevelTime = levelTime;
        cachedOverlayVersion = overlayVersion;
        cachedReachBits = reachBits;
        cachedTickDeltaBits = tickDeltaBits;
        cachedHit = bestHit;

        return bestHit;
    }
}
