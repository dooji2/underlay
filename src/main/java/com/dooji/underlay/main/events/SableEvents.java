package com.dooji.underlay.main.events;

import java.util.List;

import com.dooji.underlay.main.UnderlayManager;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.neoforge.event.ForgeSableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;

public class SableEvents implements SubLevelObserver {
    @SubscribeEvent
    public void onSubLevelContainerReady(ForgeSableSubLevelContainerReadyEvent event) {
        event.getContainer().addObserver(this);
    }

    @Override
    public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
        if (reason != SubLevelRemovalReason.REMOVED || !(subLevel.getLevel() instanceof ServerLevel world)) {
            return;
        }

        List<BlockPos> overlays = UnderlayManager.getOverlaysFor(world).keySet().stream()
                .filter(pos -> subLevel.getPlot().contains(new ChunkPos(pos)))
                .toList();

        overlays.forEach(pos -> UnderlayManager.removeOverlayAndBroadcast(world, pos));
    }
}
