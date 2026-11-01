package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * Reads a target (spec §41.2): a player name, a UUID or a selector with its options (§43). While it reads it
 * remembers what to suggest at the point it stopped, so the same parse drives the suggestion list.
 */
public final class EntitySelectorParser {

	static final SimpleCommandExceptionType INVALID_NAME_OR_UUID = simple("argument.entity.invalid");
	static final DynamicCommandExceptionType UNKNOWN_SELECTOR_TYPE = dynamic("argument.entity.selector.unknown");
	static final SimpleCommandExceptionType SELECTORS_NOT_ALLOWED = simple("argument.entity.selector.not_allowed");
	static final SimpleCommandExceptionType MISSING_SELECTOR_TYPE = simple("argument.entity.selector.missing");
	static final SimpleCommandExceptionType EXPECTED_END_OF_OPTIONS = simple("argument.entity.options.unterminated");
	static final DynamicCommandExceptionType EXPECTED_OPTION_VALUE = dynamic("argument.entity.options.valueless");

	static final BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> SUGGEST_NOTHING =
		(builder, names) -> builder.buildFuture();

	final StringReader reader;
	private final boolean allowSelectors;
	private BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;
	private int startPosition;
	int maxResults;
	boolean includesEntities;
	boolean worldLimited;
	MinMaxBounds.Doubles distance;
	MinMaxBounds.Ints level;
	Double x;
	Double y;
	Double z;
	Double deltaX;
	Double deltaY;
	Double deltaZ;
	MinMaxBounds.FloatDegrees rotX;
	MinMaxBounds.FloatDegrees rotY;
	final List<Predicate<Entity>> predicates = new ArrayList<>();
	EntitySelector.Order order = EntitySelector.Order.ARBITRARY;
	boolean currentEntity;
	private String playerName;
	private UUID uuid;
	private boolean usesSelector;
	final NegatableOption nameOption = new NegatableOption();
	final OnceOption limitOption = new OnceOption();
	final OnceOption sortOption = new OnceOption();
	final NegatableOption gamemodeOption = new NegatableOption();
	final NegatableOption teamOption = new NegatableOption();
	final NegatableOption typeOption = new NegatableOption();
	final OnceOption scoresOption = new OnceOption();
	final OnceOption advancementsOption = new OnceOption();

	public EntitySelectorParser(StringReader reader, boolean allowSelectors) {
		this.reader = reader;
		this.allowSelectors = allowSelectors;
	}

	private static SimpleCommandExceptionType simple(String key) {
		return new SimpleCommandExceptionType(ComponentMessage.translatable(key));
	}

	private static DynamicCommandExceptionType dynamic(String key) {
		return new DynamicCommandExceptionType(value -> ComponentMessage.translatable(key, value));
	}

	public EntitySelector parse() throws CommandSyntaxException {
		startPosition = reader.getCursor();
		suggestions = this::suggestNameOrSelector;
		if (reader.canRead() && reader.peek() == '@') {
			if (!allowSelectors) {
				throw SELECTORS_NOT_ALLOWED.createWithContext(reader);
			}
			reader.skip();
			parseSelector();
		} else {
			parseNameOrUuid();
		}
		finalizePredicates();
		return new EntitySelector(maxResults, includesEntities, worldLimited, new ArrayList<>(predicates), distance, x, y, z,
			box(), order, currentEntity, playerName, uuid, usesSelector);
	}

	/** From the origin to the origin plus dx, dy and dz, one block more on the far side of each axis (§43.3). */
	private AxisAlignedBB box() {
		if (deltaX == null && deltaY == null && deltaZ == null) {
			return null;
		}
		double dx = deltaX == null ? 0.0D : deltaX;
		double dy = deltaY == null ? 0.0D : deltaY;
		double dz = deltaZ == null ? 0.0D : deltaZ;
		return AxisAlignedBB.getBoundingBox(Math.min(dx, 0.0D), Math.min(dy, 0.0D), Math.min(dz, 0.0D),
			Math.max(dx, 0.0D) + 1.0D, Math.max(dy, 0.0D) + 1.0D, Math.max(dz, 0.0D) + 1.0D);
	}

