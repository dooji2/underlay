package com.dooji.underlay.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.UnderlayManager;
import com.dooji.underlay.UnderlayApi;

@Mixin(BlockItem.class)
public class BlockItemMixin {

	@Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"), cancellable = true)
	private void handleOverlayPlacement(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
		BlockItem self = (BlockItem)(Object)this;
		Block block = self.getBlock();

		BlockPos pos = context.getBlockPos();
		World world = context.getWorld();
		BlockState existing = world.getBlockState(pos);
		BlockState newState  = block.getPlacementState(context);

		if (newState != null && existing.getBlock() == block && newState.getBlock() == block) {
			return;
		}
		
		if (!UnderlayApi.isOverlayBlock(block)) {
			return;
		}

		if (existing.isAir() || Block.isShapeFullCube(existing.getOutlineShape(world, pos)) || context.getSide() != Direction.UP) {
			return;
		}

		BlockState overlay = block.getPlacementState(context);
		ItemStack stack = context.getStack();

		if (!world.isClient()) {
			UnderlayManager.addOverlay((ServerPlayerEntity)context.getPlayer(), world, pos, overlay);
			PlayerEntity player = context.getPlayer();

			if (player != null && !player.isCreative()) {
				stack.decrement(1);
			}

			BlockSoundGroup sounds = overlay.getSoundGroup();
			world.playSound(null, pos, sounds.getPlaceSound(), SoundCategory.BLOCKS, sounds.getVolume(), sounds.getPitch());
		}

		cir.setReturnValue(ActionResult.success(world.isClient()));
		cir.cancel();
	}
}
