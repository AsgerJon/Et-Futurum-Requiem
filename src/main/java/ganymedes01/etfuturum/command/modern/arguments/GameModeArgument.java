package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.Suggest;
import net.minecraft.world.WorldSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** A game mode by its name (spec §51.1): survival, creative, adventure, and EFR's spectator when it is on. */
public final class GameModeArgument implements ArgumentType<WorldSettings.GameType> {

	private static final Collection<String> EXAMPLES = Arrays.asList("survival", "creative");
	private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
		name -> ComponentMessage.translatable("argument.gamemode.invalid", name));

	private GameModeArgument() {
	}

	public static GameModeArgument gameMode() {
		return new GameModeArgument();
	}

	public static WorldSettings.GameType getGameMode(CommandContext<?> context, String name) {
		return context.getArgument(name, WorldSettings.GameType.class);
	}

	/** The modes 1.7.10 (and EFR) has, by name. */
	public static List<String> names() {
		List<String> names = new ArrayList<>();
		for (WorldSettings.GameType type : WorldSettings.GameType.values()) {
			if (type != WorldSettings.GameType.NOT_SET) {
				names.add(type.getName());
			}
		}
		return names;
	}

	@Override
	public WorldSettings.GameType parse(StringReader reader) throws CommandSyntaxException {
		String name = reader.readUnquotedString();
		if (!names().contains(name)) {
			throw INVALID.createWithContext(reader, name);
		}
		return WorldSettings.GameType.getByName(name);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return Suggest.matching(names(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
