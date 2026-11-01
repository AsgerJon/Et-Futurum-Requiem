package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.entity.EntityNbtPattern;
import ganymedes01.etfuturum.command.modern.entity.EntityTags;
import ganymedes01.etfuturum.command.modern.item.DecodeException;
import ganymedes01.etfuturum.command.modern.nbt.SnbtReader;
import ganymedes01.etfuturum.command.modern.Suggest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.world.WorldSettings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** The options a selector takes in square brackets (spec §43.3), on both sides. */
public final class EntitySelectorOptions {

	static final DynamicCommandExceptionType UNKNOWN_OPTION = dynamic("argument.entity.options.unknown");
	static final DynamicCommandExceptionType INAPPLICABLE_OPTION = dynamic("argument.entity.options.inapplicable");
	static final SimpleCommandExceptionType DISTANCE_NEGATIVE = simple("argument.entity.options.distance.negative");
	static final SimpleCommandExceptionType LEVEL_NEGATIVE = simple("argument.entity.options.level.negative");
	static final SimpleCommandExceptionType LIMIT_TOO_SMALL = simple("argument.entity.options.limit.toosmall");
	static final DynamicCommandExceptionType SORT_UNKNOWN = dynamic("argument.entity.options.sort.irreversible");
	static final DynamicCommandExceptionType GAME_MODE_INVALID = dynamic("argument.entity.options.mode.invalid");
	static final DynamicCommandExceptionType ENTITY_TYPE_INVALID = dynamic("argument.entity.options.type.invalid");
	/** EFR's own, for the option that waits for milestone 2's reader of modern's text form. */
	static final SimpleCommandExceptionType EXPECTED_COMPOUND = simple("argument.nbt.expected.compound");
	/** EFR's own: data the pattern can't be read from. */
	static final DynamicCommandExceptionType BAD_NBT = dynamic("argument.etfuturum.entity.options.nbt.bad");

