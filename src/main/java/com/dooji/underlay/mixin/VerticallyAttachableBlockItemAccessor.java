package com.dooji.underlay.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.StandingAndWallBlockItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StandingAndWallBlockItem.class)
public interface VerticallyAttachableBlockItemAccessor {
	@Accessor("wallBlock")
	Block getWallBlock();
}
