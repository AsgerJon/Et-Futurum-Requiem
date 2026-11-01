package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;

/** Modern's resource ids in commands: `namespace:path`, the namespace `minecraft` when left out (spec §41.3). */
public final class ResourceIds {

	private static final SimpleCommandExceptionType INVALID_ID = new SimpleCommandExceptionType(ComponentMessage.translatable("argument.id.invalid"));

	private ResourceIds() {
	}

	/** Reads `a-z 0-9 _ . - : /`; the id with its namespace. A second `:`, or `/` in the namespace, is "Invalid ID". */
	public static String read(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();
		while (reader.canRead() && isAllowed(reader.peek())) {
			reader.skip();
		}
		String text = reader.getString().substring(start, reader.getCursor());
		int colon = text.indexOf(':');
		String namespace = colon > 0 ? text.substring(0, colon) : "minecraft";
		String path = colon >= 0 ? text.substring(colon + 1) : text;
		if (path.indexOf(':') >= 0 || namespace.indexOf('/') >= 0) {
			reader.setCursor(start);
			throw INVALID_ID.createWithContext(reader);
		}
		return namespace + ":" + path;
	}

	private static boolean isAllowed(char c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
	}
}
