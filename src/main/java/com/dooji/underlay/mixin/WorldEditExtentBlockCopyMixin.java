package com.dooji.underlay.mixin;

import com.dooji.underlay.main.worldedit.UnderlayWorldEdit;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.block.ExtentBlockCopy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = ExtentBlockCopy.class, remap = false)
public abstract class WorldEditExtentBlockCopyMixin {
    @Shadow
    @Final
    private Extent source;

    @Shadow
    @Final
    private Extent destination;

    @Shadow
    @Final
    private BlockVector3 from;

    @Shadow
    @Final
    private BlockVector3 to;

    @Shadow
    @Final
    private Transform transform;

    @Inject(method = "apply", at = @At("HEAD"))
    private void apply(BlockVector3 position, CallbackInfoReturnable<Boolean> cir) {
        UnderlayWorldEdit.beginCopy();
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void applyReturn(BlockVector3 position, CallbackInfoReturnable<Boolean> cir) {
        if (UnderlayWorldEdit.finishCopy(cir.getReturnValueZ())) {
            BlockVector3 transformed = transform.apply(position.subtract(from).toVector3()).toBlockPoint().add(to);
            UnderlayWorldEdit.copy(source, destination, position, transformed);
        }
    }
}
