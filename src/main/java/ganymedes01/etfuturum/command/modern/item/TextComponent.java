package ganymedes01.etfuturum.command.modern.item;

import ganymedes01.etfuturum.command.modern.nbt.Snbt;
import net.minecraft.util.StatCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A modern text component read from its SNBT form (spec §45.4), kept until the item is made and then written as
 * 1.7.10's §-coded text.
 */
public final class TextComponent {

	/** Modern's named colours in 1.7.10's code order, 0 to f. */
	private static final String[] COLOR_NAMES = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
		"dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};
	private static final int[] COLOR_VALUES = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
		0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
	private static final String CODES = "0123456789abcdef";
	private static final String[] FLAGS = {"bold", "italic", "underlined", "strikethrough", "obfuscated"};
	private static final char[] FLAG_CODES = {'l', 'o', 'n', 'm', 'k'};
	private static final int ITALIC = 1;

	/** Plain text, or null for a translation. */
	private final String text;
	private final String translate;
	private final String fallback;
	private final List<TextComponent> arguments;
	/** The colour's code character, or 0 to inherit. */
	private final char color;
	/** bold, italic, underlined, strikethrough, obfuscated; null to inherit. */
	private final Boolean[] flags;
	private final List<TextComponent> children;

	private TextComponent(String text, String translate, String fallback, List<TextComponent> arguments, char color, Boolean[] flags,
						  List<TextComponent> children) {
		this.text = text;
		this.translate = translate;
		this.fallback = fallback;
		this.arguments = arguments;
		this.color = color;
		this.flags = flags;
		this.children = children;
	}

	private static TextComponent plain(String text) {
		return new TextComponent(text, null, null, Collections.emptyList(), (char) 0, new Boolean[FLAGS.length], new ArrayList<>());
	}

	/** A string is plain text; a list its first element with the rest as children; a compound content and style. */
	public static TextComponent decode(Object value) throws DecodeException {
		if (value instanceof String s) {
			return plain(s);
		}
		if (value instanceof List<?> list) {
			if (list.isEmpty()) {
				throw new DecodeException("List must have contents");
			}
			TextComponent first = decode(list.get(0));
			List<TextComponent> children = new ArrayList<>(first.children);
			for (int i = 1; i < list.size(); i++) {
				children.add(decode(list.get(i)));
			}
			return new TextComponent(first.text, first.translate, first.fallback, first.arguments, first.color, first.flags, children);
		}
		if (!(value instanceof Map<?, ?> map)) {
			throw new DecodeException("Not a text component: " + Snbt.toText(value));
		}
		String type = map.get("type") instanceof String t ? t : contentType(map);
		String text = null;
		String translate = null;
		String fallback = null;
		List<TextComponent> arguments = Collections.emptyList();
		switch (type) {
			case "text" -> text = string(map, "text");
			case "translatable" -> {
				translate = string(map, "translate");
				if (map.containsKey("fallback")) {
					fallback = string(map, "fallback");
				}
				if (map.containsKey("with")) {
					arguments = new ArrayList<>();
					if (!(map.get("with") instanceof List<?> with)) {
						throw new DecodeException("Not a list: " + Snbt.toText(map.get("with")));
					}
					for (Object argument : with) {
						arguments.add(argument instanceof Number number ? plain(String.valueOf(number)) : decode(argument));
					}
				}
			}
			case "keybind" -> text = StatCollector.translateToLocal(string(map, "keybind"));
			case "score", "selector", "nbt", "object" -> throw new DecodeException("'" + type + "' text has nothing to show on an item in 1.7.10");
			default -> throw new DecodeException("Unknown text type: " + type);
		}

		char color = 0;
		if (map.containsKey("color")) {
			color = colorCode(map.get("color"));
		}
		Boolean[] flags = new Boolean[FLAGS.length];
		for (int i = 0; i < FLAGS.length; i++) {
			if (map.containsKey(FLAGS[i])) {
				if (!(map.get(FLAGS[i]) instanceof Number number)) {
					throw new DecodeException("Not a boolean: " + Snbt.toText(map.get(FLAGS[i])));
				}
				flags[i] = number.doubleValue() != 0.0D;
			}
		}
		List<TextComponent> children = new ArrayList<>();
		if (map.containsKey("extra")) {
			if (!(map.get("extra") instanceof List<?> extra)) {
				throw new DecodeException("Not a list: " + Snbt.toText(map.get("extra")));
			}
			if (extra.isEmpty()) {
				throw new DecodeException("List must have contents");
			}
			for (Object child : extra) {
				children.add(decode(child));
			}
		}
		return new TextComponent(text, translate, fallback, arguments, color, flags, children);
	}

	/** Without a `type`, the content is the first of modern's content keys present. */
	private static String contentType(Map<?, ?> map) throws DecodeException {
		if (map.containsKey("text")) {
			return "text";
		}
		if (map.containsKey("translate")) {
			return "translatable";
		}
		for (String type : new String[]{"keybind", "score", "selector", "nbt", "object"}) {
			if (map.containsKey(type)) {
				return type;
			}
		}
		throw new DecodeException("No text, translate or other content in " + Snbt.toText(map));
	}

