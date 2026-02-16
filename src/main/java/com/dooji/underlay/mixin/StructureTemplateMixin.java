package com.dooji.underlay.mixin;

import com.dooji.underlay.main.UnderlayManager;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
    @Unique
    private final Map<BlockPos, BlockState> relativeOverlays = new HashMap<>();

    @Shadow
    public static BlockPos calculateRelativePosition(StructurePlaceSettings settings, BlockPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "fillFromWorld", at = @At("TAIL"))
    private void captureOverlays(Level world, BlockPos start, Vec3i dimensions, boolean includeEntities, @Nullable Block ignoredBlock, CallbackInfo ci) {
        relativeOverlays.clear();

        int width = dimensions.getX();
        int height = dimensions.getY();
        int depth = dimensions.getZ();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos relativePos = new BlockPos(x, y, z);
                    BlockPos worldPos = start.offset(relativePos);

                    if (!UnderlayManager.hasOverlay(world, worldPos)) {
                        continue;
                    }

                    BlockState overlay = UnderlayManager.getOverlay(world, worldPos);
                    relativeOverlays.put(relativePos, overlay);
                }
            }
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void writeOverlays(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag resultNbt = cir.getReturnValue();
        if (resultNbt == null) {
            return;
        }

        ListTag overlays = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : relativeOverlays.entrySet()) {
            CompoundTag tag = new CompoundTag();
            BlockPos pos = entry.getKey();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            tag.put("state", NbtUtils.writeBlockState(entry.getValue()));
            overlays.add(tag);
        }

        resultNbt.put("underlay_overlays", overlays);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void readOverlays(HolderGetter<Block> blockLookup, CompoundTag nbt, CallbackInfo ci) {
        relativeOverlays.clear();

        if (!nbt.contains("underlay_overlays", 9)) {
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

            BlockState state = NbtUtils.readBlockState(blockLookup, stateTag);
            relativeOverlays.put(pos, state);
        }
    }

    @Inject(method = "placeInWorld", at = @At("RETURN"))
    private void placeOverlays(ServerLevelAccessor world, BlockPos pos, BlockPos pivot, StructurePlaceSettings settings, RandomSource random, int flags, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || relativeOverlays.isEmpty()) {
            return;
        }

        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }

        for (Map.Entry<BlockPos, BlockState> entry : relativeOverlays.entrySet()) {
            BlockPos transformed = calculateRelativePosition(settings, entry.getKey());
            BlockPos worldPos = pos.offset(transformed);
            UnderlayManager.addOverlayFromStructure(serverLevel, worldPos, entry.getValue());
        }
    }
}
