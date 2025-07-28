package com.dooji.underlay.network;

import java.util.HashMap;
import java.util.Map;

import com.dooji.underlay.UnderlayManager;
import com.dooji.underlay.network.payloads.AddOverlayPayload;
import com.dooji.underlay.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.network.payloads.SyncOverlaysPayload;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;

public class UnderlayNetworking {
	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			syncOverlaysToPlayer(handler.getPlayer());
		});

		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) -> {
			syncOverlaysToPlayer(player);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			syncOverlaysToPlayer(newPlayer);
		});

		ServerPlayNetworking.registerGlobalReceiver(RemoveOverlayPayload.ID, (server, player, handler, buf, responseSender) -> {
			RemoveOverlayPayload payload = RemoveOverlayPayload.read(buf);

			server.execute(() -> {
				ServerWorld world = (ServerWorld) player.getWorld();
				BlockPos pos = payload.pos();

				if (!world.canPlayerModifyAt(player, pos)) {
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
		});
	}

	public static void syncOverlaysToPlayer(ServerPlayerEntity player) {
		var world = player.getWorld();
		Map<BlockPos, NbtCompound> tags = new HashMap<>();
		UnderlayManager.getOverlaysFor(world).forEach((pos, state) ->
			tags.put(pos, NbtHelper.fromBlockState(state))
		);

		if (!tags.isEmpty()) {
			PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
			SyncOverlaysPayload.write(buf, new SyncOverlaysPayload(tags));
			ServerPlayNetworking.send(player, SyncOverlaysPayload.ID, buf);
		}
	}

	public static void broadcastAdd(ServerWorld world, BlockPos pos) {
		var tag = NbtHelper.fromBlockState(UnderlayManager.getOverlay(world, pos));
		var payload = new AddOverlayPayload(pos, tag);

		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		AddOverlayPayload.write(buf, payload);

		for (ServerPlayerEntity p : world.getPlayers()) {
			PacketByteBuf copy = new PacketByteBuf(buf.copy());
			ServerPlayNetworking.send(p, AddOverlayPayload.ID, copy);
		}
	}

	private static void broadcastRemove(ServerWorld world, BlockPos pos) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		RemoveOverlayPayload.write(buf, new RemoveOverlayPayload(pos));

		for (ServerPlayerEntity p : world.getPlayers()) {
			PacketByteBuf copy = new PacketByteBuf(buf.copy());
			ServerPlayNetworking.send(p, RemoveOverlayPayload.ID, copy);
		}
	}
}
