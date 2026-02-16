package com.dooji.underlay.network;

import java.util.HashMap;
import java.util.Map;

import com.dooji.underlay.UnderlayManager;
import com.dooji.underlay.network.payloads.AddOverlayPayload;
import com.dooji.underlay.network.payloads.PickItemFromOverlayPayload;
import com.dooji.underlay.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.network.payloads.RequestOverlaySyncPayload;
import com.dooji.underlay.network.payloads.SyncOverlaysPayload;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;

public class UnderlayNetworking {
	public static void init() {
		PayloadTypeRegistry.playS2C().register(SyncOverlaysPayload.ID, SyncOverlaysPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(AddOverlayPayload.ID, AddOverlayPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RemoveOverlayPayload.ID, RemoveOverlayPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RemoveOverlayPayload.ID, RemoveOverlayPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(PickItemFromOverlayPayload.ID, PickItemFromOverlayPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RequestOverlaySyncPayload.ID, RequestOverlaySyncPayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			syncOverlaysToPlayer(handler.getPlayer());
		});

		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> {
			syncOverlaysToPlayer(player);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			syncOverlaysToPlayer(newPlayer);
		});

		ServerPlayNetworking.registerGlobalReceiver(RemoveOverlayPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ServerWorld world = player.getEntityWorld();
			BlockPos pos = payload.pos();

			if (!world.canEntityModifyAt(player, pos)) {
				return;
			}

			if (UnderlayManager.hasOverlay(world, pos)) {
				var old = UnderlayManager.getOverlay(world, pos);
				UnderlayManager.removeOverlay(world, pos);
				
				if (!player.isCreative()) {
					ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(old.getBlock()));
				}

				broadcastRemove(world, pos);
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(PickItemFromOverlayPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			ServerWorld world = player.getEntityWorld();
			BlockPos pos = payload.pos();

			if (!world.canEntityModifyAt(player, pos)) {
				return;
			}

			if (UnderlayManager.hasOverlay(world, pos)) {
				BlockState overlayState = UnderlayManager.getOverlay(world, pos);
				ItemStack itemStack = overlayState.getPickStack(world, pos, player.isCreative());
				
				if (!itemStack.isEmpty()) {
					if (itemStack.isItemEnabled(world.getEnabledFeatures())) {
						PlayerInventory playerInventory = player.getInventory();
						int slotWithStack = playerInventory.getSlotWithStack(itemStack);
						
						if (slotWithStack != -1) {
							if (PlayerInventory.isValidHotbarIndex(slotWithStack)) {
								playerInventory.setSelectedSlot(slotWithStack);
							} else {
								playerInventory.swapSlotWithHotbar(slotWithStack);
							}
						} else if (player.isCreative()) {
							playerInventory.swapStackWithHotbar(itemStack);
						}

						player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(playerInventory.getSelectedSlot()));
						player.playerScreenHandler.sendContentUpdates();
					}
				}
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(RequestOverlaySyncPayload.ID, (payload, context) -> {
			syncOverlaysToPlayer(context.player());
		});
	}

	public static void syncOverlaysToPlayer(ServerPlayerEntity player) {
		var world = player.getEntityWorld();

		Map<BlockPos, NbtCompound> tags = new HashMap<>();
		UnderlayManager.getOverlaysFor(world).forEach((pos, state) ->
			tags.put(pos, NbtHelper.fromBlockState(state))
		);

		ServerPlayNetworking.send(player, new SyncOverlaysPayload(tags));
	}

	public static void broadcastAdd(ServerWorld world, BlockPos pos) {
		var tag = NbtHelper.fromBlockState(UnderlayManager.getOverlay(world, pos));
		var payload = new AddOverlayPayload(pos, tag);

		for (ServerPlayerEntity p : world.getPlayers()) {
			ServerPlayNetworking.send(p, payload);
		}
	}

	private static void broadcastRemove(ServerWorld world, BlockPos pos) {
		var payload = new RemoveOverlayPayload(pos);

		for (ServerPlayerEntity p : world.getPlayers()) {
			ServerPlayNetworking.send(p, payload);
		}
	}
}
