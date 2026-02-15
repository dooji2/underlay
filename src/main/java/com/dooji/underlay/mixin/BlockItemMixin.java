package com.dooji.underlay.mixin;

import org.spongepowered.asm.mixin.Mixin;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import com.dooji.underlay.UnderlayApi;

@Mixin(BlockItem.class)
public class BlockItemMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void handleOverlayPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		BlockItem self = (BlockItem)(Object)this;
		Block block = self.getBlock();

		BlockPos pos = context.getClickedPos();
		Level world = context.getLevel();
		BlockState existing = world.getBlockState(pos);
		BlockState newState  = block.getStateForPlacement(context);

		if (newState != null && existing.getBlock() == block && newState.getBlock() == block) {
			return;
		}
		
		if (!world.isClientSide() && !UnderlayApi.isOverlayBlock((ServerLevel)world, block)) {
			return;
		}

		if (existing.isAir() || Block.isShapeFullBlock(existing.getShape(world, pos)) || context.getClickedFace() != Direction.UP || !existing.getFluidState().isEmpty()) {
			return;
		}

		BlockState overlay = block.getStateForPlacement(context);
		ItemStack stack = context.getItemInHand();

		if (!world.isClientSide()) {
			UnderlayManager.addOverlay((ServerPlayer)context.getPlayer(), (ServerLevel)world, pos, overlay);
			Player player = context.getPlayer();

			if (!world.mayInteract(player, pos)) {
                cir.setReturnValue(InteractionResult.FAIL);
                cir.cancel();

                return;
            }

			if (player != null && !player.isCreative()) {
				stack.shrink(1);
			}

			SoundType sounds = overlay.getSoundType();
			world.playSound(null, pos, sounds.getPlaceSound(), SoundSource.BLOCKS, sounds.getVolume(), sounds.getPitch());
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
		cir.cancel();
	}
}
