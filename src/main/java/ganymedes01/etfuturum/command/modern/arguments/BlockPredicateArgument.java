package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** A block test (spec §49.1): a block as the block argument reads it, or `#tag` with properties by name and data. */
public final class BlockPredicateArgument implements ArgumentType<BlockTest> {

	private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "#stone", "#stone[foo=bar]{baz=nbt}");

	private BlockPredicateArgument() {
	}

	public static BlockPredicateArgument blockPredicate() {
		return new BlockPredicateArgument();
	}

	public static BlockTest getBlockPredicate(CommandContext<?> context, String name) {
		return context.getArgument(name, BlockTest.class);
	}

	@Override
	public BlockTest parse(StringReader reader) throws CommandSyntaxException {
		return BlockStateArgument.parse(reader, true);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return BlockStateArgument.suggest(builder, true);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
