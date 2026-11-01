package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** The rest of the line as a message (spec §51.1); with permission its selectors become the names they find. */
public final class MessageArgument implements ArgumentType<String> {

	private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");

	private MessageArgument() {
	}

	public static MessageArgument message() {
		return new MessageArgument();
	}

	@Override
	public String parse(StringReader reader) {
		String text = reader.getRemaining();
		reader.setCursor(reader.getTotalLength());
		return text;
	}

	/** The message for the source: each selector (where the source may use them) replaced by the found names. */
	public static IChatComponent getMessage(CommandContext<ModernCommandSource> context, String name) throws CommandSyntaxException {
		String text = context.getArgument(name, String.class);
		ModernCommandSource source = context.getSource();
		IChatComponent message = new ChatComponentText("");
		if (!source.allowsSelectors()) {
			return message.appendText(text);
		}
		StringBuilder plain = new StringBuilder();
		int i = 0;
		while (i < text.length()) {
			if (text.charAt(i) == '@' && i + 1 < text.length() && "parsen".indexOf(text.charAt(i + 1)) >= 0) {
				StringReader reader = new StringReader(text);
				reader.setCursor(i);
				try {
					List<Entity> found = new EntitySelectorParser(reader, true).parse().findEntities(source);
					message.appendText(plain.toString());
					plain.setLength(0);
					for (int n = 0; n < found.size(); n++) {
						if (n > 0) {
							message.appendText(", ");
						}
						message.appendSibling(found.get(n).func_145748_c_());
					}
					i = reader.getCursor();
					continue;
				} catch (CommandSyntaxException e) {
					// not a selector after all: the text stays
				}
			}
			plain.append(text.charAt(i));
			i++;
		}
		return message.appendText(plain.toString());
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
