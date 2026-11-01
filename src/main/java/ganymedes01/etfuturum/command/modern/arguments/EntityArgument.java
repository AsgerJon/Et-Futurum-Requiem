package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.Suggest;
import ganymedes01.etfuturum.command.modern.SuggestionSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** A target argument (spec §41.2): one or more entities or players, by name, UUID or selector. */
public final class EntityArgument implements ArgumentType<EntitySelector> {

	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "dd12be42-52a9-4a91-a8a1-11c01849e498");
	public static final SimpleCommandExceptionType NOT_SINGLE_ENTITY = simple("argument.entity.toomany");
	public static final SimpleCommandExceptionType NOT_SINGLE_PLAYER = simple("argument.player.toomany");
	public static final SimpleCommandExceptionType ONLY_PLAYERS_ALLOWED = simple("argument.player.entities");
	public static final SimpleCommandExceptionType NO_ENTITIES_FOUND = simple("argument.entity.notfound.entity");
	public static final SimpleCommandExceptionType NO_PLAYERS_FOUND = simple("argument.entity.notfound.player");

	private final boolean single;
	private final boolean playersOnly;

	private EntityArgument(boolean single, boolean playersOnly) {
		this.single = single;
		this.playersOnly = playersOnly;
	}

	public static EntityArgument entities() {
		return new EntityArgument(false, false);
	}

	public static EntityArgument players() {
		return new EntityArgument(false, true);
	}

	public static EntityArgument of(boolean single, boolean playersOnly) {
		return new EntityArgument(single, playersOnly);
	}

	public boolean single() {
		return single;
	}

	public boolean playersOnly() {
		return playersOnly;
	}

	private static SimpleCommandExceptionType simple(String key) {
		return new SimpleCommandExceptionType(ComponentMessage.translatable(key));
	}

	/** The targets found; none is "No entity was found". */
	public static List<Entity> getEntities(CommandContext<ModernCommandSource> context, String name) throws CommandSyntaxException {
		List<Entity> entities = context.getArgument(name, EntitySelector.class).findEntities(context.getSource());
		if (entities.isEmpty()) {
			throw NO_ENTITIES_FOUND.create();
		}
		return entities;
	}

	/** The players found; none is "No player was found". */
	public static List<EntityPlayerMP> getPlayers(CommandContext<ModernCommandSource> context, String name) throws CommandSyntaxException {
		List<EntityPlayerMP> players = context.getArgument(name, EntitySelector.class).findPlayers(context.getSource());
		if (players.isEmpty()) {
			throw NO_PLAYERS_FOUND.create();
		}
		return players;
	}

	@Override
	public EntitySelector parse(StringReader reader) throws CommandSyntaxException {
		return parse(reader, true);
	}

	@Override
	public <S> EntitySelector parse(StringReader reader, S source) throws CommandSyntaxException {
		return parse(reader, !(source instanceof SuggestionSource suggestionSource) || suggestionSource.allowsSelectors());
	}

	private EntitySelector parse(StringReader reader, boolean allowSelectors) throws CommandSyntaxException {
		EntitySelector selector = new EntitySelectorParser(reader, allowSelectors).parse();
		if (selector.maxResults() > 1 && single) {
			reader.setCursor(0);
			throw (playersOnly ? NOT_SINGLE_PLAYER : NOT_SINGLE_ENTITY).createWithContext(reader);
		}
		if (selector.includesEntities() && playersOnly && !selector.isSelfSelector()) {
			reader.setCursor(0);
			throw ONLY_PLAYERS_ALLOWED.createWithContext(reader);
		}
		return selector;
	}

	/** Parses what is typed of the argument and suggests for where that stopped: names and selectors. */
	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		if (!(context.getSource() instanceof SuggestionSource source)) {
			return Suggestions.empty();
		}
		StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());
		EntitySelectorParser parser = new EntitySelectorParser(reader, source.allowsSelectors());
		try {
			parser.parse();
		} catch (CommandSyntaxException ignored) {
			// suggest for where it stopped
		}
		return parser.fillSuggestions(builder, names -> Suggest.matching(source.onlinePlayerNames(), names));
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
