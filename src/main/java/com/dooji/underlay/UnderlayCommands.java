package com.dooji.underlay;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class UnderlayCommands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("underlay")
						.requires(source -> source.hasPermissionLevel(UnderlayConfig.getCommandsOpLevel()))
						.then(CommandManager.literal("config")
								.then(CommandManager.literal("edit")
										.then(CommandManager.literal(UnderlayConfig.PLACE_ON_REPLACEABLE_BLOCKS_KEY)
												.then(CommandManager.argument("value", BoolArgumentType.bool())
														.executes(context -> setPlaceOnReplaceableBlocks(
																context.getSource(),
																BoolArgumentType.getBool(context, "value")))))))));
	}

	private static int setPlaceOnReplaceableBlocks(ServerCommandSource source, boolean value) {
		UnderlayConfig.setPlaceOnReplaceableBlocks(value);
		source.sendFeedback(() -> Text.translatable("commands.underlay.config.edit.success",
				UnderlayConfig.PLACE_ON_REPLACEABLE_BLOCKS_KEY, value), true);
		return Command.SINGLE_SUCCESS;
	}
}
