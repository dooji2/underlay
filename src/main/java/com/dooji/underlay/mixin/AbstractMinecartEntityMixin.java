package com.dooji.underlay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dooji.underlay.UnderlayManager;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(AbstractMinecartEntity.class)
public class AbstractMinecartEntityMixin {
    @Shadow private boolean onRail;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState redirectGetStateInTick(World world, BlockPos pos) {
        BlockState originalState = world.getBlockState(pos);

        if (originalState.getBlock() instanceof AbstractRailBlock) return originalState;
        if (UnderlayManager.hasOverlay(world, pos)) {
            BlockState overlayState = UnderlayManager.getOverlay(world, pos);
            if (overlayState != null && overlayState.getBlock() instanceof AbstractRailBlock) return overlayState;
        }

        return originalState;
    }

    @Redirect(method = "snapPositionToRail", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState redirectGetStateInSnap(World world, BlockPos pos) {
        return redirectGetStateInTick(world, pos);
    }

    @Redirect(method = "snapPositionToRailWithOffset", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState redirectGetStateInSnapOffset(World world, BlockPos pos) {
        return redirectGetStateInTick(world, pos);
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;onRail:Z", shift = At.Shift.AFTER))
    private void forceOnRail(CallbackInfo ci) {
        AbstractMinecartEntity self = (AbstractMinecartEntity)(Object)this;
        if (!this.onRail) {
            BlockPos posUnder = BlockPos.ofFloored(self.getX(), self.getY() - 1, self.getZ());
            World world = self.getWorld();

            if (UnderlayManager.hasOverlay(world, posUnder)) {
                BlockState overlayState = UnderlayManager.getOverlay(world, posUnder);
                if (overlayState != null && overlayState.getBlock() instanceof AbstractRailBlock) this.onRail = true;
            }
        }
    }
}
