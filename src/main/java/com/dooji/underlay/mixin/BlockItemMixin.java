package com.dooji.underlay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.UnderlayManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import com.dooji.underlay.UnderlayApi;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Shadow
	public abstract BlockPlaceContext updatePlacementContext(BlockPlaceContext context);

	@Inject(method = "place", at = @At("RETURN"), cancellable = true)
	private void handleOverlayPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		InteractionResult result = cir.getReturnValue();
		if (result != null && result.consumesAction()) {
			return;
		}

		BlockItem self = (BlockItem)(Object)this;
		Block block = self.getBlock();
		BlockPlaceContext placementContext = this.updatePlacementContext(context);
		if (placementContext == null) {
			return;
		}

		BlockPos pos = placementContext.getClickedPos();
		Level world = placementContext.getLevel();
		BlockState existing = world.getBlockState(pos);
		BlockState newState = resolveOverlayState(self, block, placementContext, context.getClickedFace());

		if (newState != null && existing.getBlock() == block && newState.getBlock() == block) {
			return;
		}

		if (!world.isClientSide() && !UnderlayApi.isOverlayBlock((ServerLevel)world, block)) {
			return;
		}

		if (existing.isAir() || Block.isShapeFullBlock(existing.getShape(world, pos)) || !existing.getFluidState().isEmpty()) {
			return;
		}

		BlockState overlay = resolveOverlayState(self, block, placementContext, context.getClickedFace());
		if (overlay == null) {
			return;
		}

		ItemStack stack = context.getItemInHand();

		if (!world.isClientSide()) {
			Player player = context.getPlayer();
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return;
			}

			if (!world.mayInteract(player, pos)) {
				cir.setReturnValue(InteractionResult.FAIL);
				cir.cancel();
				return;
			}

			UnderlayManager.addOverlay(serverPlayer, (ServerLevel)world, pos, overlay, block);

			if (!player.isCreative()) {
				stack.shrink(1);
			}

			SoundType sounds = overlay.getSoundType();
			world.playSound(null, pos, sounds.getPlaceSound(), SoundSource.BLOCKS, sounds.getVolume(), sounds.getPitch());
		}

		cir.setReturnValue(world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
		cir.cancel();
	}

	private static BlockState resolveOverlayState(BlockItem self, Block fallbackBlock, BlockPlaceContext context, Direction clickedSide) {
		if (self instanceof StandingAndWallBlockItem verticalItem) {
			VerticallyAttachableBlockItemAccessor accessor = (VerticallyAttachableBlockItemAccessor)(Object)verticalItem;
			Block wallBlock = accessor.getWallBlock();

			if (clickedSide.getAxis().isHorizontal()) {
				BlockState wallState = wallBlock.getStateForPlacement(context);
				if (wallState != null) {
					return wallState;
				}
			}
		}

		return fallbackBlock.getStateForPlacement(context);
	}
}
