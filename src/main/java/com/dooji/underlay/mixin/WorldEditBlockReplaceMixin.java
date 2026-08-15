package com.dooji.underlay.mixin;

import com.dooji.underlay.main.worldedit.UnderlayWorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.neoforge.NeoForgeAdapter;
import com.sk89q.worldedit.neoforge.NeoForgeWorld;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = BlockReplace.class, remap = false)
public abstract class WorldEditBlockReplaceMixin {
    @Shadow
    @Final
    private Extent extent;

    @Shadow
    @Final
    private Pattern pattern;

    @Inject(method = "apply", at = @At("HEAD"))
    private void apply(BlockVector3 position, CallbackInfoReturnable<Boolean> cir) {
        UnderlayWorldEdit.beginCopy();
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void applyReturn(BlockVector3 position, CallbackInfoReturnable<Boolean> cir) {
        if (!UnderlayWorldEdit.finishCopy(cir.getReturnValueZ())) {
            return;
        }

        if (pattern instanceof Extent source) {
            UnderlayWorldEdit.copy(source, extent, position, position);
            return;
        }

        if (extent instanceof EditSession editSession && editSession.getWorld() instanceof NeoForgeWorld neoForgeWorld) {
            ServerLevel world = neoForgeWorld.getWorld();
            UnderlayWorldEdit.removeOverlay(editSession.getChangeSet(), world, NeoForgeAdapter.toBlockPos(position));
        }
    }
}
