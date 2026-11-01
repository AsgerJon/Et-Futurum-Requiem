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
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/** An item test (spec §51.2): an item id with its variant, or `*` for any item. Tags and component tests aren't ported. */
public final class ItemPredicateArgument implements ArgumentType<Predicate<ItemStack>> {

	private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "*");
	private static final DynamicCommandExceptionType UNKNOWN_ITEM = new DynamicCommandExceptionType(
		id -> ComponentMessage.translatable("argument.item.id.invalid", id));
	private static final DynamicCommandExceptionType UNKNOWN_TAG = new DynamicCommandExceptionType(
		id -> ComponentMessage.translatable("arguments.item.tag.unknown", id));

	private ItemPredicateArgument() {
	}

	public static ItemPredicateArgument itemPredicate() {
		return new ItemPredicateArgument();
	}

	@SuppressWarnings("unchecked")
	public static Predicate<ItemStack> getItemPredicate(CommandContext<?> context, String name) {
		return context.getArgument(name, Predicate.class);
	}

	@Override
	public Predicate<ItemStack> parse(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();
		if (reader.canRead() && reader.peek() == '*') {
			reader.skip();
			return stack -> true;
		}
		if (reader.canRead() && reader.peek() == '#') {
			reader.skip();
			String tag = ResourceIds.read(reader);
			reader.setCursor(start);
			throw UNKNOWN_TAG.createWithContext(reader, tag);
		}
		String id = ResourceIds.read(reader);
		ItemIds.Entry entry = ItemIds.byId(id);
		if (entry == null) {
			reader.setCursor(start);
			throw UNKNOWN_ITEM.createWithContext(reader, id);
		}
		// an item whose damage is its variant matches that variant; one whose damage is wear matches any wear
		return stack -> stack.getItem() == entry.item() && (!entry.item().getHasSubtypes() || stack.getItemDamage() == entry.meta());
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		List<String> ids = new ArrayList<>(ItemIds.ids());
		ids.add("*");
		return Suggest.resources(ids, builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
