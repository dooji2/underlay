package com.dooji.underlay.flashback;

import com.dooji.underlay.network.payloads.RequestOverlaySyncPayload;
import com.moulberry.flashback.Flashback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class FlashbackCompat {
    private static boolean requestedSyncThisRecording = false;

    private FlashbackCompat() {
    }

    public static void onClientTick(MinecraftClient client) {
        if (client.getNetworkHandler() == null) {
            return;
        }

        boolean recording = !Flashback.isInReplay() && Flashback.RECORDER != null;
        if (!recording) {
            requestedSyncThisRecording = false;
            return;
        }

        if (!requestedSyncThisRecording) {
            ClientPlayNetworking.send(new RequestOverlaySyncPayload());
            requestedSyncThisRecording = true;
        }
    }

    public static void onDisconnect() {
        requestedSyncThisRecording = false;
    }
}
