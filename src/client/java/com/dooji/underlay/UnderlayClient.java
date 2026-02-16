package com.dooji.underlay;

import java.util.Map;

import com.dooji.underlay.flashback.FlashbackCompat;
import com.dooji.underlay.mixin.client.ClientPlayerInteractionManagerAccessor;
import com.dooji.underlay.network.payloads.AddOverlayPayload;
import com.dooji.underlay.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.network.payloads.SyncOverlaysPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class UnderlayClient implements ClientModInitializer {
	private static final boolean IS_FLASHBACK_INSTALLED = FabricLoader.getInstance().isModLoaded("flashback");

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		ClientPlayNetworking.registerGlobalReceiver(SyncOverlaysPayload.ID, (payload, context) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			client.execute(() -> {
				RegistryEntryLookup<Block> lookup = (RegistryEntryLookup<Block>) client.getNetworkHandler().getRegistryManager().getWrapperOrThrow(RegistryKeys.BLOCK);
				Map<BlockPos, BlockState> map = payload.tags().entrySet().stream()
					.collect(java.util.stream.Collectors.toMap(
						Map.Entry::getKey,
						e -> NbtHelper.toBlockState(lookup, e.getValue())
					));

				UnderlayManagerClient.sync(map);

				UnderlayRenderer.clearAllOverlays();
				UnderlayManagerClient.getAll().forEach(UnderlayRenderer::registerOverlay);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(AddOverlayPayload.ID, (payload, context) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			client.execute(() -> {
				RegistryEntryLookup<Block> lookup = (RegistryEntryLookup<Block>) client.getNetworkHandler().getRegistryManager().getWrapperOrThrow(RegistryKeys.BLOCK);

				BlockPos pos = payload.pos();
				BlockState state = NbtHelper.toBlockState(lookup, payload.stateTag());

				UnderlayManagerClient.syncAdd(pos, state);
				UnderlayRenderer.registerOverlay(pos, state);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(RemoveOverlayPayload.ID, (payload, context) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			client.execute(() -> {
				BlockPos pos = payload.pos();
				var state = UnderlayManagerClient.getOverlay(pos);

				UnderlayRenderer.unregisterOverlay(pos);
				UnderlayManagerClient.syncRemove(pos);
				client.world.playSound(client.player, pos, state.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1f, 1f);
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

	private void onClientTick(MinecraftClient client) {
		if (client.player == null || client.world == null) return;
		if (IS_FLASHBACK_INSTALLED) {
			FlashbackCompat.onClientTick(client);
		}

		if (client.currentScreen != null) return;

		handleContinuousBreaking(client);
	}

	private void handleContinuousBreaking(MinecraftClient client) {
		if (client.options.attackKey.isPressed()) {
			BlockPos hit = findOverlayUnderCrosshair(client);
			ClientPlayerInteractionManagerAccessor playerInteraction = (ClientPlayerInteractionManagerAccessor) client.interactionManager;
			if (hit != null && playerInteraction.getBlockBreakingCooldown() == 0) {
				breakOverlay(client, hit);
			}
		}
	}

	public static void breakOverlay(MinecraftClient client, BlockPos pos) {
		ClientPlayerInteractionManagerAccessor interactionManager = (ClientPlayerInteractionManagerAccessor) client.interactionManager;
		ClientPlayNetworking.send(new RemoveOverlayPayload(pos));
		interactionManager.setBlockBreakingCooldown(5);
	}

	public static BlockPos findOverlayUnderCrosshair(MinecraftClient client) {
		if (client.player == null) return null;

		BlockHitResult overlayHit = UnderlayRaycast.trace(client.player, client.player.getBlockInteractionRange(), client.getRenderTickCounter().getTickDelta(true));
		return overlayHit == null ? null : overlayHit.getBlockPos();
	}
}
