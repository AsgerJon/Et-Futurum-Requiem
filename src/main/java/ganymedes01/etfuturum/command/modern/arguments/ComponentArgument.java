package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.item.DecodeException;
import ganymedes01.etfuturum.command.modern.item.TextComponent;
import ganymedes01.etfuturum.command.modern.nbt.SnbtReader;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.Arrays;
import java.util.Collection;

/** A text component in SNBT (spec §51.2): 1.7.10's chat shows it as §-coded text. JSON reads as SNBT too. */
public final class ComponentArgument implements ArgumentType<IChatComponent> {

	private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "\"\"", "{text:\"hello world\"}", "[\"\"]");
	private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(
		reason -> ComponentMessage.translatable("argument.component.invalid", reason));

	private ComponentArgument() {
	}

	public static ComponentArgument textComponent() {
		return new ComponentArgument();
	}

	public static IChatComponent getComponent(CommandContext<?> context, String name) {
		return context.getArgument(name, IChatComponent.class);
	}

	@Override
	public IChatComponent parse(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();
		Object value = SnbtReader.read(reader);
		try {
			return new ChatComponentText(TextComponent.decode(value).toLegacy('f', false));
		} catch (DecodeException e) {
			reader.setCursor(start);
			throw INVALID.createWithContext(reader, e.getMessage());
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
