package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.GameModeArgument;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;

/** Modern's /defaultgamemode <mode> (spec §51.1); with forced game modes on, the players follow. */
public final class DefaultGameModeCommand extends ModernCommand {

	public DefaultGameModeCommand() {
		super("defaultgamemode", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("defaultgamemode")
			.requires(requirement())
			.then(RequiredArgumentBuilder.<ModernCommandSource, WorldSettings.GameType>argument("gamemode", GameModeArgument.gameMode())
				.executes(c -> {
					WorldSettings.GameType type = GameModeArgument.getGameMode(c, "gamemode");
					MinecraftServer server = c.getSource().server();
					server.setGameType(type);
					int changed = 0;
					if (server.getForceGamemode()) {
						for (Object player : server.getConfigurationManager().playerEntityList) {
							EntityPlayerMP playerMP = (EntityPlayerMP) player;
							if (playerMP.theItemInWorldManager.getGameType() != type) {
								playerMP.setGameType(type);
								changed++;
							}
						}
					}
					c.getSource().sendSuccess(legacyView(), true, "commands.etfuturum.defaultgamemode.success", GameModeCommand.modeName(type));
					return changed;
				})));
	}
}
