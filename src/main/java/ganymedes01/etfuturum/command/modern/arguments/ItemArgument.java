package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.Suggest;
import ganymedes01.etfuturum.command.modern.item.DecodeException;
import ganymedes01.etfuturum.command.modern.item.ItemComponents;
import ganymedes01.etfuturum.command.modern.nbt.SnbtReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * An item with components, `id[name=value,...,!name]` (spec §45.2). Both sides know the items and components, so the
 * client parses and suggests itself; values are decoded while parsing, so a malformed one shows while typing.
 */
public final class ItemArgument implements ArgumentType<ItemInput> {

	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "stick[custom_data={foo:\"bar\"}]");
	private static final DynamicCommandExceptionType UNKNOWN_ITEM = dynamic("argument.item.id.invalid");
	private static final DynamicCommandExceptionType UNKNOWN_COMPONENT = dynamic("arguments.item.component.unknown");
	private static final Dynamic2CommandExceptionType MALFORMED_COMPONENT = new Dynamic2CommandExceptionType(
		(id, reason) -> ComponentMessage.translatable("arguments.item.component.malformed", id, reason));
	private static final SimpleCommandExceptionType EXPECTED_COMPONENT = new SimpleCommandExceptionType(ComponentMessage.translatable("arguments.item.component.expected"));
	private static final DynamicCommandExceptionType REPEATED_COMPONENT = dynamic("arguments.item.component.repeated");
	/** EFR's own, for modern's components not ported yet. */
	private static final DynamicCommandExceptionType NOT_YET = dynamic("arguments.etfuturum.item.component.notYet");
	private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;

	private ItemArgument() {
	}

	public static ItemArgument item() {
		return new ItemArgument();
	}

	public static ItemInput getItem(CommandContext<?> context, String name) {
		return context.getArgument(name, ItemInput.class);
	}

	private static DynamicCommandExceptionType dynamic(String key) {
		return new DynamicCommandExceptionType(value -> ComponentMessage.translatable(key, value));
	}

	@Override
	public ItemInput parse(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();
		try {
			return new Parser(reader).parse();
		} catch (CommandSyntaxException e) {
			reader.setCursor(start);
			throw e;
		}
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		StringReader reader = new StringReader(builder.getInput());
		reader.setCursor(builder.getStart());
		Parser parser = new Parser(reader);
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
		private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;
		private final List<ItemComponents.Decoded> components = new ArrayList<>();

		private Parser(StringReader reader) {
			this.reader = reader;
		}

		private ItemInput parse() throws CommandSyntaxException {
			suggestions = builder -> Suggest.resources(ItemIds.ids(), builder);
			int start = reader.getCursor();
			String id = ResourceIds.read(reader);
			ItemIds.Entry item = ItemIds.byId(id);
			if (item == null) {
				reader.setCursor(start);
				throw UNKNOWN_ITEM.createWithContext(reader, id);
			}
			suggestions = builder -> suggestIfNothingTyped(builder, "[");
			if (reader.canRead() && reader.peek() == '[') {
				suggestions = SUGGEST_NOTHING;
				readComponents();
			}
			return new ItemInput(item, id, components);
		}

		private void readComponents() throws CommandSyntaxException {
			reader.expect('[');
			suggestions = this::suggestAssignmentOrRemoval;
			Set<String> seen = new HashSet<>();
			while (reader.canRead() && reader.peek() != ']') {
				reader.skipWhitespace();
				if (reader.canRead() && reader.peek() == '!') {
					reader.skip();
					suggestions = builder -> Suggest.resources(ItemComponents.portedIds(), builder, "", "");
					String id = readComponentId();
					if (!seen.add(id)) {
						throw REPEATED_COMPONENT.create(id);
					}
					// 1.7.10 items have no default components to remove
					suggestions = SUGGEST_NOTHING;
					reader.skipWhitespace();
				} else {
					int nameStart = reader.getCursor();
					String id = readComponentId();
					if (!seen.add(id)) {
						throw REPEATED_COMPONENT.create(id);
					}
					if (!ItemComponents.isPorted(id)) {
						reader.setCursor(nameStart);
						throw NOT_YET.createWithContext(reader, id);
					}
					suggestions = builder -> suggestIfNothingTyped(builder, "=");
					reader.skipWhitespace();
					reader.expect('=');
					suggestions = SUGGEST_NOTHING;
					reader.skipWhitespace();
					readValue(id);
					reader.skipWhitespace();
				}
				suggestions = builder -> suggestIfNothingTyped(builder, ",", "]");
				if (!reader.canRead() || reader.peek() != ',') {
					break;
				}
				reader.skip();
				reader.skipWhitespace();
				suggestions = this::suggestAssignmentOrRemoval;
				if (!reader.canRead()) {
					throw EXPECTED_COMPONENT.createWithContext(reader);
				}
			}
			reader.expect(']');
			suggestions = SUGGEST_NOTHING;
		}

		private String readComponentId() throws CommandSyntaxException {
			if (!reader.canRead()) {
				throw EXPECTED_COMPONENT.createWithContext(reader);
			}
			int start = reader.getCursor();
			String id = ResourceIds.read(reader);
			if (!ItemComponents.exists(id)) {
				reader.setCursor(start);
				throw UNKNOWN_COMPONENT.createWithContext(reader, id);
			}
			return id;
		}

		private void readValue(String id) throws CommandSyntaxException {
			int start = reader.getCursor();
			Object value = SnbtReader.read(reader);
			try {
				components.add(ItemComponents.decode(id, value));
			} catch (DecodeException e) {
				reader.setCursor(start);
				throw MALFORMED_COMPONENT.createWithContext(reader, id, e.getMessage());
			}
		}

		private CompletableFuture<Suggestions> suggestAssignmentOrRemoval(SuggestionsBuilder builder) {
			builder.suggest("!");
			return Suggest.resources(ItemComponents.portedIds(), builder, "", "=");
		}

		private static CompletableFuture<Suggestions> suggestIfNothingTyped(SuggestionsBuilder builder, String... texts) {
			if (builder.getRemaining().isEmpty()) {
				for (String text : texts) {
					builder.suggest(text);
				}
			}
			return builder.buildFuture();
		}
	}
}
