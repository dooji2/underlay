package com.dooji.underlay;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class UnderlayCommands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("underlay")
						.requires(source -> source.permissions().hasPermission(
								new Permission.HasCommandLevel(PermissionLevel.byId(UnderlayConfig.getCommandsOpLevel()))))
						.then(Commands.literal("config")
								.then(Commands.literal("edit")
										.then(Commands.literal(UnderlayConfig.PLACE_ON_REPLACEABLE_BLOCKS_KEY)
												.then(Commands.argument("value", BoolArgumentType.bool())
														.executes(context -> setPlaceOnReplaceableBlocks(
																context.getSource(),
																BoolArgumentType.getBool(context, "value")))))))));
	}

	private static int setPlaceOnReplaceableBlocks(CommandSourceStack source, boolean value) {
		UnderlayConfig.setPlaceOnReplaceableBlocks(value);
		source.sendSuccess(() -> Component.translatable("commands.underlay.config.edit.success",
				UnderlayConfig.PLACE_ON_REPLACEABLE_BLOCKS_KEY, value), true);
		return Command.SINGLE_SUCCESS;
	}
}
