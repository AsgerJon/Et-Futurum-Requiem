package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.Suggest;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * An entity type by its modern id (spec §47.1); suggests the summonable ones. Whether it can be summoned is checked
 * when the command runs, as in modern.
 */
public final class EntityTypeArgument implements ArgumentType<String> {

	private static final Collection<String> EXAMPLES = Arrays.asList("pig", "minecraft:wither_skeleton");
	private static final Dynamic2CommandExceptionType UNKNOWN = new Dynamic2CommandExceptionType(
		(id, registry) -> ComponentMessage.translatable("argument.resource.not_found", id, registry));

	private EntityTypeArgument() {
	}

	public static EntityTypeArgument summonable() {
		return new EntityTypeArgument();
	}

	public static String getEntityType(CommandContext<?> context, String name) {
		return context.getArgument(name, String.class);
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		String id = ResourceIds.read(reader);
		if (!EntityTypeIds.exists(id)) {
			throw UNKNOWN.createWithContext(reader, id, "minecraft:entity_type");
		}
		return id;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return Suggest.resources(EntityTypeIds.summonableIds(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
