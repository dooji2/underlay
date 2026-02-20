package com.dooji.underlay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;

@Mixin(StandingAndWallBlockItem.class)
public interface StandingAndWallBlockItemAccessor {
    @Accessor("wallBlock")
    Block getWallBlock();
}
