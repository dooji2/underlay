package com.dooji.underlay.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dooji.underlay.main.UnderlayManager;
import com.dooji.underlay.util.HasUnderlayOverlays;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

@Pseudo
@Mixin(value = Contraption.class, remap = false)
public abstract class CreateContraptionMixin implements HasUnderlayOverlays {
    @Shadow
    public BlockPos anchor;

    @Shadow
    protected Map<BlockPos, StructureBlockInfo> blocks;

    @Unique
    private final Map<BlockPos, BlockState> movingOverlays = new HashMap<>();

    @Unique
    private int movingOverlaysVersion = 0;

    @Unique
    private void markOverlaysChanged() {
        movingOverlaysVersion++;
    }

    @Override
    public Map<BlockPos, BlockState> getMovingOverlays() {
        return movingOverlays;
    }

    @Override
    public int getMovingOverlaysVersion() {
        return movingOverlaysVersion;
    }

    @Inject(method = "removeBlocksFromWorld", at = @At("HEAD"))
    private void captureAndRemoveOverlays(Level world, BlockPos offset, CallbackInfo ci) {
        movingOverlays.clear();
        markOverlaysChanged();

        if (world.isClientSide()) {
            return;
        }

        for (StructureBlockInfo block : blocks.values()) {
            BlockPos localPos = block.pos();
            BlockPos worldPos = localPos.offset(anchor).offset(offset);

            if (!UnderlayManager.hasOverlay(world, worldPos)) {
                continue;
            }

            BlockState overlay = UnderlayManager.getOverlay(world, worldPos);
            movingOverlays.put(localPos.immutable(), overlay);
            UnderlayManager.removeOverlayAndBroadcast(world, worldPos);
        }
    }

    @Inject(method = "writeNBT", at = @At("RETURN"))
    private void writeOverlays(HolderLookup.Provider registries, boolean spawnPacket, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag nbt = cir.getReturnValue();
        if (nbt == null || movingOverlays.isEmpty()) {
            return;
        }

        ListTag overlays = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : movingOverlays.entrySet()) {
            CompoundTag tag = new CompoundTag();
            BlockPos pos = entry.getKey();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            tag.put("state", NbtUtils.writeBlockState(entry.getValue()));
            overlays.add(tag);
        }

        nbt.put("underlay_overlays", overlays);
    }

    @Inject(method = "readNBT", at = @At("TAIL"))
    private void readOverlays(Level world, CompoundTag nbt, boolean spawnData, CallbackInfo ci) {
        movingOverlays.clear();

        if (!nbt.contains("underlay_overlays", 9)) {
            markOverlaysChanged();
            return;
        }

        ListTag overlays = nbt.getList("underlay_overlays", 10);

        for (int i = 0; i < overlays.size(); i++) {
            CompoundTag tag = overlays.getCompound(i);
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));

            if (!tag.contains("state", 10)) {
                continue;
            }

            CompoundTag stateTag = tag.getCompound("state");
            if (stateTag.isEmpty()) {
                continue;
            }

            BlockState state = NbtUtils.readBlockState(world.holderLookup(Registries.BLOCK), stateTag);
            movingOverlays.put(pos.immutable(), state);
        }

        markOverlaysChanged();
    }

    @Inject(method = "addBlocksToWorld", at = @At("TAIL"))
    private void restoreOverlays(Level world, StructureTransform transform, CallbackInfo ci) {
        if (world.isClientSide() || movingOverlays.isEmpty()) {
            return;
        }

        for (Map.Entry<BlockPos, BlockState> entry : movingOverlays.entrySet()) {
            BlockPos worldPos = transform.apply(entry.getKey());
            BlockState worldState = transform.apply(entry.getValue());
            UnderlayManager.addOverlayFromContraption(world, worldPos, worldState);
        }

        movingOverlays.clear();
        markOverlaysChanged();
    }
}
