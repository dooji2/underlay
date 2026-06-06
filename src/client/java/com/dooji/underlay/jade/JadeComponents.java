package com.dooji.underlay.jade;

import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;

public final class JadeComponents {
    private JadeComponents() {
    }

    public static void init() {
        UnderlayJadePlugin.setClientRegistration(JadeComponents::register);
    }

    private static void register(IWailaClientRegistration registration) {
        registration.registerBlockComponent(UnderlayComponentProvider.INSTANCE, Block.class);
    }
}
