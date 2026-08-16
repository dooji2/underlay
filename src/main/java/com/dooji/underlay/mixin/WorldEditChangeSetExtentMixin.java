package com.dooji.underlay.mixin;

import com.dooji.underlay.worldedit.UnderlayWorldEdit;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.ChangeSetExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.fabric.FabricWorld;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = ChangeSetExtent.class, remap = false)
public abstract class WorldEditChangeSetExtentMixin {
    @Shadow
    @Final
    private ChangeSet changeSet;

    @Inject(method = "setBlock", at = @At("HEAD"))
    private <B extends BlockStateHolder<B>> void setBlock(BlockVector3 location, B block, CallbackInfoReturnable<Boolean> cir) {
        Extent extent = ((ChangeSetExtent)(Object)this).getExtent();
        while (extent instanceof AbstractDelegateExtent delegate) {
            extent = delegate.getExtent();
        }

        if (!(extent instanceof FabricWorld fabricWorld) || !(fabricWorld.getWorld() instanceof ServerWorld world)) {
            return;
        }

        BlockPos blockPos = FabricAdapter.toBlockPos(location);
        UnderlayWorldEdit.removeOverlay(changeSet, world, blockPos);
    }
}