	private void finalizePredicates() {
		if (rotX != null) {
			predicates.add(rotationTest(rotX, e -> e.rotationPitch));
		}
		if (rotY != null) {
			predicates.add(rotationTest(rotY, e -> e.rotationYaw));
		}
		if (level != null) {
			MinMaxBounds.Ints levels = level;
			predicates.add(e -> e instanceof EntityPlayer player && levels.matches(player.experienceLevel));
		}
	}

	/** Both ends and the angle wrapped into -180 to 180; a range whose low end is above its high end runs around. */
	private static Predicate<Entity> rotationTest(MinMaxBounds.FloatDegrees range, ToDoubleFunction<Entity> rotation) {
		float min = MathHelper.wrapAngleTo180_float(range.min != null ? range.min : 0.0F);
		float max = MathHelper.wrapAngleTo180_float(range.max != null ? range.max : 359.0F);
		return e -> {
			float angle = MathHelper.wrapAngleTo180_float((float) rotation.applyAsDouble(e));
			return min > max ? angle >= min || angle <= max : angle >= min && angle <= max;
		};
	}

	/** @p nearest player, @a all players, @r a random player, @s the running entity, @e all, @n the nearest; the last two only living ones. */
	private void parseSelector() throws CommandSyntaxException {
		usesSelector = true;
		suggestions = this::suggestSelector;
		if (!reader.canRead()) {
			throw MISSING_SELECTOR_TYPE.createWithContext(reader);
		}
		int start = reader.getCursor();
		char type = reader.read();
		switch (type) {
			case 'a' -> maxResults = Integer.MAX_VALUE;
			case 'e' -> {
				maxResults = Integer.MAX_VALUE;
				includesEntities = true;
				predicates.add(Entity::isEntityAlive);
			}
			case 'n' -> {
				maxResults = 1;
				includesEntities = true;
				order = EntitySelector.Order.NEAREST;
				predicates.add(Entity::isEntityAlive);
			}
			case 'p' -> {
				maxResults = 1;
				order = EntitySelector.Order.NEAREST;
			}
			case 'r' -> {
				maxResults = 1;
				order = EntitySelector.Order.RANDOM;
			}
			case 's' -> {
				maxResults = 1;
				includesEntities = true;
				currentEntity = true;
			}
			default -> {
				reader.setCursor(start);
				throw UNKNOWN_SELECTOR_TYPE.createWithContext(reader, "@" + type);
			}
		}
		suggestions = this::suggestOpenOptions;
		if (reader.canRead() && reader.peek() == '[') {
			reader.skip();
			suggestions = this::suggestOptionsKeyOrClose;
			parseOptions();
		}
	}

	/** `key=value` pairs separated by `,` up to `]` (§43.1). */
	private void parseOptions() throws CommandSyntaxException {
		suggestions = this::suggestOptionsKey;
		reader.skipWhitespace();
		while (reader.canRead() && reader.peek() != ']') {
			reader.skipWhitespace();
			int start = reader.getCursor();
			String key = reader.readString();
			EntitySelectorOptions.Modifier modifier = EntitySelectorOptions.get(this, key, start);
			reader.skipWhitespace();
			if (!reader.canRead() || reader.peek() != '=') {
				reader.setCursor(start);
				throw EXPECTED_OPTION_VALUE.createWithContext(reader, key);
			}
			reader.skip();
			reader.skipWhitespace();
			suggestions = SUGGEST_NOTHING;
			modifier.handle(this);
			reader.skipWhitespace();
			suggestions = this::suggestOptionsNextOrClose;
			if (reader.canRead()) {
				if (reader.peek() != ',') {
					if (reader.peek() != ']') {
						throw EXPECTED_END_OF_OPTIONS.createWithContext(reader);
					}
					break;
				}
				reader.skip();
				suggestions = this::suggestOptionsKey;
			}
		}
		if (!reader.canRead()) {
			throw EXPECTED_END_OF_OPTIONS.createWithContext(reader);
		}
		reader.skip();
		suggestions = SUGGEST_NOTHING;
	}

	/** A leading `!` (spaces around it allowed) negates the value. */
	boolean shouldInvertValue() {
		return skipMark('!');
	}

