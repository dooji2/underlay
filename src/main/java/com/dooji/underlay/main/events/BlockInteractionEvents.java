package com.dooji.underlay.main.events;

import com.dooji.underlay.main.Underlay;
import com.dooji.underlay.main.UnderlayApi;
import com.dooji.underlay.main.UnderlayManager;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.network.play.server.SPacketAnimation;
import net.minecraft.world.WorldServer;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Underlay.MOD_ID)
public class BlockInteractionEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void handleOverlayPlacement(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) {
            return;
        }

        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        ItemStack stack = event.getItemStack();

        Item item = stack.getItem();
        if (!(item instanceof ItemBlock)) {
            return;
        }

        BlockPos basePos = event.getPos();
        EnumFacing face = event.getFace();
        if (face == null) {
            return;
        }

        WorldServer world = (WorldServer) event.getWorld();
        IBlockState baseState = world.getBlockState(basePos);
        BlockPos targetPos = baseState.getBlock().isReplaceable(world, basePos) ? basePos : basePos.offset(face);

        Vec3d hitVec = event.getHitVec();
        float hitX = hitVec != null ? (float) (hitVec.x - basePos.getX()) : 0.5F;
        float hitY = hitVec != null ? (float) (hitVec.y - basePos.getY()) : 0.5F;
        float hitZ = hitVec != null ? (float) (hitVec.z - basePos.getZ()) : 0.5F;

        if (!placeOverlay(world, player, stack, item, targetPos, face, hitX, hitY, hitZ)) {
            return;
        }

        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void handleOverlayPlacement(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) {
            return;
        }

        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        ItemStack stack = event.getItemStack();

        Item item = stack.getItem();
        if (!(item instanceof ItemBlock)) {
            return;
        }

        RayTraceResult hit = rayTraceFromPlayer(player, player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue(), 1.0F);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null) {
            return;
        }

        EnumFacing face = hit.sideHit;
        if (face == null) {
            return;
        }

        BlockPos basePos = hit.getBlockPos();
        WorldServer world = player.getServerWorld();
        IBlockState baseState = world.getBlockState(basePos);
        BlockPos targetPos = baseState.getBlock().isReplaceable(world, basePos) ? basePos : basePos.offset(face);

        float hitX = (float) (hit.hitVec.x - basePos.getX());
        float hitY = (float) (hit.hitVec.y - basePos.getY());
        float hitZ = (float) (hit.hitVec.z - basePos.getZ());

        if (!placeOverlay(world, player, stack, item, targetPos, face, hitX, hitY, hitZ)) {
            return;
        }

        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean placeOverlay(WorldServer world, EntityPlayerMP player, ItemStack stack, Item item, BlockPos targetPos, EnumFacing face, float hitX, float hitY, float hitZ) {
        ItemBlock itemBlock = (ItemBlock)item;
        Block block = itemBlock.getBlock();

        IBlockState newState = block.getStateForPlacement(world, targetPos, face, hitX, hitY, hitZ, stack.getMetadata(), player, EnumHand.MAIN_HAND);
        if (newState == null) {
            return false;
        }

        IBlockState existingState = world.getBlockState(targetPos);

        if (existingState.getBlock() == block && newState.getBlock() == block) {
            return false;
        }

        if (!UnderlayApi.isOverlayBlock(block)) {
            return false;
        }

        if (existingState.getBlock().isReplaceable(world, targetPos)) {
            return false;
        }

        if (existingState.getBlock().isAir(existingState, world, targetPos)) {
            return false;
        }

        if (existingState.isFullCube()) {
            return false;
        }

        // prevent overlay placement if there's fluid in the block for now
        if (existingState.getMaterial().isLiquid()) {
            return false;
        }

        if (!world.isBlockModifiable(player, targetPos)) {
            return false;
        }

        UnderlayManager.addOverlay(player, world, targetPos, newState, block);

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        SoundType sound = newState.getBlock().getSoundType(newState, world, targetPos, player);
        world.playSound(player, targetPos, sound.getPlaceSound(), SoundCategory.BLOCKS, sound.getVolume(), sound.getPitch());
        player.swingArm(EnumHand.MAIN_HAND);
        world.getEntityTracker().sendToTrackingAndSelf(player, new SPacketAnimation(player, 0));

        return true;
    }

    private static RayTraceResult rayTraceFromPlayer(EntityPlayer player, double reach, float partialTicks) {
        Vec3d eye = player.getPositionEyes(partialTicks);
        Vec3d look = player.getLook(partialTicks);
        Vec3d end = eye.addVector(look.x * reach, look.y * reach, look.z * reach);
        return player.world.rayTraceBlocks(eye, end, false, true, false);
    }
}
