package com.dooji.underlay.mixin;

import com.dooji.underlay.UnderlayManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OldMinecartBehavior.class)
public abstract class DefaultMinecartControllerMixin {
    @WrapOperation(
            method = {
                    "tick",
                    "moveAlongTrack",
                    "getPosOffs",
                    "getPos"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            )
    )
    private BlockState wrapGetBlockState(Level world, BlockPos pos, Operation<BlockState> original) {
        BlockState originalState = original.call(world, pos);

        if (originalState.getBlock() instanceof BaseRailBlock) {
            return originalState;
        }

        if (UnderlayManager.hasOverlay(world, pos)) {
            BlockState overlayState = UnderlayManager.getOverlay(world, pos);

            if (overlayState != null && overlayState.getBlock() instanceof BaseRailBlock) {
                return overlayState;
            }
        }

        return originalState;
    }
}
