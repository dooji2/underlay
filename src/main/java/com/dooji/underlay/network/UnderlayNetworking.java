package com.dooji.underlay.network;

import java.util.HashMap;
import java.util.Map;

import com.dooji.underlay.UnderlayManager;
import com.dooji.underlay.network.payloads.AddOverlayPayload;
import com.dooji.underlay.network.payloads.PickItemFromOverlayPayload;
import com.dooji.underlay.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.network.payloads.SyncOverlaysPayload;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
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
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			syncOverlaysToPlayer((ServerPlayer) event.getEntity());
		});

		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
			syncOverlaysToPlayer((ServerPlayer) event.getEntity());
		});

		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
			syncOverlaysToPlayer((ServerPlayer) event.getEntity());
		});
	}

	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playToClient(SyncOverlaysPayload.ID, SyncOverlaysPayload.CODEC);
		registrar.playToClient(AddOverlayPayload.ID, AddOverlayPayload.CODEC);

		registrar.playBidirectional(RemoveOverlayPayload.ID, RemoveOverlayPayload.CODEC, (payload, context) -> {
			ServerPlayer player = (ServerPlayer) context.player();
			ServerLevel world = (ServerLevel) player.level();
			BlockPos pos = payload.pos();

			if (!world.mayInteract(player, pos)) {
				return;
			}

			if (UnderlayManager.hasOverlay(world, pos)) {
				BlockState old = UnderlayManager.getOverlay(world, pos);
				UnderlayManager.removeOverlay(world, pos);
				
				if (!player.isCreative()) {
					Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(old.getBlock()));
				}

				broadcastRemove(world, pos);
			}
		});

		registrar.playToServer(PickItemFromOverlayPayload.ID, PickItemFromOverlayPayload.CODEC, (payload, context) -> {
			ServerPlayer player = (ServerPlayer) context.player();
			ServerLevel world = (ServerLevel) player.level();
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

	}

	public static void syncOverlaysToPlayer(ServerPlayer player) {
		ServerLevel world = (ServerLevel) player.level();

		Map<BlockPos, CompoundTag> tags = new HashMap<>();
		UnderlayManager.getOverlaysFor(world).forEach((pos, state) ->
			tags.put(pos, NbtUtils.writeBlockState(state))
		);

		PacketDistributor.sendToPlayer(player, new SyncOverlaysPayload(tags));
	}

	public static void broadcastAdd(ServerLevel world, BlockPos pos) {
		CompoundTag tag = NbtUtils.writeBlockState(UnderlayManager.getOverlay(world, pos));
		AddOverlayPayload payload = new AddOverlayPayload(pos, tag);

		for (ServerPlayer p : world.players()) {
			PacketDistributor.sendToPlayer(p, payload);
		}
	}

	public static void broadcastRemove(ServerLevel world, BlockPos pos) {
		RemoveOverlayPayload payload = new RemoveOverlayPayload(pos);

		for (ServerPlayer p : world.players()) {
			PacketDistributor.sendToPlayer(p, payload);
		}
	}
}
