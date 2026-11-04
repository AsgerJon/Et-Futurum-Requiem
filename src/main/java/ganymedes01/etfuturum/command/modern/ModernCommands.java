package ganymedes01.etfuturum.command.modern;

import io.github.asgerjon.mcfly.McFly;
import io.github.asgerjon.mcfly.network.McFlyNetwork;

import io.github.asgerjon.mcfly.config.McFlyConfig;
import com.github.bsideup.jabel.Desugar;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.command.modern.commands.EffectCommand;
import ganymedes01.etfuturum.command.modern.commands.EnchantCommand;
import ganymedes01.etfuturum.command.modern.commands.GiveCommand;
import ganymedes01.etfuturum.command.modern.commands.SummonCommand;
import ganymedes01.etfuturum.command.modern.commands.TeleportCommand;
import ganymedes01.etfuturum.command.modern.commands.KillCommand;
import ganymedes01.etfuturum.command.modern.commands.TagCommand;
import ganymedes01.etfuturum.command.modern.commands.GameModeCommand;
import ganymedes01.etfuturum.command.modern.commands.DefaultGameModeCommand;
import ganymedes01.etfuturum.command.modern.commands.DifficultyCommand;
import ganymedes01.etfuturum.command.modern.commands.TimeCommand;
import ganymedes01.etfuturum.command.modern.commands.WeatherCommand;
import ganymedes01.etfuturum.command.modern.commands.ExperienceCommand;
import ganymedes01.etfuturum.command.modern.commands.ClearCommand;
import ganymedes01.etfuturum.command.modern.commands.GameRuleCommand;
import ganymedes01.etfuturum.command.modern.commands.SpawnPointCommand;
import ganymedes01.etfuturum.command.modern.commands.SetWorldSpawnCommand;
import ganymedes01.etfuturum.command.modern.commands.ChatCommands;
import ganymedes01.etfuturum.command.modern.commands.InfoCommands;
import ganymedes01.etfuturum.command.modern.commands.CloneCommand;
import ganymedes01.etfuturum.command.modern.commands.FillCommand;
import ganymedes01.etfuturum.command.modern.commands.SetBlockCommand;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import io.github.asgerjon.mcfly.network.CommandSuggestionsMessage;
import io.github.asgerjon.mcfly.network.CommandTreeMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server's command dispatcher (spec §40.5, §41.1): the tree each player may use, sent to their client, the
 * suggestions the client asks for, and the commands EFR has in modern form, which it runs itself with the 1.7.10
 * form as the fallback. Every other 1.7.10 command is in the tree as a literal with a greedy argument whose
 * suggestions are the command's own tab completion, and runs through 1.7.10's handler as before.
 */
public final class ModernCommands {

	private static final Logger LOGGER = LogManager.getLogger(McFly.DOMAIN);
	private static final int MAX_SUGGESTIONS = 1000;
	/** 1.7.10's handler runs these; the tree only needs to know that input ending here runs. */
	private static final Command<ModernCommandSource> RUN_BY_LEGACY_HANDLER = context -> 0;

	private static CommandDispatcher<ModernCommandSource> dispatcher = new CommandDispatcher<>();
	/** The literal of each wrapped 1.7.10 command, with the command. */
	private static final Map<CommandNode<ModernCommandSource>, ICommand> WRAPPED = new IdentityHashMap<>();
	/** The latest suggestion request of each player, answered at the end of the tick. */
	private static final Map<EntityPlayerMP, Request> REQUESTS = new ConcurrentHashMap<>();
	/** The commands in modern form, by name. */
	private static final Map<String, ModernCommand> MODERN = new HashMap<>();
	/** One entry per executeCommand under way (commands can run commands). */
	private static final ThreadLocal<ArrayDeque<Run>> RUNS = ThreadLocal.withInitial(ArrayDeque::new);

	private ModernCommands() {
	}

