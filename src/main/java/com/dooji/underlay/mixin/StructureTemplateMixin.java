package com.dooji.underlay.mixin;

import com.dooji.underlay.UnderlayManager;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jetbrains.annotations.Nullable;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {

    @Unique
    private final Map<BlockPos, BlockState> relativeOverlays = new HashMap<>();

    @Shadow
    public static BlockPos transform(StructurePlacementData settings, BlockPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "saveFromWorld", at = @At("TAIL"))
    private void captureOverlays(World world, BlockPos start, Vec3i dimensions, boolean includeEntities, @Nullable Block ignoredBlock, CallbackInfo ci) {
        relativeOverlays.clear();

        int width = dimensions.getX();
        int height = dimensions.getY();
        int depth = dimensions.getZ();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos relativePos = new BlockPos(x, y, z);
                    BlockPos worldPos = start.add(relativePos);

                    if (!UnderlayManager.hasOverlay(world, worldPos)) {
                        continue;
                    }

                    BlockState overlay = UnderlayManager.getOverlay(world, worldPos);
                    relativeOverlays.put(relativePos, overlay);
                }
            }
        }
    }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void writeOverlays(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        NbtCompound resultNbt = cir.getReturnValue();
        if (resultNbt == null) {
            return;
        }

        NbtList overlays = new NbtList();
        for (Map.Entry<BlockPos, BlockState> entry : relativeOverlays.entrySet()) {
            NbtCompound tag = new NbtCompound();
            BlockPos pos = entry.getKey();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            tag.put("state", NbtHelper.fromBlockState(entry.getValue()));
            overlays.add(tag);
        }

        resultNbt.put("underlay_overlays", overlays);
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readOverlays(RegistryEntryLookup<Block> blockLookup, NbtCompound nbt, CallbackInfo ci) {
        relativeOverlays.clear();
        if (!nbt.contains("underlay_overlays", NbtElement.LIST_TYPE)) {
            return;
        }

        NbtList overlays = nbt.getList("underlay_overlays", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < overlays.size(); i++) {
            NbtCompound tag = overlays.getCompound(i);
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));

            if (!tag.contains("state", NbtElement.COMPOUND_TYPE)) {
                continue;
            }

            NbtCompound stateTag = tag.getCompound("state");
            if (stateTag.isEmpty()) {
                continue;
            }

            BlockState state = NbtHelper.toBlockState(blockLookup, stateTag);
            relativeOverlays.put(pos, state);
        }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void placeOverlays(ServerWorldAccess world, BlockPos pos, BlockPos pivot, StructurePlacementData settings, Random random, int flags, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || relativeOverlays.isEmpty()) {
            return;
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        for (Map.Entry<BlockPos, BlockState> entry : relativeOverlays.entrySet()) {
            BlockPos transformed = transform(settings, entry.getKey());
            BlockPos worldPos = pos.add(transformed);
            UnderlayManager.addOverlayFromStructure(serverWorld, worldPos, entry.getValue());
        }
    }
}
