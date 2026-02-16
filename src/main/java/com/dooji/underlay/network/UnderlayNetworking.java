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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

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
			ServerPlayer player = context.player();
			ServerLevel world = player.level();
			BlockPos pos = payload.pos();

			if (!world.mayInteract(player, pos)) {
				return;
			}

			if (UnderlayManager.hasOverlay(world, pos)) {
				var old = UnderlayManager.getOverlay(world, pos);
				UnderlayManager.removeOverlay(world, pos);
				
				if (!player.isCreative()) {
					Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(old.getBlock()));
				}

				broadcastRemove(world, pos);
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(PickItemFromOverlayPayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			ServerLevel world = player.level();
			BlockPos pos = payload.pos();

			if (!world.mayInteract(player, pos)) {
				return;
			}

			if (UnderlayManager.hasOverlay(world, pos)) {
				BlockState overlayState = UnderlayManager.getOverlay(world, pos);
				ItemStack itemStack = overlayState.getCloneItemStack(world, pos, player.isCreative());
				
				if (!itemStack.isEmpty()) {
					if (itemStack.isItemEnabled(world.enabledFeatures())) {
						Inventory playerInventory = player.getInventory();
						int slotWithStack = playerInventory.findSlotMatchingItem(itemStack);
						
						if (slotWithStack != -1) {
							if (Inventory.isHotbarSlot(slotWithStack)) {
								playerInventory.setSelectedSlot(slotWithStack);
							} else {
								playerInventory.pickSlot(slotWithStack);
							}
						} else if (player.isCreative()) {
							playerInventory.addAndPickItem(itemStack);
						}

						player.connection.send(new ClientboundSetHeldSlotPacket(playerInventory.getSelectedSlot()));
						player.inventoryMenu.broadcastChanges();
					}
				}
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(RequestOverlaySyncPayload.ID, (payload, context) -> {
			syncOverlaysToPlayer(context.player());
		});
	}

	public static void syncOverlaysToPlayer(ServerPlayer player) {
		var world = player.level();

		Map<BlockPos, CompoundTag> tags = new HashMap<>();
		UnderlayManager.getOverlaysFor(world).forEach((pos, state) ->
			tags.put(pos, NbtUtils.writeBlockState(state))
		);

		ServerPlayNetworking.send(player, new SyncOverlaysPayload(tags));
	}

	public static void broadcastAdd(ServerLevel world, BlockPos pos) {
		var tag = NbtUtils.writeBlockState(UnderlayManager.getOverlay(world, pos));
		var payload = new AddOverlayPayload(pos, tag);

		for (ServerPlayer p : world.players()) {
			ServerPlayNetworking.send(p, payload);
		}
	}

	private static void broadcastRemove(ServerLevel world, BlockPos pos) {
		var payload = new RemoveOverlayPayload(pos);

		for (ServerPlayer p : world.players()) {
			ServerPlayNetworking.send(p, payload);
		}
	}
}
