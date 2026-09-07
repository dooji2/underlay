package com.dooji.underlay.client.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class UnderlaySableClient {
    public static Vec3 getEyePosition(Entity viewer, float tickDelta) {
        return Sable.HELPER.getEyePositionInterpolated(viewer, tickDelta);
    }

    public static BlockHitResult clip(VoxelShape shape, BlockPos pos, Vec3 eye, Vec3 end, float tickDelta) {
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(pos);
        if (subLevel == null) {
            return shape.clip(eye, end, pos);
        }

        Pose3dc pose = subLevel.renderPose(tickDelta);
        BlockHitResult hit = shape.clip(pose.transformPositionInverse(eye), pose.transformPositionInverse(end), pos);
        return hit == null ? null : new BlockHitResult(pose.transformPosition(hit.getLocation()), hit.getDirection(), pos, hit.isInside());
    }

    public static BlockHitResult clip(ClipContext context, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        LevelPoseProviderExtension provider = (LevelPoseProviderExtension) client.level;
        provider.sable$pushPoseSupplier(subLevel -> ((ClientSubLevel) subLevel).renderPose(tickDelta));
        try {
            BlockHitResult hit = client.level.clip(context);
            if (hit.getType() == HitResult.Type.MISS) {
                return hit;
            }
            Vec3 point = Sable.HELPER.projectOutOfSubLevel(client.level, hit.getLocation());
            return new BlockHitResult(point, hit.getDirection(), hit.getBlockPos(), hit.isInside());
        } finally {
            provider.sable$popPoseSupplier();
        }
    }

    public static boolean isInSubLevel(int sectionX, int sectionZ) {
        return Sable.HELPER.getContainingClient(sectionX, sectionZ) != null;
    }
}
