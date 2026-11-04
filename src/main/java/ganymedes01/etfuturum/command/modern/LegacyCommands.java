package ganymedes01.etfuturum.command.modern;

import io.github.asgerjon.mcfly.McFly;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 1.7.10 commands in a Brigadier tree (spec §40.5): the name as a literal that runs, with one optional greedy
 * argument `args` whose suggestions are the command's own tab completion. Used for the server's commands and for
 * Forge's client commands.
 */
public final class LegacyCommands {

	private static final Logger LOGGER = LogManager.getLogger(McFly.DOMAIN);

	private LegacyCommands() {
	}

	public static <S> LiteralArgumentBuilder<S> wrap(String name, ICommand command, Function<S, ICommandSender> sender,
													 Predicate<S> requirement, Command<S> run) {
		return LiteralArgumentBuilder.<S>literal(name)
			.requires(requirement)
			.executes(run)
			.then(RequiredArgumentBuilder.<S, String>argument("args", StringArgumentType.greedyString())
				.suggests((context, builder) -> suggest(command, sender.apply(context.getSource()), builder))
				.executes(run));
	}

	/** Whether a name can be a literal: 1.7.10 splits commands at spaces. */
	public static boolean isWrappable(String name) {
		return !name.isEmpty() && name.indexOf(' ') < 0;
	}

	/** The command's tab completion for what is typed after its name, each replacing the last word as in 1.7.10. */
	public static CompletableFuture<Suggestions> suggest(ICommand command, ICommandSender sender, SuggestionsBuilder builder) {
		String typed = builder.getRemaining();
		List<String> completions;
		try {
			completions = command.addTabCompletionOptions(sender, typed.split(" ", -1));
		} catch (Exception e) {
			LOGGER.debug("Tab completion of /{} failed", command.getCommandName(), e);
			completions = null;
		}
		if (completions == null || completions.isEmpty()) {
			return builder.buildFuture();
		}
		SuggestionsBuilder lastWord = builder.createOffset(builder.getStart() + typed.lastIndexOf(' ') + 1);
		for (Object completion : completions) {
			lastWord.suggest(String.valueOf(completion));
		}
		return lastWord.buildFuture();
	}

	public static boolean canUse(ICommand command, ICommandSender sender) {
		try {
			return command.canCommandSenderUseCommand(sender);
		} catch (Exception e) {
			return false;
		}
	}

	/** The command's usage text for this sender (usually a lang key), or null. */
	public static String usage(ICommand command, ICommandSender sender) {
		try {
			String usage = command.getCommandUsage(sender);
			return usage == null || usage.isEmpty() ? null : usage;
		} catch (Exception e) {
			return null;
		}
	}
}
