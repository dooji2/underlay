package com.dooji.underlay.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.VerticallyAttachableBlockItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.UnderlayManager;
import com.dooji.underlay.UnderlayApi;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Shadow
	public abstract ItemPlacementContext getPlacementContext(ItemPlacementContext context);

	@Inject(method = "place", at = @At("RETURN"), cancellable = true)
	private void handleOverlayPlacement(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
		ActionResult result = cir.getReturnValue();
		if (result != null && result.isAccepted()) {
			return;
		}

		BlockItem self = (BlockItem)(Object)this;
		Block block = self.getBlock();
		ItemPlacementContext placementContext = this.getPlacementContext(context);
		if (placementContext == null) {
			return;
		}

		BlockPos pos = placementContext.getBlockPos();
		World world = placementContext.getWorld();
		BlockState existing = world.getBlockState(pos);
		BlockState newState = resolveOverlayState(self, block, placementContext, context.getSide());

		if (newState != null && existing.getBlock() == block && newState.getBlock() == block) {
			return;
		}

		if (!world.isClient() && !UnderlayApi.isOverlayBlock((ServerWorld)world, block)) {
			return;
		}

		if (existing.isAir() || Block.isShapeFullCube(existing.getOutlineShape(world, pos)) || !existing.getFluidState().isEmpty()) {
			return;
		}

		BlockState overlay = resolveOverlayState(self, block, placementContext, context.getSide());
		if (overlay == null) {
			return;
		}

		ItemStack stack = context.getStack();

		if (!world.isClient()) {
			PlayerEntity player = context.getPlayer();

			if (!((ServerWorld)world).canEntityModifyAt(player, pos)) {
                cir.setReturnValue(ActionResult.FAIL);
                cir.cancel();

                return;
            }

			UnderlayManager.addOverlay((ServerPlayerEntity)context.getPlayer(), (ServerWorld)world, pos, overlay, block);

			if (player != null && !player.isCreative()) {
				stack.decrement(1);
			}

			BlockSoundGroup sounds = overlay.getSoundGroup();
			world.playSound(null, pos, sounds.getPlaceSound(), SoundCategory.BLOCKS, sounds.getVolume(), sounds.getPitch());
		}

		cir.setReturnValue(world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
		cir.cancel();
	}

	private static BlockState resolveOverlayState(BlockItem self, Block fallbackBlock, ItemPlacementContext context, Direction clickedSide) {
		if (self instanceof VerticallyAttachableBlockItem verticalItem) {
			VerticallyAttachableBlockItemAccessor accessor = (VerticallyAttachableBlockItemAccessor)(Object)verticalItem;
			Block wallBlock = accessor.getWallBlock();

			if (clickedSide.getAxis().isHorizontal()) {
				BlockState wallState = wallBlock.getPlacementState(context);
				if (wallState != null) {
					return wallState;
				}
			}
		}

		return fallbackBlock.getPlacementState(context);
	}
}
