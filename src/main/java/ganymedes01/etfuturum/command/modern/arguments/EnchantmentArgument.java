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
import net.minecraft.enchantment.Enchantment;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** An enchantment by its modern id (spec §44.1); both sides know the enchantments, so the client suggests them itself. */
public final class EnchantmentArgument implements ArgumentType<Enchantment> {

	private static final Collection<String> EXAMPLES = Arrays.asList("unbreaking", "minecraft:sharpness");
	private static final Dynamic2CommandExceptionType UNKNOWN = new Dynamic2CommandExceptionType(
		(id, registry) -> ComponentMessage.translatable("argument.resource.not_found", id, registry));

	private EnchantmentArgument() {
	}

	public static EnchantmentArgument enchantment() {
		return new EnchantmentArgument();
	}

	public static Enchantment getEnchantment(CommandContext<?> context, String name) {
		return context.getArgument(name, Enchantment.class);
	}

	@Override
	public Enchantment parse(StringReader reader) throws CommandSyntaxException {
		String id = ResourceIds.read(reader);
		Enchantment enchantment = EnchantmentIds.byId(id);
		if (enchantment == null) {
			throw UNKNOWN.createWithContext(reader, id, "minecraft:enchantment");
		}
		return enchantment;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return Suggest.resources(EnchantmentIds.ids(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
