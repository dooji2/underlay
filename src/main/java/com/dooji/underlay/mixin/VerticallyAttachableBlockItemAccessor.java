package com.dooji.underlay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.Block;
import net.minecraft.item.VerticallyAttachableBlockItem;

@Mixin(VerticallyAttachableBlockItem.class)
public interface VerticallyAttachableBlockItemAccessor {
    @Accessor("wallBlock")
    Block getWallBlock();
}
