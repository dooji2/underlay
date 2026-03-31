package com.dooji.underlay;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

public class UnderlayRaycast {
    private static Object cachedLevel = null;
    private static int cachedViewerId = Integer.MIN_VALUE;
    private static long cachedLevelTime = Long.MIN_VALUE;
    private static long cachedOverlayVersion = Long.MIN_VALUE;
    private static long cachedReachBits = Long.MIN_VALUE;
    private static int cachedTickDeltaBits = Integer.MIN_VALUE;
    private static BlockHitResult cachedHit = null;

    public static BlockHitResult trace(Entity viewer, double reach, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }

        long overlayVersion = UnderlayManagerClient.getVersion();
        long levelTime = client.world.getTime();
        long reachBits = Double.doubleToLongBits(reach);
        int tickDeltaBits = Float.floatToIntBits(tickDelta);

        if (viewer.getId() == cachedViewerId && client.world == cachedLevel && levelTime == cachedLevelTime && overlayVersion == cachedOverlayVersion && reachBits == cachedReachBits && tickDeltaBits == cachedTickDeltaBits) {
            return cachedHit;
        }

        Vec3d eye = viewer.getCameraPosVec(tickDelta);
        Vec3d look = viewer.getRotationVec(tickDelta);
        Vec3d end = eye.add(look.multiply(reach));
        double minX = Math.min(eye.x, end.x) - 1.0;
        double minY = Math.min(eye.y, end.y) - 1.0;
        double minZ = Math.min(eye.z, end.z) - 1.0;
        double maxX = Math.max(eye.x, end.x) + 1.0;
        double maxY = Math.max(eye.y, end.y) + 1.0;
        double maxZ = Math.max(eye.z, end.z) + 1.0;
        double maxReachSq = reach * reach;

        double best = Double.MAX_VALUE;
        BlockHitResult bestHit = null;
        ShapeContext shapeContext = ShapeContext.of(viewer);

        for (var entry : UnderlayManagerClient.getAll().entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getX() > maxX || pos.getX() + 1 < minX || pos.getY() > maxY || pos.getY() + 1 < minY || pos.getZ() > maxZ || pos.getZ() + 1 < minZ || pos.getSquaredDistance(eye) > maxReachSq) {
                continue;
            }

            BlockState state = entry.getValue();
            VoxelShape shape = state.getOutlineShape(client.world, pos, shapeContext);
            BlockHitResult hit = shape.raycast(eye, end, pos);
            if (hit == null) {
                continue;
            }

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

        cachedViewerId = viewer.getId();
        cachedLevel = client.world;
        cachedLevelTime = levelTime;
        cachedOverlayVersion = overlayVersion;
        cachedReachBits = reachBits;
        cachedTickDeltaBits = tickDeltaBits;
        cachedHit = bestHit;

        return bestHit;
    }
}