	/** A new server has its own commands. */
	public static synchronized void reset() {
		dispatcher = new CommandDispatcher<>();
		WRAPPED.clear();
		REQUESTS.clear();
		MODERN.clear();
		if (McFlyConfig.modernChat) {
			register(new EffectCommand());
			register(new EnchantCommand());
			register(new GiveCommand());
			register(new SummonCommand());
			register(new SetBlockCommand());
			register(new FillCommand());
			register(new CloneCommand());
			register(new TeleportCommand());
			register(new KillCommand());
			register(new TagCommand());
			register(new GameModeCommand());
			register(new DefaultGameModeCommand());
			register(new DifficultyCommand());
			register(new TimeCommand());
			register(new WeatherCommand());
			register(new ExperienceCommand());
			register(new ClearCommand());
			register(new GameRuleCommand());
			register(new SpawnPointCommand());
			register(new SetWorldSpawnCommand());
			register(new ChatCommands.Say());
			register(new ChatCommands.Me());
			register(new ChatCommands.Msg());
			register(new ChatCommands.TellRaw());
			register(new InfoCommands.Seed());
			register(new InfoCommands.ListPlayers());
			register(new InfoCommands.Help());
		}
	}

	private static void register(ModernCommand command) {
		command.register(dispatcher);
		MODERN.put(command.name(), command);
		for (String alias : command.aliases()) {
			MODERN.put(alias, command);
		}
	}

