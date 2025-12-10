package com.dooji.underlay.mixin;

import com.dooji.underlay.client.UnderlayClient;
import com.dooji.underlay.client.UnderlayManagerClient;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.state.IBlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void handleInitialBreaking(CallbackInfo ci) {
        Minecraft client = Minecraft.getMinecraft();
        BlockPos overlayPos = UnderlayClient.findOverlayUnderCrosshair(client);

        if (overlayPos != null) {
            UnderlayClient.breakOverlay(client, overlayPos);
            ci.cancel();
        }
    }

    @Inject(method = "middleClickMouse", at = @At("HEAD"), cancellable = true)
    private void onPickBlock(CallbackInfo ci) {
        Minecraft client = Minecraft.getMinecraft();
        BlockPos overlayPos = UnderlayClient.findOverlayUnderCrosshair(client);

        if (overlayPos != null && client.player != null && client.player.capabilities.isCreativeMode) {
            IBlockState underlayState = UnderlayManagerClient.getOverlay(overlayPos);
            if (underlayState != null) {
                ItemStack itemStack = new ItemStack(underlayState.getBlock(), 1, underlayState.getBlock().damageDropped(underlayState));
                client.player.inventory.setPickedItemStack(itemStack);
                client.playerController.sendSlotPacket(client.player.getHeldItem(EnumHand.MAIN_HAND), 36 + client.player.inventory.currentItem);
                ci.cancel();
            }
        }
    }
}
