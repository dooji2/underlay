package com.dooji.underlay.worldedit;

import java.util.Map;
import java.util.WeakHashMap;

import com.dooji.underlay.UnderlayManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.buffer.ForgetfulExtentBuffer;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.collection.BlockMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class UnderlayWorldEdit {
    private static final Map<Extent, Map<BlockVector3, BlockState>> EXTENT_OVERLAYS = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> COPY_ACCEPTED = new ThreadLocal<>();

    public static void beginCopy() {
        COPY_ACCEPTED.set(false);
    }

    public static boolean finishCopy(boolean changed) {
        boolean accepted = changed || Boolean.TRUE.equals(COPY_ACCEPTED.get());
        COPY_ACCEPTED.remove();
        return accepted;
    }

    public static void copy(Extent source, Extent destination, BlockVector3 sourcePosition, BlockVector3 destinationPosition) {
        BlockState overlay = getOverlay(source, sourcePosition);
        if (overlay == null) {
            return;
        }

        placeOverlay(destination, destinationPosition, overlay);
    }

    public static void removeOverlay(ChangeSet changeSet, ServerLevel world, BlockPos pos) {
        if (COPY_ACCEPTED.get() != null) {
            COPY_ACCEPTED.set(true);
        }

        BlockState overlay = UnderlayManager.getOverlaysFor(world).get(pos);
        if (overlay == null) {
            return;
        }

        changeSet.add(new OverlayChange(world, pos, overlay, null));
        setOverlay(world, pos, null);
    }

    private static BlockState getOverlay(Extent extent, BlockVector3 position) {
        Map<BlockVector3, BlockState> overlays = EXTENT_OVERLAYS.get(extent);
        if (overlays != null) {
            return overlays.get(position);
        }

        if (extent instanceof BlockTransformExtent transformExtent) {
            BlockState overlay = getOverlay(transformExtent.getExtent(), position);
            return overlay == null ? null : FabricAdapter.adapt(BlockTransformExtent.transform(FabricAdapter.adapt(overlay), transformExtent.getTransform()));
        }

        if (extent instanceof EditSession editSession) {
            Level world = FabricAdapter.adapt(editSession.getWorld());
            return UnderlayManager.getOverlaysFor(world).get(FabricAdapter.toBlockPos(position));
        }

        return null;
    }

    private static void placeOverlay(Extent extent, BlockVector3 position, BlockState overlay) {
        if (extent instanceof ForgetfulExtentBuffer buffer && !buffer.asRegion().contains(position)) {
            placeOverlay(buffer.getExtent(), position, overlay);
            return;
        }

        if (!(extent instanceof EditSession editSession)) {
            EXTENT_OVERLAYS.computeIfAbsent(extent, key -> BlockMap.create()).put(position, overlay);
            return;
        }

        Level adaptedWorld = FabricAdapter.adapt(editSession.getWorld());
        if (!(adaptedWorld instanceof ServerLevel world)) {
            return;
        }

        BlockPos pos = FabricAdapter.toBlockPos(position);
        editSession.getChangeSet().add(new OverlayChange(world, pos, null, overlay));
        setOverlay(world, pos, overlay);
    }

    private static void setOverlay(ServerLevel world, BlockPos pos, BlockState overlay) {
        if (overlay == null) {
            UnderlayManager.removeOverlayAndBroadcast(world, pos);
            return;
        }

        UnderlayManager.addOverlayFromStructure(world, pos, overlay);
    }

    private record OverlayChange(ServerLevel world, BlockPos pos, BlockState oldOverlay, BlockState newOverlay) implements Change {
        @Override
        public void undo(UndoContext context) {
            setOverlay(world, pos, oldOverlay);
        }

        @Override
        public void redo(UndoContext context) {
            setOverlay(world, pos, newOverlay);
        }
    }

    private UnderlayWorldEdit() {
    }
}
