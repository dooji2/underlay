package com.dooji.underlay.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.UnderlayManagerClient;
import com.dooji.underlay.UnderlayRaycast;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import snownee.jade.impl.BlockAccessorImpl;

@Mixin(BlockAccessorImpl.class)
public class BlockAccessorImplMixin {
    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true, remap = false)
    private void getOverlayBlockState(CallbackInfoReturnable<BlockState> cir) {
        BlockAccessorImpl self = (BlockAccessorImpl)(Object)this;
        BlockPos pos = self.getPosition();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            BlockHitResult hit = UnderlayRaycast.trace(client.player, client.player.getBlockInteractionRange(), client.getRenderTickCounter().getDynamicDeltaTicks());
            if (hit != null && hit.getBlockPos().equals(pos)) {
                BlockState overlayState = UnderlayManagerClient.getOverlay(pos);
                if (overlayState != null) cir.setReturnValue(overlayState);
            }
        }
    }

    @Inject(method = "getBlock", at = @At("RETURN"), cancellable = true, remap = false)
    private void getOverlayBlock(CallbackInfoReturnable<Block> cir) {
        BlockAccessorImpl self = (BlockAccessorImpl)(Object)this;
        BlockPos pos = self.getPosition();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            BlockHitResult hit = UnderlayRaycast.trace(client.player, client.player.getBlockInteractionRange(), client.getRenderTickCounter().getDynamicDeltaTicks());
            if (hit != null && hit.getBlockPos().equals(pos)) {
                BlockState overlayState = UnderlayManagerClient.getOverlay(pos);
                if (overlayState != null) cir.setReturnValue(overlayState.getBlock());
            }
        }
    }

    @Inject(method = "getPickedResult", at = @At("RETURN"), cancellable = true, remap = false)
    private void getOverlayPickedResult(CallbackInfoReturnable<ItemStack> cir) {
        BlockAccessorImpl self = (BlockAccessorImpl)(Object)this;
        BlockPos pos = self.getPosition();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            BlockHitResult hit = UnderlayRaycast.trace(client.player, client.player.getBlockInteractionRange(), client.getRenderTickCounter().getDynamicDeltaTicks());
            if (hit != null && hit.getBlockPos().equals(pos)) {
                BlockState overlayState = UnderlayManagerClient.getOverlay(pos);
                if (overlayState != null) cir.setReturnValue(new ItemStack(overlayState.getBlock().asItem()));
            }
        }
    }

    @Inject(method = "getFakeBlock", at = @At("RETURN"), cancellable = true, remap = false)
    private void getOverlayFakeBlock(CallbackInfoReturnable<ItemStack> cir) {
        BlockAccessorImpl self = (BlockAccessorImpl)(Object)this;
        BlockPos pos = self.getPosition();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            BlockHitResult hit = UnderlayRaycast.trace(client.player, client.player.getBlockInteractionRange(), client.getRenderTickCounter().getDynamicDeltaTicks());
            if (hit != null && hit.getBlockPos().equals(pos)) {
                BlockState overlayState = UnderlayManagerClient.getOverlay(pos);
                if (overlayState != null && overlayState.getBlock().asItem() instanceof BlockItem) {
                    cir.setReturnValue(new ItemStack(overlayState.getBlock().asItem()));
                }
            }
        }
    }
}
