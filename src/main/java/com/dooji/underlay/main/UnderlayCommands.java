package com.dooji.underlay.main;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class UnderlayCommands {
    @SubscribeEvent
    public void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("underlay")
                .requires(source -> source.hasPermission(UnderlayConfig.getCommandsOpLevel()))
                .then(Commands.literal("config")
                        .then(Commands.literal("edit")
                                .then(Commands.literal(UnderlayConfig.PLACE_ON_REPLACEABLE_BLOCKS_KEY)
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(context -> setPlaceOnReplaceableBlocks(
                                                        context.getSource(),
                                                        BoolArgumentType.getBool(context, "value"))))))));
    }

    private static int setPlaceOnReplaceableBlocks(CommandSourceStack source, boolean value) {
        UnderlayConfig.setPlaceOnReplaceableBlocks(value);
        source.sendSuccess(() -> Component.translatable("commands.underlay.config.edit.success",
                UnderlayConfig.PLACE_ON_REPLACEABLE_BLOCKS_KEY, value), true);
        return Command.SINGLE_SUCCESS;
    }
}
