package com.dooji.underlay;

import java.util.Map;

import com.dooji.underlay.compat.FlashbackCompat;
import com.dooji.underlay.mixin.client.ClientPlayerInteractionManagerAccessor;
import com.dooji.underlay.network.payloads.AddOverlayPayload;
import com.dooji.underlay.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.network.payloads.SyncOverlaysPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;

public class UnderlayClient implements ClientModInitializer {
	private static final boolean IS_FLASHBACK_INSTALLED = FabricLoader.getInstance().isModLoaded("flashback");

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		ClientPlayNetworking.registerGlobalReceiver(SyncOverlaysPayload.ID, (payload, context) -> {
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				HolderLookup<Block> lookup = client.getConnection().registryAccess().lookupOrThrow(Registries.BLOCK);
				Map<BlockPos, BlockState> map = payload.tags().entrySet().stream()
					.collect(java.util.stream.Collectors.toMap(
						Map.Entry::getKey,
						e -> NbtUtils.readBlockState(lookup, e.getValue())
					));

				UnderlayManagerClient.sync(map);

				UnderlayRenderer.clearAllOverlays();
				UnderlayManagerClient.getAll().forEach(UnderlayRenderer::registerOverlay);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(AddOverlayPayload.ID, (payload, context) -> {
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				HolderLookup<Block> lookup = client.getConnection().registryAccess().lookupOrThrow(Registries.BLOCK);

				BlockPos pos = payload.pos();
				BlockState state = NbtUtils.readBlockState(lookup, payload.stateTag());

				UnderlayManagerClient.syncAdd(pos, state);
				UnderlayRenderer.registerOverlay(pos, state);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(RemoveOverlayPayload.ID, (payload, context) -> {
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				BlockPos pos = payload.pos();
				var state = UnderlayManagerClient.getOverlay(pos);

				UnderlayRenderer.unregisterOverlay(pos);
				UnderlayManagerClient.syncRemove(pos);
				client.level.playSound(client.player, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
			});
		});

		UnderlayRenderer.init();
		ClientPlayConnectionEvents.DISCONNECT.register((handler, cli) -> {
			UnderlayRenderer.clearAllOverlays();
			UnderlayManagerClient.removeAll();

			if (IS_FLASHBACK_INSTALLED) {
				FlashbackCompat.onDisconnect();
			}
		});
	}

	private void onClientTick(Minecraft client) {
		if (client.player == null || client.level == null) return;
		if (IS_FLASHBACK_INSTALLED) {
			FlashbackCompat.onClientTick(client);
		}

		if (client.screen != null) return;

		handleContinuousBreaking(client);
	}

	private void handleContinuousBreaking(Minecraft client) {
		if (client.options.keyAttack.isDown()) {
			BlockPos hit = findOverlayUnderCrosshair(client);
			ClientPlayerInteractionManagerAccessor playerInteraction = (ClientPlayerInteractionManagerAccessor) client.gameMode;
			if (hit != null && playerInteraction.getBlockBreakingCooldown() == 0) {
				breakOverlay(client, hit);
			}
		}
	}

	public static void breakOverlay(Minecraft client, BlockPos pos) {
		ClientPlayerInteractionManagerAccessor interactionManager = (ClientPlayerInteractionManagerAccessor) client.gameMode;
		ClientPlayNetworking.send(new RemoveOverlayPayload(pos));
		interactionManager.setBlockBreakingCooldown(5);
	}

	public static BlockPos findOverlayUnderCrosshair(Minecraft client) {
		if (client.player == null) return null;

		BlockHitResult overlayHit = UnderlayRaycast.trace(client.player, client.player.blockInteractionRange(), client.getDeltaTracker().getGameTimeDeltaTicks());
		return overlayHit == null ? null : overlayHit.getBlockPos();
	}
}
