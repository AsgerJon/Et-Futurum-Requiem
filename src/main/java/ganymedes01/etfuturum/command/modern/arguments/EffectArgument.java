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
import net.minecraft.potion.Potion;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** An effect by its modern id (spec §41.3); both sides know the effects, so the client suggests them itself. */
public final class EffectArgument implements ArgumentType<Potion> {

	private static final Collection<String> EXAMPLES = Arrays.asList("speed", "minecraft:speed");
	private static final Dynamic2CommandExceptionType UNKNOWN = new Dynamic2CommandExceptionType(
		(id, registry) -> ComponentMessage.translatable("argument.resource.not_found", id, registry));

	private EffectArgument() {
	}

	public static EffectArgument effect() {
		return new EffectArgument();
	}

	public static Potion getEffect(CommandContext<?> context, String name) {
		return context.getArgument(name, Potion.class);
	}

	@Override
	public Potion parse(StringReader reader) throws CommandSyntaxException {
		String id = ResourceIds.read(reader);
		Potion potion = EffectIds.byId(id);
		if (potion == null) {
			throw UNKNOWN.createWithContext(reader, id, "minecraft:mob_effect");
		}
		return potion;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return Suggest.resources(EffectIds.ids(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
