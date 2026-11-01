package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.EntityArgument;
import ganymedes01.etfuturum.command.modern.arguments.EntitySelector;
import ganymedes01.etfuturum.command.modern.arguments.GameModeArgument;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldSettings;

import java.util.Collections;
import java.util.List;

/** Modern's /gamemode <mode> [player] (spec §51.1); a player already in the mode is skipped. */
public final class GameModeCommand extends ModernCommand {

	public GameModeCommand() {
		super("gamemode", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("gamemode")
			.requires(requirement())
			.then(RequiredArgumentBuilder.<ModernCommandSource, WorldSettings.GameType>argument("gamemode", GameModeArgument.gameMode())
				.executes(c -> setMode(c.getSource(), Collections.singletonList(Commands.requirePlayer(c.getSource())), GameModeArgument.getGameMode(c, "gamemode")))
				.then(RequiredArgumentBuilder.<ModernCommandSource, EntitySelector>argument("target", EntityArgument.players())
					.executes(c -> setMode(c.getSource(), EntityArgument.getPlayers(c, "target"), GameModeArgument.getGameMode(c, "gamemode"))))));
	}

	/** `gameMode.survival` and so on; EFR's spectator under `gameMode.spectator`. */
	static ChatComponentTranslation modeName(WorldSettings.GameType type) {
		return new ChatComponentTranslation("gameMode." + type.getName());
	}

	private int setMode(ModernCommandSource source, List<EntityPlayerMP> players, WorldSettings.GameType type) throws CommandSyntaxException {
		int count = 0;
		for (EntityPlayerMP player : players) {
			if (player.theItemInWorldManager.getGameType() == type) {
				continue;
			}
			player.setGameType(type);
			count++;
			if (source.entity() == player) {
				source.sendSuccess(legacyView(), true, "commands.gamemode.success.self", modeName(type));
			} else {
				player.addChatMessage(new ChatComponentTranslation("commands.etfuturum.gamemode.changed", modeName(type)));
				source.sendSuccess(legacyView(), true, "commands.gamemode.success.other", player.func_145748_c_(), modeName(type));
			}
		}
		return count;
	}
}