	/** A leading `#` names a tag. */
	boolean isTag() {
		return skipMark('#');
	}

	private boolean skipMark(char mark) {
		reader.skipWhitespace();
		if (reader.canRead() && reader.peek() == mark) {
			reader.skip();
			reader.skipWhitespace();
			return true;
		}
		return false;
	}

	void setSuggestions(BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestions) {
		this.suggestions = suggestions;
	}

	/** A UUID, else a player name of 1 to 16 characters. */
	private void parseNameOrUuid() throws CommandSyntaxException {
		if (reader.canRead()) {
			suggestions = this::suggestName;
		}
		int start = reader.getCursor();
		String name = reader.readString();
		try {
			uuid = UUID.fromString(name);
			includesEntities = true;
		} catch (IllegalArgumentException notUuid) {
			if (name.isEmpty() || name.length() > 16) {
				reader.setCursor(start);
				throw INVALID_NAME_OR_UUID.createWithContext(reader);
			}
			playerName = name;
		}
		maxResults = 1;
	}

	/** The suggestions for where the parse stopped; {@code names} adds the names that fit. */
	public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
		return suggestions.apply(builder.createOffset(reader.getCursor()), names);
	}

	private static void suggestSelectors(SuggestionsBuilder builder) {
		builder.suggest("@p", ComponentMessage.translatable("argument.entity.selector.nearestPlayer"));
		builder.suggest("@a", ComponentMessage.translatable("argument.entity.selector.allPlayers"));
		builder.suggest("@r", ComponentMessage.translatable("argument.entity.selector.randomPlayer"));
		builder.suggest("@s", ComponentMessage.translatable("argument.entity.selector.self"));
		builder.suggest("@e", ComponentMessage.translatable("argument.entity.selector.allEntities"));
		builder.suggest("@n", ComponentMessage.translatable("argument.entity.selector.nearestEntity"));
	}

	private CompletableFuture<Suggestions> suggestNameOrSelector(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
		names.accept(builder);
		if (allowSelectors) {
			suggestSelectors(builder);
		}
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestName(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
		SuggestionsBuilder fromStart = builder.createOffset(startPosition);
		names.accept(fromStart);
		return builder.add(fromStart).buildFuture();
	}

	/** After `@`: the selectors, replacing the `@` too. */
	private CompletableFuture<Suggestions> suggestSelector(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
		SuggestionsBuilder fromAt = builder.createOffset(builder.getStart() - 1);
		suggestSelectors(fromAt);
		builder.add(fromAt);
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestOpenOptions(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
		builder.suggest("[");
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestOptionsKeyOrClose(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
		builder.suggest("]");
		EntitySelectorOptions.suggestNames(this, builder);
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestOptionsKey(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
		EntitySelectorOptions.suggestNames(this, builder);
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestOptionsNextOrClose(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
		builder.suggest(",");
		builder.suggest("]");
		return builder.buildFuture();
	}

	/** An option allowed once. */
	static final class OnceOption {
		private boolean parsed;

		boolean canParse() {
			return !parsed;
		}

		void markParsed() {
			parsed = true;
		}
	}

	/**
	 * An option taking either one positive value and nothing more, or any number of negated values and tags, each tag
	 * once (§43.3).
	 */
	static final class NegatableOption {
		private enum State {
			NONE, SINGLE, MULTIPLE
		}

		private State state = State.NONE;
		private final Set<String> tags = new HashSet<>();

		boolean canParseAny() {
			return state != State.SINGLE;
		}

		boolean canParsePositive() {
			return state == State.NONE;
		}

		boolean canParseNegative() {
			return state != State.SINGLE;
		}

		boolean canParse(boolean inverted) {
			return inverted ? canParseNegative() : canParsePositive();
		}

		boolean canParseTag(String tag) {
			return canParseNegative() && !tags.contains(tag);
		}

		void markParsed(boolean inverted) {
			state = inverted ? State.MULTIPLE : State.SINGLE;
		}

		void markParsedTag(String tag) {
			state = State.MULTIPLE;
			tags.add(tag);
		}
	}
}
