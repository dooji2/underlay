package com.dooji.underlay.main.events;

import com.dooji.underlay.main.Underlay;
import com.dooji.underlay.main.network.UnderlayNetworking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Underlay.MOD_ID)
public class PlayerEvents {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UnderlayNetworking.syncOverlaysToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UnderlayNetworking.syncOverlaysToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UnderlayNetworking.syncOverlaysToPlayer(player);
        }
    }
}
