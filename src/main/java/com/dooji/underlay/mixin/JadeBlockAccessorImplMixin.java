package com.dooji.underlay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.client.UnderlayManagerClient;
import com.dooji.underlay.client.UnderlayRaycast;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import snownee.jade.impl.BlockAccessorImpl;

@Pseudo
@Mixin(value = BlockAccessorImpl.class, remap = false)
public class JadeBlockAccessorImplMixin {
    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void getOverlayBlockState(CallbackInfoReturnable<BlockState> cir) {
        BlockAccessorImpl self = (BlockAccessorImpl)(Object) this;
        BlockPos pos = self.getPosition();
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.level != null) {
            double reach = Player.DEFAULT_BLOCK_INTERACTION_RANGE;
            float tickDelta = (float)(client.getFrameTimeNs() / 50_000_000L);
            BlockHitResult hit = UnderlayRaycast.trace(client.player, reach, tickDelta);

            if (hit != null && hit.getBlockPos().equals(pos)) {
                BlockState overlayState = UnderlayManagerClient.getOverlay(pos);
                if (overlayState != null) {
                    cir.setReturnValue(overlayState);
                }
            }
        }
    }

    @Inject(method = "getBlock", at = @At("RETURN"), cancellable = true)
    private void getOverlayBlock(CallbackInfoReturnable<Block> cir) {
        BlockAccessorImpl self = (BlockAccessorImpl)(Object) this;
        BlockPos pos = self.getPosition();
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.level != null) {
            double reach = Player.DEFAULT_BLOCK_INTERACTION_RANGE;
            float tickDelta = (float)(client.getFrameTimeNs() / 50_000_000L);
            BlockHitResult hit = UnderlayRaycast.trace(client.player, reach, tickDelta);

            if (hit != null && hit.getBlockPos().equals(pos)) {
                BlockState overlayState = UnderlayManagerClient.getOverlay(pos);
                if (overlayState != null) {
                    cir.setReturnValue(overlayState.getBlock());
                }
            }
        }
    }

    @Inject(method = "getPickedResult", at = @At("RETURN"), cancellable = true)
    private void getOverlayPickedResult(CallbackInfoReturnable<ItemStack> cir) {
        BlockAccessorImpl self = (BlockAccessorImpl)(Object) this;
        BlockPos pos = self.getPosition();
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.level != null) {
            double reach = Player.DEFAULT_BLOCK_INTERACTION_RANGE;
            float tickDelta = (float)(client.getFrameTimeNs() / 50_000_000L);
            BlockHitResult hit = UnderlayRaycast.trace(client.player, reach, tickDelta);

            if (hit != null && hit.getBlockPos().equals(pos)) {
                BlockState overlayState = UnderlayManagerClient.getOverlay(pos);
                if (overlayState != null) {
                    Item overlayItem = overlayState.getBlock().asItem();
                    if (overlayItem != Items.AIR) {
                        cir.setReturnValue(new ItemStack(overlayItem));
                    }
                }
            }
        }
    }

    @Inject(method = "getFakeBlock", at = @At("RETURN"), cancellable = true)
    private void getOverlayFakeBlock(CallbackInfoReturnable<ItemStack> cir) {
        BlockAccessorImpl self = (BlockAccessorImpl)(Object) this;
        BlockPos pos = self.getPosition();
        Minecraft client = Minecraft.getInstance();

        if (client.player != null && client.level != null) {
            double reach = Player.DEFAULT_BLOCK_INTERACTION_RANGE;
            float tickDelta = (float)(client.getFrameTimeNs() / 50_000_000L);
            BlockHitResult hit = UnderlayRaycast.trace(client.player, reach, tickDelta);

            if (hit != null && hit.getBlockPos().equals(pos)) {
                BlockState overlayState = UnderlayManagerClient.getOverlay(pos);
                if (overlayState != null && overlayState.getBlock().asItem() instanceof BlockItem) {
                    cir.setReturnValue(new ItemStack(overlayState.getBlock().asItem()));
                }
            }
        }
    }
}
