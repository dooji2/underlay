package com.dooji.underlay;

import java.util.Map;
import java.util.stream.Collectors;

import com.dooji.underlay.jade.JadeComponents;
import com.dooji.underlay.mixin.client.ClientPlayerInteractionManagerAccessor;
import com.dooji.underlay.network.payloads.AddOverlayPayload;
import com.dooji.underlay.network.payloads.RemoveOverlayPayload;
import com.dooji.underlay.network.payloads.SyncOverlaysPayload;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;

@Mod(value = Underlay.MOD_ID, dist = Dist.CLIENT)
public class UnderlayClient {
	public UnderlayClient(IEventBus modEventBus) {
		modEventBus.addListener(this::registerPayloads);
		onInitializeClient();
	}

	public void onInitializeClient() {
		if (ModList.get().isLoaded("jade")) {
			JadeComponents.init();
		}

		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> onClientTick(Minecraft.getInstance()));

		UnderlayRenderer.init();
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
			UnderlayRenderer.clearAllOverlays();
			UnderlayManagerClient.removeAll();
		});
	}

	private void registerPayloads(RegisterClientPayloadHandlersEvent event) {
		event.register(SyncOverlaysPayload.ID, (payload, context) -> {
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				HolderLookup<Block> lookup = client.getConnection().registryAccess().lookupOrThrow(Registries.BLOCK);
				Map<BlockPos, BlockState> map = payload.tags().entrySet().stream()
					.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> NbtUtils.readBlockState(lookup, e.getValue())
					));

				UnderlayManagerClient.sync(map);
				UnderlayRenderer.forceRefresh();
			});
		});

		event.register(AddOverlayPayload.ID, (payload, context) -> {
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				HolderLookup<Block> lookup = client.getConnection().registryAccess().lookupOrThrow(Registries.BLOCK);

				BlockPos pos = payload.pos();
				BlockState state = NbtUtils.readBlockState(lookup, payload.stateTag());

				UnderlayManagerClient.syncAdd(pos, state);
				UnderlayRenderer.registerOverlay(pos, state);
			});
		});

		event.register(RemoveOverlayPayload.ID, (payload, context) -> {
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				BlockPos pos = payload.pos();
				BlockState state = UnderlayManagerClient.getOverlay(pos);

				UnderlayRenderer.unregisterOverlay(pos);
				UnderlayManagerClient.syncRemove(pos);
				client.level.playSound(client.player, pos, state.getSoundType(client.level, pos, client.player).getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
			});
		});
	}

	private void onClientTick(Minecraft client) {
		if (client.player == null || client.level == null) return;

		if (client.gui.screen() != null) return;

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
		ClientPacketDistributor.sendToServer(new RemoveOverlayPayload(pos));
		interactionManager.setBlockBreakingCooldown(5);
	}

	public static BlockPos findOverlayUnderCrosshair(Minecraft client) {
		if (client.player == null) return null;

		BlockHitResult overlayHit = UnderlayRaycast.trace(client.player, client.player.blockInteractionRange(), client.getDeltaTracker().getGameTimeDeltaTicks());
		return overlayHit == null ? null : overlayHit.getBlockPos();
	}
}
