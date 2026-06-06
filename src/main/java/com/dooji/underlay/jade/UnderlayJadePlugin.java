package com.dooji.underlay.jade;

import java.util.Objects;
import java.util.function.Consumer;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class UnderlayJadePlugin implements IWailaPlugin {
    private static Consumer<IWailaClientRegistration> clientRegistration = registration -> {
    };

    public static void setClientRegistration(Consumer<IWailaClientRegistration> clientRegistration) {
        UnderlayJadePlugin.clientRegistration = Objects.requireNonNull(clientRegistration);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        clientRegistration.accept(registration);
    }
}
