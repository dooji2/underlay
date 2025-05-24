package com.dooji.underlay.main.events;

import com.dooji.underlay.main.network.UnderlayNetworking;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerEvents {
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UnderlayNetworking.syncOverlaysToPlayer(player);
        }
    }
}
