package com.dooji.underlay.main.events;

import com.dooji.underlay.main.Underlay;
import com.dooji.underlay.main.UnderlayApi;
import com.dooji.underlay.main.UnderlayManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Underlay.MOD_ID)
public class BlockInteractionEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void handleOverlayPlacement(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof BlockItem)) {
            return;
        }

        BlockItem blockItem = (BlockItem) stack.getItem();
        Block block = blockItem.getBlock();

        Direction face = event.getFace();
        BlockPos clickedPos = event.getPos();
        BlockPos targetPos;

        if (face == Direction.DOWN) {
            targetPos = clickedPos;
        } else if (face == Direction.UP) {
            targetPos = clickedPos.above();
        } else {
            return;
        }

        ServerLevel world = (ServerLevel) event.getLevel();

        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player, event.getHand(), event.getHitVec()));
        BlockState newState = block.getStateForPlacement(context);
        BlockState existingState = world.getBlockState(targetPos);

        if (newState != null && existingState.getBlock() == block && newState.getBlock() == block) {
            return;
        }

        if (!UnderlayApi.isOverlayBlock(block)) {
            return;
        }

        if (existingState.isAir()) {
            return;
        }

        if (Block.isShapeFullBlock(existingState.getShape(world, targetPos))) {
            return;
        }

        if (!world.mayInteract(player, targetPos)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        UnderlayManager.addOverlay(player, world, targetPos, newState);

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        var sound = newState.getSoundType();
        world.playSound(player, targetPos, sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
    }
}