	/**
	 * The start of 1.7.10's CommandHandler.executeCommand (chat, command blocks, the console). A command in modern form
	 * that parses runs here; 1.7.10 then finds no command of that name, its "unknown command" reply is dropped and
	 * endRun returns the modern result. Otherwise 1.7.10 runs the text, with the modern parse error, if any, to show in
	 * place of its own failure. The method is never cancelled, so other mods' hooks at its start and end both run.
	 */
	public static void beginRun(ICommandSender sender, String text, Map<String, ICommand> legacyCommands) {
		Run run = new Run();
		try {
			String command = text.trim();
			if (command.startsWith("/")) {
				command = command.substring(1);
			}
			String name = command.split(" ", 2)[0];
			ModernCommand modern = McFlyConfig.modernChat ? MODERN.get(name) : null;
			if (modern != null) {
				ModernCommandSource source = new ModernCommandSource(sender);
				ParseResults<ModernCommandSource> parse = dispatcher.parse(command, source);
				CommandSyntaxException error = parseError(parse, command);
				if (error == null) {
					run.ranModern = true;
					run.result = run(modern, parse, command, source);
				} else if (!legacyCommands.containsKey(name) || !modern.legacyCouldRun(legacyArgs(command))) {
					run.ranModern = true;
					sendParseError(source, error, command);
				} else {
					run.error = error;
					run.command = command;
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("Could not run '/{}' in its modern form; 1.7.10 runs it", text, e);
		}
		RUNS.get().push(run);
	}

	/** The words after the command's name, as 1.7.10's handler splits them. */
	private static String[] legacyArgs(String command) {
		String[] words = command.split(" ");
		return Arrays.copyOfRange(words, 1, words.length);
	}

	/** 1.7.10 finds no command when the modern one ran. */
	public static boolean ranModern() {
		Run run = RUNS.get().peek();
		return run != null && run.ranModern;
	}

	/**
	 * A message 1.7.10's handler sends for the text: dropped when the modern command ran; when the modern parse failed
	 * and 1.7.10's run fails too (its usage, a wrong argument, no permission), modern's error instead, once. A crash
	 * inside the command keeps 1.7.10's message. True when 1.7.10's message is not to be sent.
	 */
	public static boolean replaceLegacyMessage(ICommandSender sender, IChatComponent message) {
		Run run = RUNS.get().peek();
		if (run == null) {
			return false;
		}
		if (run.ranModern) {
			return true;
		}
		if (run.error == null || message instanceof ChatComponentTranslation translation && "commands.generic.exception".equals(translation.getKey())) {
			return false;
		}
		if (!run.errorShown) {
			run.errorShown = true;
			sendParseError(new ModernCommandSource(sender), run.error, run.command);
		}
		return true;
	}

	/** The end of executeCommand: the modern result when the modern command ran, else 1.7.10's. */
	public static int endRun(int legacyResult) {
		ArrayDeque<Run> runs = RUNS.get();
		Run run = runs.isEmpty() ? null : runs.pop();
		return run != null && run.ranModern ? run.result : legacyResult;
	}

	/** Why the text doesn't run: modern's parse error, or "Unknown or incomplete command" when it ends where nothing runs. */
	private static CommandSyntaxException parseError(ParseResults<ModernCommandSource> parse, String command) {
		if (parse.getReader().canRead()) {
			if (parse.getExceptions().size() == 1) {
				return parse.getExceptions().values().iterator().next();
			}
			return parse.getContext().getRange().isEmpty()
				? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader())
				: CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parse.getReader());
		}
		CommandContext<ModernCommandSource> context = parse.getContext().build(command);
		while (context.getChild() != null) {
			context = context.getChild();
		}
		return context.getCommand() == null
			? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader()) : null;
	}

	/** Forge's CommandEvent first, as for any command; then the command, its errors in red, a crash as modern words it. */
	private static int run(ModernCommand modern, ParseResults<ModernCommandSource> parse, String command, ModernCommandSource source) {
		String[] words = command.split(" ");
		CommandEvent event = new CommandEvent(modern.legacyView(), source.sender(), Arrays.copyOfRange(words, 1, words.length));
		if (MinecraftForge.EVENT_BUS.post(event)) {
			if (event.exception != null) {
				source.sendFailure(new ChatComponentTranslation("commands.generic.exception"));
				LOGGER.error("Couldn't process command: '{}'", command, event.exception);
			}
			return 0;
		}
		try {
			return dispatcher.execute(parse);
		} catch (CommandSyntaxException e) {
			source.sendFailure(ComponentMessage.toComponent(e.getRawMessage()));
			return 0;
		} catch (RuntimeException e) {
			IChatComponent failed = new ChatComponentTranslation("command.failed");
			failed.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				new ChatComponentText(e.getMessage() == null ? e.getClass().getName() : e.getMessage())));
			source.sendFailure(failed);
			LOGGER.error("'/{}' threw an exception", command, e);
			return 0;
		}
	}

	/** The error in red, then the input up to it in grey, the rest red and underlined, and <--[HERE]; a click puts the command in the chat. */
	private static void sendParseError(ModernCommandSource source, CommandSyntaxException error, String command) {
		source.sendFailure(ComponentMessage.toComponent(error.getRawMessage()));
		String input = error.getInput();
		if (input == null || error.getCursor() < 0) {
			return;
		}
		int cursor = Math.min(input.length(), error.getCursor());
		IChatComponent context = new ChatComponentText("");
		context.getChatStyle().setColor(EnumChatFormatting.GRAY).setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + command));
		if (cursor > 10) {
			context.appendText("...");
		}
		context.appendText(input.substring(Math.max(0, cursor - 10), cursor));
		if (cursor < input.length()) {
			IChatComponent rest = new ChatComponentText(input.substring(cursor));
			rest.getChatStyle().setColor(EnumChatFormatting.RED).setUnderlined(true);
			context.appendSibling(rest);
		}
		IChatComponent here = new ChatComponentTranslation("command.context.here");
		here.getChatStyle().setColor(EnumChatFormatting.RED).setItalic(true);
		context.appendSibling(here);
		source.sendFailure(context);
	}

	/** Sends the player the commands they may use: on login, and whenever their permission changes. */
	public static synchronized void sendCommands(EntityPlayerMP player) {
		if (!McFlyConfig.modernChat || player.playerNetServerHandler == null) {
			return;
		}
		wrapLegacyCommands(player.mcServer);
		ModernCommandSource source = new ModernCommandSource(player);
		RootCommandNode<ModernCommandSource> root = new RootCommandNode<>();
		Map<CommandNode<ModernCommandSource>, CommandNode<ModernCommandSource>> copies = new IdentityHashMap<>();
		copies.put(dispatcher.getRoot(), root);
		copyUsable(dispatcher.getRoot(), root, source, copies);

		Map<CommandNode<ModernCommandSource>, String> usage = new IdentityHashMap<>();
		for (Map.Entry<CommandNode<ModernCommandSource>, ICommand> wrapped : WRAPPED.entrySet()) {
			CommandNode<ModernCommandSource> copy = copies.get(wrapped.getKey());
			if (copy != null) {
				String text = LegacyCommands.usage(wrapped.getValue(), player);
				if (text != null) {
					usage.put(copy, text);
				}
			}
		}
		ByteBuf buf = Unpooled.buffer();
		CommandTree.write(root, usage, buf);
		byte[] bytes = new byte[buf.readableBytes()];
		buf.readBytes(bytes);
		McFlyNetwork.CHANNEL.sendTo(new CommandTreeMessage(bytes), player);
	}

	/** Everyone's permissions may have changed (cheats turned on for the LAN world). */
	@SuppressWarnings("unchecked")
	public static void sendCommandsToAll(MinecraftServer server) {
		for (EntityPlayerMP player : (List<EntityPlayerMP>) server.getConfigurationManager().playerEntityList) {
			sendCommands(player);
		}
	}

	/** The player's latest request replaces any still waiting. */
	public static void requestSuggestions(EntityPlayerMP player, int id, String text) {
		REQUESTS.put(player, new Request(id, text));
	}

	/** End of each server tick: at most one request per player, the latest. */
	public static void answerRequests() {
		for (EntityPlayerMP player : REQUESTS.keySet()) {
			Request request = REQUESTS.remove(player);
			if (request != null && player.playerNetServerHandler != null) {
				answer(player, request);
			}
		}
	}

	private static synchronized void answer(EntityPlayerMP player, Request request) {
		try {
			StringReader reader = new StringReader(request.text);
			if (reader.canRead() && reader.peek() == '/') {
				reader.skip();
			}
			ParseResults<ModernCommandSource> parse = dispatcher.parse(reader, new ModernCommandSource(player));
			dispatcher.getCompletionSuggestions(parse).thenAccept(suggestions -> McFlyNetwork.CHANNEL.sendTo(
				new CommandSuggestionsMessage(request.id, limit(suggestions)), player));
		} catch (Exception e) {
			LOGGER.error("Failed to resolve command suggestions for {}", player.getCommandSenderName(), e);
		}
	}

	private static Suggestions limit(Suggestions suggestions) {
		return suggestions.getList().size() <= MAX_SUGGESTIONS ? suggestions
			: new Suggestions(suggestions.getRange(), suggestions.getList().subList(0, MAX_SUGGESTIONS));
	}

	/** Each 1.7.10 command name and alias not in the tree yet joins it (LegacyCommands). */
	private static void wrapLegacyCommands(MinecraftServer server) {
		if (!(server.getCommandManager() instanceof CommandHandler handler)) {
			return;
		}
		for (Map.Entry<String, ICommand> entry : handler.getCommands().entrySet()) {
			String name = entry.getKey();
			ICommand command = entry.getValue();
			if (!LegacyCommands.isWrappable(name) || dispatcher.getRoot().getChild(name) != null) {
				continue;
			}
			LiteralCommandNode<ModernCommandSource> literal = dispatcher.register(LegacyCommands.wrap(name, command, ModernCommandSource::sender,
				source -> LegacyCommands.canUse(command, source.sender()), RUN_BY_LEGACY_HANDLER));
			WRAPPED.put(literal, command);
		}
	}

	/** The nodes the source may use, copied with their redirects pointing at the copies, as modern does. */
	private static void copyUsable(CommandNode<ModernCommandSource> from, CommandNode<ModernCommandSource> to, ModernCommandSource source,
								   Map<CommandNode<ModernCommandSource>, CommandNode<ModernCommandSource>> copies) {
		for (CommandNode<ModernCommandSource> child : from.getChildren()) {
			if (child.canUse(source)) {
				ArgumentBuilder<ModernCommandSource, ?> builder = child.createBuilder();
				if (builder.getRedirect() != null) {
					builder.redirect(copies.get(builder.getRedirect()));
				}
				CommandNode<ModernCommandSource> copy = builder.build();
				copies.put(child, copy);
				to.addChild(copy);
				if (!child.getChildren().isEmpty()) {
					copyUsable(child, copy, source, copies);
				}
			}
		}
	}

	@Desugar
	private record Request(int id, String text) {
	}

	/** One executeCommand: whether the modern command ran and its result, or the modern error for 1.7.10's run. */
	private static final class Run {
		private boolean ranModern;
		private int result;
		private CommandSyntaxException error;
		private String command;
		private boolean errorShown;
	}
}
