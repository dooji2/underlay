package com.dooji.underlay.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.UnderlayManagerClient;
import com.dooji.underlay.UnderlayRaycast;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import snownee.jade.api.AccessorImpl;
import snownee.jade.impl.BlockAccessorImpl;

@Mixin(AccessorImpl.class)
public class AccessorImplMixin {
    @Inject(method = "getServersideRep", at = @At("RETURN"), cancellable = true, remap = false)
    private void getOverlayServersideRep(CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object)this instanceof BlockAccessorImpl self)) return;
        BlockPos pos = self.getPosition();
        Minecraft client = Minecraft.getInstance();

        if (client.player != null) {
            BlockHitResult hit = UnderlayRaycast.trace(client.player, client.player.blockInteractionRange(), client.getDeltaTracker().getGameTimeDeltaTicks());
            if (hit != null && hit.getBlockPos().equals(pos)) {
                BlockState overlayState = UnderlayManagerClient.getOverlay(pos);
                if (overlayState != null && overlayState.getBlock().asItem() instanceof BlockItem) {
                    cir.setReturnValue(new ItemStack(overlayState.getBlock().asItem()));
                }
            }
        }
    }
}
