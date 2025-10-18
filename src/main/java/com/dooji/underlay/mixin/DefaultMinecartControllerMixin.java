package com.dooji.underlay.mixin;

import com.dooji.underlay.UnderlayManager;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.vehicle.DefaultMinecartController;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DefaultMinecartController.class)
public abstract class DefaultMinecartControllerMixin {
    @Redirect(
            method = {
                    "tick",
                    "moveOnRail",
                    "simulateMovement",
                    "snapPositionToRail"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"
            )
    )
    private BlockState redirectGetBlockState(World world, BlockPos pos) {
        BlockState originalState = world.getBlockState(pos);

        if (originalState.getBlock() instanceof AbstractRailBlock) {
            return originalState;
        }

        if (UnderlayManager.hasOverlay(world, pos)) {
            BlockState overlayState = UnderlayManager.getOverlay(world, pos);

            if (overlayState != null && overlayState.getBlock() instanceof AbstractRailBlock) {
                return overlayState;
            }
        }

        return originalState;
    }
}
