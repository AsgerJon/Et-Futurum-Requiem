package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Modern's /seed, /list and /help (spec §51.1). */
public final class InfoCommands {

	private InfoCommands() {
	}

	public static final class Seed extends ModernCommand {
		public Seed() {
			super("seed", 2);
		}

		@Override
		public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
			dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("seed")
				.requires(requirement())
				.executes(c -> {
					long seed = c.getSource().world().getSeed();
					IChatComponent shown = new ChatComponentText("[" + seed + "]");
					shown.getChatStyle().setColor(EnumChatFormatting.GREEN);
					c.getSource().sendSuccess(legacyView(), false, "commands.seed.success", shown);
					return (int) seed;
				}));
		}
	}

	public static final class ListPlayers extends ModernCommand {
		public ListPlayers() {
			super("list", 0);
		}

		@Override
		public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
			dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("list")
				.requires(requirement())
				.executes(c -> list(c.getSource(), false))
				.then(LiteralArgumentBuilder.<ModernCommandSource>literal("uuids").executes(c -> list(c.getSource(), true))));
		}

		private int list(ModernCommandSource source, boolean uuids) {
			List<?> players = source.server().getConfigurationManager().playerEntityList;
			List<String> names = new ArrayList<>();
			for (Object player : players) {
				EntityPlayerMP playerMP = (EntityPlayerMP) player;
				names.add(uuids ? playerMP.getCommandSenderName() + " (" + playerMP.getUniqueID() + ")" : playerMP.getCommandSenderName());
			}
			source.sendSuccess(legacyView(), false, "commands.list.players", players.size(), source.server().getMaxPlayers(), String.join(", ", names));
			return players.size();
		}
	}

	/** Every command the source may use with its usage, or one command's. */
	public static final class Help extends ModernCommand {
		private static final SimpleCommandExceptionType FAILED = Responses.failure("commands.help.failed");

		public Help() {
			super("help", 0);
		}

		@Override
		public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
			dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("help")
				.requires(requirement())
				.executes(c -> {
					Map<CommandNode<ModernCommandSource>, String> usage = dispatcher.getSmartUsage(dispatcher.getRoot(), c.getSource());
					for (String line : usage.values()) {
						c.getSource().sender().addChatMessage(new ChatComponentText("/" + line));
					}
					return usage.size();
				})
				.then(RequiredArgumentBuilder.<ModernCommandSource, String>argument("command", StringArgumentType.greedyString())
					.executes(c -> {
						ParseResults<ModernCommandSource> parse = dispatcher.parse(StringArgumentType.getString(c, "command"), c.getSource());
						if (parse.getContext().getNodes().isEmpty()) {
							throw FAILED.create();
						}
						List<ParsedCommandNode<ModernCommandSource>> nodes = parse.getContext().getNodes();
						CommandNode<ModernCommandSource> last = nodes.get(nodes.size() - 1).getNode();
						Map<CommandNode<ModernCommandSource>, String> usage = dispatcher.getSmartUsage(last, c.getSource());
						String typed = parse.getReader().getString().substring(0, nodes.get(nodes.size() - 1).getRange().getEnd());
						for (String line : usage.values()) {
							c.getSource().sender().addChatMessage(new ChatComponentText("/" + typed + " " + line));
						}
						if (usage.isEmpty()) {
							c.getSource().sender().addChatMessage(new ChatComponentText("/" + typed));
						}
						return usage.size();
					})));
		}
	}
}
