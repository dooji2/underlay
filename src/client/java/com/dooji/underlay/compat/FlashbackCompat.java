package com.dooji.underlay.compat;

import com.dooji.underlay.network.payloads.RequestOverlaySyncPayload;
import com.moulberry.flashback.Flashback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class FlashbackCompat {
    private static boolean requestedSyncThisRecording = false;

    private FlashbackCompat() {
    }

    public static void onClientTick(Minecraft client) {
        if (client.getConnection() == null) {
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
