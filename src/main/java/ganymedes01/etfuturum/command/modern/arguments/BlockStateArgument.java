package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.Suggest;
import ganymedes01.etfuturum.command.modern.block.LegacyBlockEntityData;
import ganymedes01.etfuturum.command.modern.item.DecodeException;
import ganymedes01.etfuturum.command.modern.nbt.SnbtReader;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A block, `id[name=value,...]{data}` (spec §49.1). Both sides know the blocks, so the client parses and suggests
 * itself. The parser also reads block predicates, which take `#tag` too (BlockPredicateArgument).
 */
public final class BlockStateArgument implements ArgumentType<BlockInput> {

	private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "foo{bar=baz}");
	private static final SimpleCommandExceptionType NO_TAGS = simple("argument.block.tag.disallowed");
	private static final DynamicCommandExceptionType UNKNOWN_BLOCK = dynamic("argument.block.id.invalid");
	private static final DynamicCommandExceptionType UNKNOWN_TAG = dynamic("arguments.block.tag.unknown");
	private static final Dynamic2CommandExceptionType UNKNOWN_PROPERTY = new Dynamic2CommandExceptionType(
		(block, property) -> ComponentMessage.translatable("argument.block.property.unknown", block, property));
	private static final Dynamic2CommandExceptionType DUPLICATE_PROPERTY = new Dynamic2CommandExceptionType(
		(block, property) -> ComponentMessage.translatable("argument.block.property.duplicate", property, block));
	private static final Dynamic3CommandExceptionType INVALID_VALUE = new Dynamic3CommandExceptionType(
		(block, property, value) -> ComponentMessage.translatable("argument.block.property.invalid", block, value, property));
	private static final Dynamic2CommandExceptionType EXPECTED_VALUE = new Dynamic2CommandExceptionType(
		(block, property) -> ComponentMessage.translatable("argument.block.property.novalue", property, block));
	private static final SimpleCommandExceptionType UNCLOSED = simple("argument.block.property.unclosed");
	private static final SimpleCommandExceptionType EXPECTED_COMPOUND = simple("argument.nbt.expected.compound");
	/** EFR's own: modern loads what it can of block data and logs the rest. */
	private static final DynamicCommandExceptionType BAD_DATA = dynamic("arguments.etfuturum.block.data");
	private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;

	private BlockStateArgument() {
	}

	public static BlockStateArgument block() {
		return new BlockStateArgument();
	}

	public static BlockInput getBlock(CommandContext<?> context, String name) {
		return context.getArgument(name, BlockInput.class);
	}

	private static SimpleCommandExceptionType simple(String key) {
		return new SimpleCommandExceptionType(ComponentMessage.translatable(key));
	}

	private static DynamicCommandExceptionType dynamic(String key) {
		return new DynamicCommandExceptionType(value -> ComponentMessage.translatable(key, value));
	}

	@Override
	public BlockInput parse(StringReader reader) throws CommandSyntaxException {
		return (BlockInput) parse(reader, false);
	}

	/** A block, or with {@code forTesting} a block or tag test. */
	static BlockTest parse(StringReader reader, boolean forTesting) throws CommandSyntaxException {
		int start = reader.getCursor();
		try {
			return new Parser(reader, forTesting).parse();
		} catch (CommandSyntaxException e) {
			reader.setCursor(start);
			throw e;
		}
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return suggest(builder, false);
	}

	static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, boolean forTesting) {
		StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());
		Parser parser = new Parser(reader, forTesting);
		try {
			parser.parse();
		} catch (CommandSyntaxException ignored) {
			// suggest for where it stopped
		}
		return parser.suggestions.apply(builder.createOffset(reader.getCursor()));
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	private static final class Parser {
		private final StringReader reader;
		private final boolean forTesting;
		private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;
		private String id = "";
		private BlockIds.Type type;
		private List<BlockIds.Type> tag;
		private final Map<String, String> given = new HashMap<>();
		private NBTTagCompound data;

		private Parser(StringReader reader, boolean forTesting) {
			this.reader = reader;
			this.forTesting = forTesting;
		}

		private BlockTest parse() throws CommandSyntaxException {
			suggestions = forTesting ? this::suggestBlockOrTag : builder -> Suggest.resources(BlockIds.ids(), builder);
			if (reader.canRead() && reader.peek() == '#') {
				readTag();
				suggestions = this::suggestOpenVaguePropertiesOrData;
				if (reader.canRead() && reader.peek() == '[') {
					readVagueProperties();
					suggestions = this::suggestOpenData;
				}
			} else {
				readBlock();
				suggestions = this::suggestOpenPropertiesOrData;
				if (reader.canRead() && reader.peek() == '[') {
					readProperties();
					suggestions = this::suggestOpenData;
				}
			}
			if (reader.canRead() && reader.peek() == '{') {
				suggestions = SUGGEST_NOTHING;
				readData();
			}
			if (tag != null) {
				return new TagTest(tag, given, data);
			}
			return new BlockInput(type, type.state(given), given, data);
		}

		private void readBlock() throws CommandSyntaxException {
			int start = reader.getCursor();
			id = ResourceIds.read(reader);
			type = BlockIds.byId(id);
			if (type == null) {
				reader.setCursor(start);
				throw UNKNOWN_BLOCK.createWithContext(reader, id);
			}
		}

		private void readTag() throws CommandSyntaxException {
			if (!forTesting) {
				throw NO_TAGS.createWithContext(reader);
			}
			int start = reader.getCursor();
			reader.expect('#');
			suggestions = builder -> Suggest.resources(BlockIds.tagIds(), builder, "#");
			id = ResourceIds.read(reader);
			tag = BlockIds.tag(id);
			if (tag == null) {
				reader.setCursor(start);
				throw UNKNOWN_TAG.createWithContext(reader, id);
			}
		}

		private void readProperties() throws CommandSyntaxException {
			reader.skip();
			suggestions = this::suggestPropertyNameOrEnd;
			reader.skipWhitespace();
			while (reader.canRead() && reader.peek() != ']') {
				reader.skipWhitespace();
				int keyStart = reader.getCursor();
				String key = reader.readString();
				List<String> values = type.properties.get(key);
				if (values == null) {
					reader.setCursor(keyStart);
					throw UNKNOWN_PROPERTY.createWithContext(reader, id, key);
				}
				if (given.containsKey(key)) {
					reader.setCursor(keyStart);
					throw DUPLICATE_PROPERTY.createWithContext(reader, id, key);
				}
				reader.skipWhitespace();
				suggestions = this::suggestEquals;
				if (!reader.canRead() || reader.peek() != '=') {
					throw EXPECTED_VALUE.createWithContext(reader, id, key);
				}
				reader.skip();
				reader.skipWhitespace();
				suggestions = builder -> Suggest.matching(values, builder);
				int valueStart = reader.getCursor();
				String value = reader.readString();
				if (!values.contains(value)) {
					reader.setCursor(valueStart);
					throw INVALID_VALUE.createWithContext(reader, id, key, value);
				}
				given.put(key, value);
				suggestions = this::suggestNextPropertyOrEnd;
				reader.skipWhitespace();
				if (reader.canRead()) {
					if (reader.peek() != ',') {
						if (reader.peek() != ']') {
							throw UNCLOSED.createWithContext(reader);
						}
						break;
					}
					reader.skip();
					suggestions = this::suggestPropertyName;
				}
			}
			if (!reader.canRead()) {
				throw UNCLOSED.createWithContext(reader);
			}
			reader.skip();
		}

		/** A tag's properties go by name: any value is taken here, and a block without the property doesn't match. */
		private void readVagueProperties() throws CommandSyntaxException {
			reader.skip();
			suggestions = this::suggestVaguePropertyNameOrEnd;
			int valueStart = -1;
			reader.skipWhitespace();
			while (reader.canRead() && reader.peek() != ']') {
				reader.skipWhitespace();
				int keyStart = reader.getCursor();
				String key = reader.readString();
				if (given.containsKey(key)) {
					reader.setCursor(keyStart);
					throw DUPLICATE_PROPERTY.createWithContext(reader, id, key);
				}
				reader.skipWhitespace();
				if (!reader.canRead() || reader.peek() != '=') {
					reader.setCursor(keyStart);
					throw EXPECTED_VALUE.createWithContext(reader, id, key);
				}
				reader.skip();
				reader.skipWhitespace();
				suggestions = builder -> suggestVaguePropertyValue(builder, key);
				valueStart = reader.getCursor();
				given.put(key, reader.readString());
				reader.skipWhitespace();
				if (reader.canRead()) {
					valueStart = -1;
					if (reader.peek() != ',') {
						if (reader.peek() != ']') {
							throw UNCLOSED.createWithContext(reader);
						}
						break;
					}
					reader.skip();
					suggestions = this::suggestVaguePropertyName;
				}
			}
			if (!reader.canRead()) {
				if (valueStart >= 0) {
					reader.setCursor(valueStart);
				}
				throw UNCLOSED.createWithContext(reader);
			}
			reader.skip();
		}

		private void readData() throws CommandSyntaxException {
			int start = reader.getCursor();
			Object value = SnbtReader.read(reader);
			if (!(value instanceof Map<?, ?> map)) {
				reader.setCursor(start);
				throw EXPECTED_COMPOUND.createWithContext(reader);
			}
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> modern = (Map<String, Object>) map;
				data = LegacyBlockEntityData.convert(modern);
			} catch (DecodeException e) {
				reader.setCursor(start);
				throw BAD_DATA.createWithContext(reader, e.getMessage());
			}
		}

		private CompletableFuture<Suggestions> suggestBlockOrTag(SuggestionsBuilder builder) {
			Suggest.resources(BlockIds.tagIds(), builder, "#");
			return Suggest.resources(BlockIds.ids(), builder);
		}

		private CompletableFuture<Suggestions> suggestOpenPropertiesOrData(SuggestionsBuilder builder) {
			if (builder.getRemaining().isEmpty()) {
				if (!type.properties.isEmpty()) {
					builder.suggest("[");
				}
				if (type.hasBlockEntity()) {
					builder.suggest("{");
				}
			}
			return builder.buildFuture();
		}

		private CompletableFuture<Suggestions> suggestOpenVaguePropertiesOrData(SuggestionsBuilder builder) {
			if (builder.getRemaining().isEmpty()) {
				boolean properties = false;
				boolean entity = false;
				for (BlockIds.Type member : tag) {
					properties |= !member.properties.isEmpty();
					entity |= member.hasBlockEntity();
				}
				if (properties) {
					builder.suggest("[");
				}
				if (entity) {
					builder.suggest("{");
				}
			}
			return builder.buildFuture();
		}

		private CompletableFuture<Suggestions> suggestOpenData(SuggestionsBuilder builder) {
			boolean entity = false;
			if (type != null) {
				entity = type.hasBlockEntity();
			} else if (tag != null) {
				for (BlockIds.Type member : tag) {
					entity |= member.hasBlockEntity();
				}
			}
			if (builder.getRemaining().isEmpty() && entity) {
				builder.suggest("{");
			}
			return builder.buildFuture();
		}

		private CompletableFuture<Suggestions> suggestPropertyNameOrEnd(SuggestionsBuilder builder) {
			if (builder.getRemaining().isEmpty()) {
				builder.suggest("]");
			}
			return suggestPropertyName(builder);
		}

		private CompletableFuture<Suggestions> suggestPropertyName(SuggestionsBuilder builder) {
			String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
			for (String name : type.properties.keySet()) {
				if (!given.containsKey(name) && name.startsWith(prefix)) {
					builder.suggest(name + "=");
				}
			}
			return builder.buildFuture();
		}

		private CompletableFuture<Suggestions> suggestVaguePropertyNameOrEnd(SuggestionsBuilder builder) {
			if (builder.getRemaining().isEmpty()) {
				builder.suggest("]");
			}
			return suggestVaguePropertyName(builder);
		}

		private CompletableFuture<Suggestions> suggestVaguePropertyName(SuggestionsBuilder builder) {
			String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
			List<String> names = new ArrayList<>();
			for (BlockIds.Type member : tag) {
				for (String name : member.properties.keySet()) {
					if (!given.containsKey(name) && name.startsWith(prefix) && !names.contains(name)) {
						names.add(name);
						builder.suggest(name + "=");
					}
				}
			}
			return builder.buildFuture();
		}

		private CompletableFuture<Suggestions> suggestVaguePropertyValue(SuggestionsBuilder builder, String key) {
			boolean moreProperties = false;
			List<String> values = new ArrayList<>();
			for (BlockIds.Type member : tag) {
				List<String> memberValues = member.properties.get(key);
				if (memberValues != null) {
					for (String value : memberValues) {
						if (!values.contains(value)) {
							values.add(value);
						}
					}
				}
				for (String name : member.properties.keySet()) {
					moreProperties |= !given.containsKey(name);
				}
			}
			for (String value : values) {
				builder.suggest(value);
			}
			if (moreProperties) {
				builder.suggest(",");
			}
			builder.suggest("]");
			return builder.buildFuture();
		}

		private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder builder) {
			if (builder.getRemaining().isEmpty()) {
				builder.suggest("=");
			}
			return builder.buildFuture();
		}

		private CompletableFuture<Suggestions> suggestNextPropertyOrEnd(SuggestionsBuilder builder) {
			if (builder.getRemaining().isEmpty()) {
				builder.suggest("]");
				if (given.size() < type.properties.size()) {
					builder.suggest(",");
				}
			}
			return builder.buildFuture();
		}
	}
}
