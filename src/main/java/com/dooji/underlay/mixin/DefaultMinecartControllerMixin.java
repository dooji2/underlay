package com.dooji.underlay.mixin;

import com.dooji.underlay.UnderlayManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(OldMinecartBehavior.class)
public abstract class DefaultMinecartControllerMixin {
    @Redirect(
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
    private BlockState redirectGetBlockState(Level world, BlockPos pos) {
        BlockState originalState = world.getBlockState(pos);

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
