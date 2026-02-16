package com.dooji.underlay.mixin;

import com.dooji.underlay.main.UnderlayManager;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Template.class)
public abstract class StructureTemplateMixin {
    @Unique
    private final Map<BlockPos, IBlockState> relativeOverlays = new HashMap<BlockPos, IBlockState>();

    @Shadow
    public static BlockPos transformedBlockPos(PlacementSettings placementIn, BlockPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "takeBlocksFromWorld", at = @At("TAIL"))
    private void captureOverlays(World worldIn, BlockPos startPos, BlockPos endPos, boolean takeEntities, @Nullable Block toIgnore, CallbackInfo ci) {
        relativeOverlays.clear();

        int width = endPos.getX();
        int height = endPos.getY();
        int depth = endPos.getZ();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos relativePos = new BlockPos(x, y, z);
                    BlockPos worldPos = startPos.add(relativePos);

                    if (!UnderlayManager.hasOverlay(worldIn, worldPos)) {
                        continue;
                    }

                    IBlockState overlay = UnderlayManager.getOverlay(worldIn, worldPos);
                    relativeOverlays.put(relativePos, overlay);
                }
            }
        }
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void writeOverlays(NBTTagCompound nbt, CallbackInfoReturnable<NBTTagCompound> cir) {
        NBTTagCompound result = cir.getReturnValue();
        if (result == null) {
            return;
        }

        NBTTagList overlays = new NBTTagList();
        for (Map.Entry<BlockPos, IBlockState> entry : relativeOverlays.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            BlockPos pos = entry.getKey();
            tag.setInteger("x", pos.getX());
            tag.setInteger("y", pos.getY());
            tag.setInteger("z", pos.getZ());
            tag.setTag("state", NBTUtil.writeBlockState(new NBTTagCompound(), entry.getValue()));
            overlays.appendTag(tag);
        }

        result.setTag("underlay_overlays", overlays);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void readOverlays(NBTTagCompound compound, CallbackInfo ci) {
        relativeOverlays.clear();

        if (!compound.hasKey("underlay_overlays", 9)) {
            return;
        }

        NBTTagList overlays = compound.getTagList("underlay_overlays", 10);

        for (int i = 0; i < overlays.tagCount(); i++) {
            NBTTagCompound tag = overlays.getCompoundTagAt(i);
            BlockPos pos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));

            if (!tag.hasKey("state", 10)) {
                continue;
            }

            NBTTagCompound stateTag = tag.getCompoundTag("state");
            IBlockState state = NBTUtil.readBlockState(stateTag);
            relativeOverlays.put(pos, state);
        }
    }

    @Inject(method = "addBlocksToWorld(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/ITemplateProcessor;Lnet/minecraft/world/gen/structure/template/PlacementSettings;I)V", at = @At("RETURN"))
    private void placeOverlays(World worldIn, BlockPos pos, @Nullable ITemplateProcessor templateProcessor, PlacementSettings placementIn, int flags, CallbackInfo ci) {
        if (worldIn == null || worldIn.isRemote || relativeOverlays.isEmpty()) {
            return;
        }

        for (Map.Entry<BlockPos, IBlockState> entry : relativeOverlays.entrySet()) {
            BlockPos transformed = transformedBlockPos(placementIn, entry.getKey());
            BlockPos worldPos = transformed.add(pos);
            UnderlayManager.addOverlayFromStructure(worldIn, worldPos, entry.getValue());
        }
    }
}