	private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-or]");
	private static final Map<String, Option> OPTIONS = new LinkedHashMap<>();

	static {
		register("name", parser -> {
			int start = parser.reader.getCursor();
			boolean inverted = parser.shouldInvertValue();
			String name = parser.reader.readString();
			if (!parser.nameOption.canParse(inverted)) {
				throw rollback(parser, start, INAPPLICABLE_OPTION, "name");
			}
			parser.nameOption.markParsed(inverted);
			parser.predicates.add(e -> plainName(e).equals(name) != inverted);
		}, parser -> parser.nameOption.canParseAny());
		register("distance", parser -> {
			int start = parser.reader.getCursor();
			MinMaxBounds.Doubles value = MinMaxBounds.Doubles.fromReader(parser.reader);
			if (value.min != null && value.min < 0.0D || value.max != null && value.max < 0.0D) {
				throw rollback(parser, start, DISTANCE_NEGATIVE);
			}
			parser.distance = value;
			parser.worldLimited = true;
		}, parser -> parser.distance == null);
		register("level", parser -> {
			int start = parser.reader.getCursor();
			MinMaxBounds.Ints value = MinMaxBounds.Ints.fromReader(parser.reader);
			if (value.min != null && value.min < 0 || value.max != null && value.max < 0) {
				throw rollback(parser, start, LEVEL_NEGATIVE);
			}
			parser.level = value;
			parser.includesEntities = false;
		}, parser -> parser.level == null);
		register("x", parser -> {
			parser.worldLimited = true;
			parser.x = parser.reader.readDouble();
		}, parser -> parser.x == null);
		register("y", parser -> {
			parser.worldLimited = true;
			parser.y = parser.reader.readDouble();
		}, parser -> parser.y == null);
		register("z", parser -> {
			parser.worldLimited = true;
			parser.z = parser.reader.readDouble();
		}, parser -> parser.z == null);
		register("dx", parser -> {
			parser.worldLimited = true;
			parser.deltaX = parser.reader.readDouble();
		}, parser -> parser.deltaX == null);
		register("dy", parser -> {
			parser.worldLimited = true;
			parser.deltaY = parser.reader.readDouble();
		}, parser -> parser.deltaY == null);
		register("dz", parser -> {
			parser.worldLimited = true;
			parser.deltaZ = parser.reader.readDouble();
		}, parser -> parser.deltaZ == null);
		register("x_rotation", parser -> parser.rotX = MinMaxBounds.FloatDegrees.fromReader(parser.reader), parser -> parser.rotX == null);
		register("y_rotation", parser -> parser.rotY = MinMaxBounds.FloatDegrees.fromReader(parser.reader), parser -> parser.rotY == null);
		register("limit", parser -> {
			int start = parser.reader.getCursor();
			int count = parser.reader.readInt();
			if (count < 1) {
				throw rollback(parser, start, LIMIT_TOO_SMALL);
			}
			parser.maxResults = count;
			parser.limitOption.markParsed();
		}, parser -> !parser.currentEntity && parser.limitOption.canParse());
		register("sort", parser -> {
			int start = parser.reader.getCursor();
			String name = parser.reader.readUnquotedString();
			parser.setSuggestions((builder, names) -> Suggest.matching(Arrays.asList("nearest", "furthest", "random", "arbitrary"), builder));
			parser.order = switch (name) {
				case "nearest" -> EntitySelector.Order.NEAREST;
				case "furthest" -> EntitySelector.Order.FURTHEST;
				case "random" -> EntitySelector.Order.RANDOM;
				case "arbitrary" -> EntitySelector.Order.ARBITRARY;
				default -> throw rollback(parser, start, SORT_UNKNOWN, name);
			};
			parser.sortOption.markParsed();
		}, parser -> !parser.currentEntity && parser.sortOption.canParse());
		register("gamemode", parser -> {
			EntitySelectorParser.NegatableOption state = parser.gamemodeOption;
			parser.setSuggestions((builder, names) -> {
				String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
				boolean positive = state.canParsePositive();
				boolean negative = state.canParseNegative();
				if (!typed.isEmpty()) {
					if (typed.charAt(0) == '!') {
						positive = false;
						typed = typed.substring(1);
					} else {
						negative = false;
					}
				}
				for (WorldSettings.GameType mode : gameModes()) {
					if (mode.getName().startsWith(typed)) {
						if (negative) {
							builder.suggest("!" + mode.getName());
						}
						if (positive) {
							builder.suggest(mode.getName());
						}
					}
				}
				return builder.buildFuture();
			});
			int start = parser.reader.getCursor();
			boolean inverted = parser.shouldInvertValue();
			if (!state.canParse(inverted)) {
				throw rollback(parser, start, INAPPLICABLE_OPTION, "gamemode");
			}
			String name = parser.reader.readUnquotedString();
			WorldSettings.GameType expected = gameMode(name);
			if (expected == null) {
				throw rollback(parser, start, GAME_MODE_INVALID, name);
			}
			parser.includesEntities = false;
			parser.predicates.add(e -> e instanceof EntityPlayerMP player && (player.theItemInWorldManager.getGameType() == expected) != inverted);
			state.markParsed(inverted);
		}, parser -> parser.gamemodeOption.canParseAny());
		register("team", parser -> {
			EntitySelectorParser.NegatableOption state = parser.teamOption;
			int start = parser.reader.getCursor();
			boolean inverted = parser.shouldInvertValue();
			String expected = parser.reader.readUnquotedString();
			if (!state.canParse(inverted)) {
				throw rollback(parser, start, INAPPLICABLE_OPTION, "team");
			}
			parser.predicates.add(e -> {
				ScorePlayerTeam team = e.worldObj.getScoreboard().getPlayersTeam(scoreHolder(e));
				return (team == null ? "" : team.getRegisteredName()).equals(expected) != inverted;
			});
			state.markParsed(inverted);
		}, parser -> parser.teamOption.canParseAny());
		register("type", parser -> {
			EntitySelectorParser.NegatableOption state = parser.typeOption;
			parser.setSuggestions((builder, names) -> {
				if (state.canParseNegative()) {
					Suggest.resources(EntityTypeIds.ids(), builder, "!");
				}
				if (state.canParsePositive()) {
					Suggest.resources(EntityTypeIds.ids(), builder);
				}
				// 1.7.10 has no entity type tags to offer
				return builder.buildFuture();
			});
			int start = parser.reader.getCursor();
			boolean inverted = parser.shouldInvertValue();
			if (parser.isTag()) {
				if (!state.canParseNegative()) {
					throw rollback(parser, start, INAPPLICABLE_OPTION, "type");
				}
				String tag = ResourceIds.read(parser.reader);
				if (!state.canParseTag(tag)) {
					throw rollback(parser, start, INAPPLICABLE_OPTION, "type");
				}
				// no tag has members, as for a modern tag nothing is in
				parser.predicates.add(e -> inverted);
				state.markParsedTag(tag);
			} else {
				if (!state.canParse(inverted)) {
					throw rollback(parser, start, INAPPLICABLE_OPTION, "type");
				}
				String id = ResourceIds.read(parser.reader);
				if (!EntityTypeIds.exists(id)) {
					throw rollback(parser, start, ENTITY_TYPE_INVALID, id);
				}
				if (id.equals(EntityTypeIds.PLAYER) && !inverted) {
					parser.includesEntities = false;
				}
				parser.predicates.add(e -> id.equals(EntityTypeIds.idOf(e)) != inverted);
				state.markParsed(inverted);
			}
		}, parser -> parser.typeOption.canParseAny());
		register("tag", parser -> {
			boolean inverted = parser.shouldInvertValue();
			String tag = parser.reader.readUnquotedString();
			// `tag=` alone: no tags at all; `tag=!` alone: any tag
			parser.predicates.add(e -> {
				Set<String> tags = EntityTags.get(e);
				return (tag.isEmpty() ? tags.isEmpty() : tags.contains(tag)) != inverted;
			});
		}, parser -> true);
		register("nbt", parser -> {
			boolean inverted = parser.shouldInvertValue();
			int start = parser.reader.getCursor();
			Object value = SnbtReader.read(parser.reader);
			if (!(value instanceof Map<?, ?> data)) {
				parser.reader.setCursor(start);
				throw EXPECTED_COMPOUND.createWithContext(parser.reader);
			}
			EntityNbtPattern pattern;
			try {
				pattern = EntityNbtPattern.of(data);
			} catch (DecodeException e) {
				parser.reader.setCursor(start);
				throw BAD_NBT.createWithContext(parser.reader, e.getMessage());
			}
			parser.predicates.add(e -> pattern.matches(e) != inverted);
		}, parser -> true);
		register("scores", parser -> {
			StringReader reader = parser.reader;
			Map<String, MinMaxBounds.Ints> expected = new HashMap<>();
			reader.expect('{');
			reader.skipWhitespace();
			while (reader.canRead() && reader.peek() != '}') {
				reader.skipWhitespace();
				String objective = reader.readUnquotedString();
				reader.skipWhitespace();
				reader.expect('=');
				reader.skipWhitespace();
				expected.put(objective, MinMaxBounds.Ints.fromReader(reader));
				reader.skipWhitespace();
				if (reader.canRead() && reader.peek() == ',') {
					reader.skip();
				}
			}
			reader.expect('}');
			if (!expected.isEmpty()) {
				parser.predicates.add(e -> hasScores(e, expected));
			}
			parser.scoresOption.markParsed();
		}, parser -> parser.scoresOption.canParse());
		register("advancements", parser -> {
			StringReader reader = parser.reader;
			boolean any = false;
			reader.expect('{');
			reader.skipWhitespace();
			while (reader.canRead() && reader.peek() != '}') {
				reader.skipWhitespace();
				ResourceIds.read(reader);
				reader.skipWhitespace();
				reader.expect('=');
				reader.skipWhitespace();
				if (reader.canRead() && reader.peek() == '{') {
					reader.skip();
					reader.skipWhitespace();
					while (reader.canRead() && reader.peek() != '}') {
						reader.skipWhitespace();
						reader.readUnquotedString();
						reader.skipWhitespace();
						reader.expect('=');
						reader.skipWhitespace();
						reader.readBoolean();
						reader.skipWhitespace();
						if (reader.canRead() && reader.peek() == ',') {
							reader.skip();
						}
					}
					reader.skipWhitespace();
					reader.expect('}');
					reader.skipWhitespace();
				} else {
					reader.readBoolean();
				}
				any = true;
				reader.skipWhitespace();
				if (reader.canRead() && reader.peek() == ',') {
					reader.skip();
				}
			}
			reader.expect('}');
			if (any) {
				// 1.7.10 has no advancements: every id is unknown, which fails as in modern
				parser.predicates.add(e -> false);
				parser.includesEntities = false;
			}
			parser.advancementsOption.markParsed();
		}, parser -> parser.advancementsOption.canParse());
		register("predicate", parser -> {
			parser.shouldInvertValue();
			ResourceIds.read(parser.reader);
			// 1.7.10 has no data pack predicates; a missing one fails, negated or not
			parser.predicates.add(e -> false);
		}, parser -> true);
	}

	private EntitySelectorOptions() {
	}

	private static SimpleCommandExceptionType simple(String key) {
		return new SimpleCommandExceptionType(ComponentMessage.translatable(key));
	}

	private static DynamicCommandExceptionType dynamic(String key) {
		return new DynamicCommandExceptionType(value -> ComponentMessage.translatable(key, value));
	}

	private static void register(String name, Modifier modifier, Predicate<EntitySelectorParser> canUse) {
		OPTIONS.put(name, new Option(modifier, canUse, ComponentMessage.translatable("argument.entity.options." + name + ".description")));
	}

	private static CommandSyntaxException rollback(EntitySelectorParser parser, int start, SimpleCommandExceptionType type) {
		parser.reader.setCursor(start);
		return type.createWithContext(parser.reader);
	}

	private static CommandSyntaxException rollback(EntitySelectorParser parser, int start, DynamicCommandExceptionType type, String value) {
		parser.reader.setCursor(start);
		return type.createWithContext(parser.reader, value);
	}

	static Modifier get(EntitySelectorParser parser, String key, int start) throws CommandSyntaxException {
		Option option = OPTIONS.get(key);
		if (option == null) {
			throw rollback(parser, start, UNKNOWN_OPTION, key);
		}
		if (!option.canUse.test(parser)) {
			throw rollback(parser, start, INAPPLICABLE_OPTION, key);
		}
		return option.modifier;
	}

	/** The keys usable now that start with what is typed, as `key=` with their descriptions. */
	static void suggestNames(EntitySelectorParser parser, SuggestionsBuilder builder) {
		String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Option> entry : OPTIONS.entrySet()) {
			if (entry.getValue().canUse.test(parser) && entry.getKey().startsWith(typed)) {
				builder.suggest(entry.getKey() + "=", entry.getValue().description);
			}
		}
	}

	/** 1.7.10's game modes: survival, creative and adventure (no spectator). */
	private static WorldSettings.GameType[] gameModes() {
		return new WorldSettings.GameType[]{WorldSettings.GameType.SURVIVAL, WorldSettings.GameType.CREATIVE, WorldSettings.GameType.ADVENTURE};
	}

	private static WorldSettings.GameType gameMode(String name) {
		for (WorldSettings.GameType mode : gameModes()) {
			if (mode.getName().equals(name)) {
				return mode;
			}
		}
		return null;
	}

	/** The name modern compares: a custom name, a player's name, else the type's name; without formatting codes. */
	private static String plainName(Entity entity) {
		return FORMATTING.matcher(entity.getCommandSenderName()).replaceAll("");
	}

	/** Who holds scores and teams: a player by name, any other entity by its UUID, as modern. */
	static String scoreHolder(Entity entity) {
		return entity instanceof EntityPlayer ? entity.getCommandSenderName() : entity.getUniqueID().toString();
	}

	@SuppressWarnings("unchecked")
	private static boolean hasScores(Entity entity, Map<String, MinMaxBounds.Ints> expected) {
		Scoreboard scoreboard = entity.worldObj.getScoreboard();
		Map<ScoreObjective, Score> scores = (Map<ScoreObjective, Score>) (Map<?, ?>) scoreboard.func_96510_d(scoreHolder(entity));
		for (Map.Entry<String, MinMaxBounds.Ints> entry : expected.entrySet()) {
			ScoreObjective objective = scoreboard.getObjective(entry.getKey());
			Score score = objective == null ? null : scores.get(objective);
			if (score == null || !entry.getValue().matches(score.getScorePoints())) {
				return false;
			}
		}
		return true;
	}

	@FunctionalInterface
	interface Modifier {
		void handle(EntitySelectorParser parser) throws CommandSyntaxException;
	}

	private static final class Option {
		private final Modifier modifier;
		private final Predicate<EntitySelectorParser> canUse;
		private final ComponentMessage description;

		private Option(Modifier modifier, Predicate<EntitySelectorParser> canUse, ComponentMessage description) {
			this.modifier = modifier;
			this.canUse = canUse;
			this.description = description;
		}
	}
}