	private static String string(Map<?, ?> map, String key) throws DecodeException {
		Object value = map.get(key);
		if (!(value instanceof String s)) {
			throw new DecodeException("Not a string: " + Snbt.toText(value));
		}
		return s;
	}

	/** A colour name, or `#RRGGBB` as the nearest of the 16 colours 1.7.10 has. */
	private static char colorCode(Object value) throws DecodeException {
		if (!(value instanceof String name)) {
			throw new DecodeException("Not a string: " + Snbt.toText(value));
		}
		if (name.startsWith("#")) {
			int rgb;
			try {
				rgb = Integer.parseInt(name.substring(1), 16);
			} catch (NumberFormatException e) {
				throw new DecodeException("Invalid color name: " + name);
			}
			if (rgb < 0 || rgb > 0xFFFFFF) {
				throw new DecodeException("Color value out of range: " + name);
			}
			int best = 0;
			long bestDistance = Long.MAX_VALUE;
			for (int i = 0; i < COLOR_VALUES.length; i++) {
				long dr = (rgb >> 16 & 255) - (COLOR_VALUES[i] >> 16 & 255);
				long dg = (rgb >> 8 & 255) - (COLOR_VALUES[i] >> 8 & 255);
				long db = (rgb & 255) - (COLOR_VALUES[i] & 255);
				long distance = dr * dr + dg * dg + db * db;
				if (distance < bestDistance) {
					bestDistance = distance;
					best = i;
				}
			}
			return CODES.charAt(best);
		}
		for (int i = 0; i < COLOR_NAMES.length; i++) {
			if (COLOR_NAMES[i].equals(name)) {
				return CODES.charAt(i);
			}
		}
		throw new DecodeException("Invalid color name: " + name);
	}

	/**
	 * 1.7.10's text for this component. Text in the default style ({@code baseColor}, italic or not) gets no codes;
	 * other text is preceded by its colour code and then its format codes, since a colour code clears formats.
	 */
	public String toLegacy(char baseColor, boolean baseItalic) {
		Boolean[] base = new Boolean[FLAGS.length];
		for (int i = 0; i < base.length; i++) {
			base[i] = i == ITALIC && baseItalic;
		}
		StringBuilder out = new StringBuilder();
		String[] current = {String.valueOf(baseColor) + styleKey(base)};
		write(out, baseColor, base, current);
		return out.toString();
	}

	private void write(StringBuilder out, char inheritedColor, Boolean[] inheritedFlags, String[] current) {
		char effectiveColor = color != 0 ? color : inheritedColor;
		Boolean[] effectiveFlags = new Boolean[FLAGS.length];
		for (int i = 0; i < FLAGS.length; i++) {
			effectiveFlags[i] = flags[i] != null ? flags[i] : inheritedFlags[i];
		}
		if (text != null) {
			piece(out, text, effectiveColor, effectiveFlags, current);
		} else {
			writeTranslation(out, effectiveColor, effectiveFlags, current);
		}
		for (TextComponent child : children) {
			child.write(out, effectiveColor, effectiveFlags, current);
		}
	}

	/** The server's text for the key (else the fallback, else the key), with `%s`, `%1$s` and `%%` filled in. */
	private void writeTranslation(StringBuilder out, char color, Boolean[] flags, String[] current) {
		String format = StatCollector.canTranslate(translate) ? StatCollector.translateToLocal(translate) : fallback != null ? fallback : translate;
		int next = 0;
		StringBuilder literal = new StringBuilder();
		for (int i = 0; i < format.length(); i++) {
			char c = format.charAt(i);
			if (c != '%' || i + 1 >= format.length()) {
				literal.append(c);
				continue;
			}
			int end = i + 1;
			while (end < format.length() && Character.isDigit(format.charAt(end))) {
				end++;
			}
			int index;
			if (end > i + 1 && end + 1 < format.length() && format.charAt(end) == '$' && format.charAt(end + 1) == 's') {
				index = Integer.parseInt(format.substring(i + 1, end)) - 1;
				end += 2;
			} else if (format.charAt(i + 1) == 's') {
				index = next++;
				end = i + 2;
			} else if (format.charAt(i + 1) == '%') {
				literal.append('%');
				i++;
				continue;
			} else {
				literal.append(c);
				continue;
			}
			piece(out, literal.toString(), color, flags, current);
			literal.setLength(0);
			if (index >= 0 && index < arguments.size()) {
				arguments.get(index).write(out, color, flags, current);
			}
			i = end - 1;
		}
		piece(out, literal.toString(), color, flags, current);
	}

	private static void piece(StringBuilder out, String text, char color, Boolean[] flags, String[] current) {
		if (text.isEmpty()) {
			return;
		}
		String style = color + styleKey(flags);
		if (!style.equals(current[0])) {
			out.append('§').append(color);
			for (int i = 0; i < FLAGS.length; i++) {
				if (flags[i]) {
					out.append('§').append(FLAG_CODES[i]);
				}
			}
			current[0] = style;
		}
		out.append(text);
	}

	private static String styleKey(Boolean[] flags) {
		StringBuilder key = new StringBuilder();
		for (Boolean flag : flags) {
			key.append(flag ? '1' : '0');
		}
		return key.toString();
	}
}
