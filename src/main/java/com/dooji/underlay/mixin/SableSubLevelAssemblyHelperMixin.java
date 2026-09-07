package com.dooji.underlay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dooji.underlay.main.UnderlayManager;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = SubLevelAssemblyHelper.class, remap = false)
public class SableSubLevelAssemblyHelperMixin {
    @Inject(method = "moveBlocks", at = @At("TAIL"))
    private static void moveOverlays(ServerLevel world, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        for (BlockPos pos : blocks) {
            if (!UnderlayManager.hasOverlay(world, pos)) {
                continue;
            }

            BlockState state = UnderlayManager.getOverlay(world, pos);
            UnderlayManager.removeOverlayAndBroadcast(world, pos);
            UnderlayManager.addOverlayFromContraption(transform.getLevel(), transform.apply(pos), transform.apply(state));
        }
    }
}
