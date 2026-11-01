package ganymedes01.etfuturum.command.modern.nbt;

import com.google.common.primitives.UnsignedBytes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Reads modern's text form of NBT (spec §45.1) into plain values: Byte (also booleans), Short, Integer, Long, Float,
 * Double, String, byte[], int[], long[], List and Map. Spaces may come before any token. Like modern's parser it
 * tries the alternatives in order and reports the error that got furthest.
 */
public final class SnbtReader {

	private static final DynamicCommandExceptionType NUMBER_PARSE_FAILURE = dynamic("snbt.parser.number_parse_failure");
	private static final DynamicCommandExceptionType EXPECTED_HEX_ESCAPE = dynamic("snbt.parser.expected_hex_escape");
	private static final DynamicCommandExceptionType INVALID_CODEPOINT = dynamic("snbt.parser.invalid_codepoint");
	private static final DynamicCommandExceptionType NO_SUCH_OPERATION = dynamic("snbt.parser.no_such_operation");
	private static final DynamicCommandExceptionType INVALID_ESCAPE = dynamic("parsing.quote.escape");
	private static final SimpleCommandExceptionType EXPECTED_NON_NEGATIVE_NUMBER = simple("snbt.parser.expected_non_negative_number");
	private static final SimpleCommandExceptionType EXPECTED_NUMBER_OR_BOOLEAN = simple("snbt.parser.expected_number_or_boolean");
	private static final SimpleCommandExceptionType EXPECTED_STRING_UUID = simple("snbt.parser.expected_string_uuid");
	private static final SimpleCommandExceptionType INVALID_CHARACTER_NAME = simple("snbt.parser.invalid_character_name");
	private static final SimpleCommandExceptionType INVALID_ARRAY_ELEMENT_TYPE = simple("snbt.parser.invalid_array_element_type");
	private static final SimpleCommandExceptionType INVALID_UNQUOTED_START = simple("snbt.parser.invalid_unquoted_start");
	private static final SimpleCommandExceptionType EXPECTED_UNQUOTED_STRING = simple("snbt.parser.expected_unquoted_string");
	private static final SimpleCommandExceptionType EXPECTED_BINARY_NUMERAL = simple("snbt.parser.expected_binary_numeral");
	private static final SimpleCommandExceptionType UNDERSCORE_NOT_ALLOWED = simple("snbt.parser.underscore_not_allowed");
	private static final SimpleCommandExceptionType EXPECTED_DECIMAL_NUMERAL = simple("snbt.parser.expected_decimal_numeral");
	private static final SimpleCommandExceptionType EXPECTED_HEX_NUMERAL = simple("snbt.parser.expected_hex_numeral");
	private static final SimpleCommandExceptionType EMPTY_KEY = simple("snbt.parser.empty_key");
	private static final SimpleCommandExceptionType LEADING_ZERO_NOT_ALLOWED = simple("snbt.parser.leading_zero_not_allowed");
	private static final SimpleCommandExceptionType INFINITY_NOT_ALLOWED = simple("snbt.parser.infinity_not_allowed");

	private enum Type {
		BYTE, SHORT, INT, LONG
	}

	private enum Base {
		BINARY(2), DECIMAL(10), HEX(16);

		private final int radix;

		Base(int radix) {
			this.radix = radix;
		}
	}

	/** A whole number as written, made into a value once its type is known. */
	private static final class IntegerLiteral {
		private final boolean negative;
		private final Base base;
		private final String digits;
		private final Boolean signed;
		private final Type type;
		private final int end;

		private IntegerLiteral(boolean negative, Base base, String digits, Boolean signed, Type type, int end) {
			this.negative = negative;
			this.base = base;
			this.digits = digits;
			this.signed = signed;
			this.type = type;
			this.end = end;
		}
	}

	private final StringReader reader;
	private int errorCursor = -1;
	private CommandSyntaxException error;

	private SnbtReader(StringReader reader) {
		this.reader = reader;
	}

	private static SimpleCommandExceptionType simple(String key) {
		return new SimpleCommandExceptionType(ComponentMessage.translatable(key));
	}

	private static DynamicCommandExceptionType dynamic(String key) {
		return new DynamicCommandExceptionType(value -> ComponentMessage.translatable(key, value));
	}

	/** One value from the reader's cursor; the reader is left just after it. */
	public static Object read(StringReader reader) throws CommandSyntaxException {
		SnbtReader parser = new SnbtReader(reader);
		Object value = parser.value();
		if (value == null) {
			throw parser.error;
		}
		return value;
	}

	// errors: the one furthest into the input wins, the first one stored there among equals

	private StringReader at(int cursor) {
		StringReader copy = new StringReader(reader.getString());
		copy.setCursor(cursor);
		return copy;
	}

	private void fail(int cursor, SimpleCommandExceptionType type) {
		fail(cursor, type.createWithContext(at(cursor)));
	}

	private void fail(int cursor, DynamicCommandExceptionType type, Object value) {
		fail(cursor, type.createWithContext(at(cursor), value));
	}

	private void fail(int cursor, CommandSyntaxException exception) {
		if (cursor > errorCursor) {
			errorCursor = cursor;
			error = exception;
		}
	}

	private boolean character(char c) {
		reader.skipWhitespace();
		int start = reader.getCursor();
		if (reader.canRead() && reader.peek() == c) {
			reader.skip();
			return true;
		}
		fail(start, CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().createWithContext(at(start), String.valueOf(c)));
		return false;
	}

	/** One of two letters, such as a suffix in either case; nothing is reported, as these are optional. */
	private boolean letter(char lower, char upper) {
		int start = reader.getCursor();
		reader.skipWhitespace();
		if (reader.canRead() && (reader.peek() == lower || reader.peek() == upper)) {
			reader.skip();
			return true;
		}
		reader.setCursor(start);
		return false;
	}

	private static boolean canStartNumber(char c) {
		return c >= '0' && c <= '9' || c == '+' || c == '-' || c == '.';
	}

	// values

	private Object value() {
		reader.skipWhitespace();
		int start = reader.getCursor();
		if (reader.canRead() && canStartNumber(reader.peek())) {
			Object number = floatLiteral();
			if (number == null) {
				reader.setCursor(start);
				IntegerLiteral integer = integerLiteral();
				number = integer == null ? null : make(integer, integer.type != null ? integer.type : Type.INT);
			}
			if (number != null) {
				return number;
			}
			reader.setCursor(start);
		} else if (reader.canRead() && (reader.peek() == '"' || reader.peek() == '\'')) {
			return quotedString();
		} else if (reader.canRead() && reader.peek() == '{') {
			return compound();
		} else if (reader.canRead() && reader.peek() == '[') {
			return listOrArray();
		}
		return unquotedOrBuiltin();
	}

	/** -1 for a minus sign, 1 for a plus or none. */
	private int sign() {
		int start = reader.getCursor();
		reader.skipWhitespace();
		if (reader.canRead() && (reader.peek() == '+' || reader.peek() == '-')) {
			return reader.read() == '-' ? -1 : 1;
		}
		reader.setCursor(start);
		return 0;
	}

	/** A run of digits (and underscores, not at either end); null if none. */
	private String numeral(Base base, SimpleCommandExceptionType missing) {
		reader.skipWhitespace();
		int start = reader.getCursor();
		String text = reader.getString();
		int end = start;
		while (end < text.length() && isDigit(text.charAt(end), base)) {
			end++;
		}
		if (end == start) {
			fail(start, missing);
			return null;
		}
		if (text.charAt(start) == '_' || text.charAt(end - 1) == '_') {
			fail(start, UNDERSCORE_NOT_ALLOWED);
			return null;
		}
		reader.setCursor(end);
		return text.substring(start, end);
	}

	private static boolean isDigit(char c, Base base) {
		if (c == '_') {
			return true;
		}
		return switch (base) {
			case BINARY -> c == '0' || c == '1';
			case DECIMAL -> c >= '0' && c <= '9';
			case HEX -> c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
		};
	}

	/**
	 * A decimal fraction (§45.1): digits `.` [digits] [exponent] [suffix]; `.` digits [exponent] [suffix]; digits
	 * exponent [suffix]; digits suffix. Null for anything else, a plain whole number included.
	 */
	private Object floatLiteral() {
		int start = reader.getCursor();
		int sign = sign();
		int afterSign = reader.getCursor();
		String whole = numeral(Base.DECIMAL, EXPECTED_DECIMAL_NUMERAL);
		String fraction = null;
		String exponent;
		Character suffix;
		if (whole != null) {
			if (letter('.', '.')) {
				int beforeFraction = reader.getCursor();
				fraction = numeral(Base.DECIMAL, EXPECTED_DECIMAL_NUMERAL);
				if (fraction == null) {
					reader.setCursor(beforeFraction);
				}
				exponent = exponent();
				suffix = floatSuffix();
			} else {
				exponent = exponent();
				suffix = floatSuffix();
				if (exponent == null && suffix == null) {
					reader.setCursor(start);
					return null;
				}
			}
		} else {
			reader.setCursor(afterSign);
			if (!letter('.', '.')) {
				reader.setCursor(start);
				return null;
			}
			// once the point is read nothing else is tried
			fraction = numeral(Base.DECIMAL, EXPECTED_DECIMAL_NUMERAL);
			if (fraction == null) {
				reader.setCursor(start);
				return null;
			}
			exponent = exponent();
			suffix = floatSuffix();
		}

		StringBuilder number = new StringBuilder(sign < 0 ? "-" : "");
		if (whole != null) {
			number.append(whole.replace("_", ""));
		}
		if (fraction != null) {
			number.append('.').append(fraction.replace("_", ""));
		}
		if (exponent != null) {
			number.append('e').append(exponent);
		}
		try {
			if (suffix != null && suffix == 'f') {
				float value = Float.parseFloat(number.toString());
				if (!Float.isInfinite(value) && !Float.isNaN(value)) {
					return value;
				}
			} else {
				double value = Double.parseDouble(number.toString());
				if (!Double.isInfinite(value) && !Double.isNaN(value)) {
					return value;
				}
			}
			fail(reader.getCursor(), INFINITY_NOT_ALLOWED);
		} catch (NumberFormatException e) {
			fail(reader.getCursor(), NUMBER_PARSE_FAILURE, e.getMessage());
		}
		reader.setCursor(start);
		return null;
	}

	/** `e` with an optional sign and digits, as text for Java's parser; null if there is none. */
	private String exponent() {
		int start = reader.getCursor();
		if (!letter('e', 'E')) {
			return null;
		}
		int sign = sign();
		String digits = numeral(Base.DECIMAL, EXPECTED_DECIMAL_NUMERAL);
		if (digits == null) {
			reader.setCursor(start);
			return null;
		}
		return (sign < 0 ? "-" : "") + digits.replace("_", "");
	}

	/** 'f' or 'd', or null. */
	private Character floatSuffix() {
		if (letter('f', 'F')) {
			return 'f';
		}
		return letter('d', 'D') ? 'd' : null;
	}

	private IntegerLiteral integerLiteral() {
		int start = reader.getCursor();
		int sign = sign();
		Base base = Base.DECIMAL;
		String digits;
		reader.skipWhitespace();
		if (reader.canRead() && reader.peek() == '0') {
			reader.skip();
			int afterZero = reader.getCursor();
			if (letter('x', 'X')) {
				base = Base.HEX;
				digits = numeral(Base.HEX, EXPECTED_HEX_NUMERAL);
				if (digits == null) {
					reader.setCursor(start);
					return null;
				}
			} else {
				digits = null;
				if (letter('b', 'B')) {
					digits = numeral(Base.BINARY, EXPECTED_BINARY_NUMERAL);
					if (digits != null) {
						base = Base.BINARY;
					} else {
						reader.setCursor(afterZero);
					}
				}
				if (digits == null) {
					int beforeDecimal = reader.getCursor();
					if (numeral(Base.DECIMAL, EXPECTED_DECIMAL_NUMERAL) != null) {
						fail(reader.getCursor(), LEADING_ZERO_NOT_ALLOWED);
						reader.setCursor(start);
						return null;
					}
					reader.setCursor(beforeDecimal);
					digits = "0";
				}
			}
		} else {
			digits = numeral(Base.DECIMAL, EXPECTED_DECIMAL_NUMERAL);
			if (digits == null) {
				reader.setCursor(start);
				return null;
			}
		}

		// suffix: [u|s](b|s|i|l), or b, s, i, l alone
		Boolean signed = null;
		Type type = null;
		int beforeSuffix = reader.getCursor();
		if (letter('u', 'U') || letter('s', 'S')) {
			char prefix = Character.toLowerCase(reader.getString().charAt(reader.getCursor() - 1));
			type = integerType();
			if (type != null) {
				signed = prefix == 's';
			} else {
				reader.setCursor(beforeSuffix);
			}
		}
		if (type == null) {
			type = integerType();
		}
		return new IntegerLiteral(sign < 0, base, digits, signed, type, reader.getCursor());
	}

	private Type integerType() {
		if (letter('b', 'B')) {
			return Type.BYTE;
		}
		if (letter('s', 'S')) {
			return Type.SHORT;
		}
		if (letter('i', 'I')) {
			return Type.INT;
		}
		return letter('l', 'L') ? Type.LONG : null;
	}

	/** The value of a whole number as the given type; decimal is signed unless told, hex and binary unsigned. */
	private Object make(IntegerLiteral literal, Type type) {
		boolean signed = literal.signed != null ? literal.signed : literal.base == Base.DECIMAL;
		if (!signed && literal.negative) {
			fail(literal.end, EXPECTED_NON_NEGATIVE_NUMBER);
			return null;
		}
		String digits = (literal.negative ? "-" : "") + literal.digits.replace("_", "");
		int radix = literal.base.radix;
		try {
			if (signed) {
				return switch (type) {
					case BYTE -> Byte.parseByte(digits, radix);
					case SHORT -> Short.parseShort(digits, radix);
					case INT -> Integer.parseInt(digits, radix);
					case LONG -> Long.parseLong(digits, radix);
				};
			}
			return switch (type) {
				case BYTE -> UnsignedBytes.parseUnsignedByte(digits, radix);
				case SHORT -> {
					int value = Integer.parseInt(digits, radix);
					if (value >> 16 != 0) {
						throw new NumberFormatException("out of range: " + value);
					}
					yield (short) value;
				}
				case INT -> Integer.parseUnsignedInt(digits, radix);
				case LONG -> Long.parseUnsignedLong(digits, radix);
			};
		} catch (NumberFormatException e) {
			fail(literal.end, NUMBER_PARSE_FAILURE, e.getMessage());
			return null;
		}
	}

	private String quotedString() {
		reader.skipWhitespace();
		int start = reader.getCursor();
		char quote = reader.read();
		StringBuilder text = new StringBuilder();
		while (reader.canRead() && reader.peek() != quote) {
			char c = reader.read();
			if (c != '\\') {
				text.append(c);
				continue;
			}
			int escapeAt = reader.getCursor();
			if (!reader.canRead()) {
				break;
			}
			char escape = reader.read();
			switch (escape) {
				case 'b' -> text.append('\b');
				case 's' -> text.append(' ');
				case 't' -> text.append('\t');
				case 'n' -> text.append('\n');
				case 'f' -> text.append('\f');
				case 'r' -> text.append('\r');
				case '\\', '\'', '"' -> text.append(escape);
				case 'x', 'u', 'U' -> {
					int length = escape == 'x' ? 2 : escape == 'u' ? 4 : 8;
					String hex = hexDigits(length);
					if (hex == null) {
						reader.setCursor(start);
						return null;
					}
					long codePoint = Long.parseLong(hex, 16);
					if (codePoint > Character.MAX_CODE_POINT) {
						fail(reader.getCursor(), INVALID_CODEPOINT, String.format(Locale.ROOT, "U+%08X", codePoint));
						reader.setCursor(start);
						return null;
					}
					text.appendCodePoint((int) codePoint);
				}
				case 'N' -> {
					Integer codePoint = namedCharacter();
					if (codePoint == null) {
						reader.setCursor(start);
						return null;
					}
					text.appendCodePoint(codePoint);
				}
				default -> {
					fail(escapeAt, INVALID_ESCAPE, String.valueOf(escape));
					reader.setCursor(start);
					return null;
				}
			}
		}
		if (!reader.canRead()) {
			fail(reader.getCursor(), CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().createWithContext(at(reader.getCursor()), String.valueOf(quote)));
			reader.setCursor(start);
			return null;
		}
		reader.skip();
		return text.toString();
	}

	private String hexDigits(int length) {
		int start = reader.getCursor();
		String text = reader.getString();
		int end = start;
		while (end < text.length() && end - start < length && isDigit(text.charAt(end), Base.HEX) && text.charAt(end) != '_') {
			end++;
		}
		if (end - start < length) {
			fail(start, EXPECTED_HEX_ESCAPE, String.valueOf(length));
			return null;
		}
		reader.setCursor(end);
		return text.substring(start, end);
	}

	/** `\N{name}`: a character by its Unicode name, any case. */
	private Integer namedCharacter() {
		if (!reader.canRead() || reader.peek() != '{') {
			fail(reader.getCursor(), CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().createWithContext(at(reader.getCursor()), "{"));
			return null;
		}
		reader.skip();
		int start = reader.getCursor();
		while (reader.canRead() && isNameChar(reader.peek())) {
			reader.skip();
		}
		String name = reader.getString().substring(start, reader.getCursor());
		if (name.isEmpty()) {
			fail(start, INVALID_CHARACTER_NAME);
			return null;
		}
		Integer codePoint = codePointOf(name);
		if (codePoint == null) {
			fail(reader.getCursor(), INVALID_CHARACTER_NAME);
			return null;
		}
		if (!reader.canRead() || reader.peek() != '}') {
			fail(reader.getCursor(), CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().createWithContext(at(reader.getCursor()), "}"));
			return null;
		}
		reader.skip();
		return codePoint;
	}

	/** The character with this Unicode name, any case; Java 8 has no lookup by name, so every one is checked. */
	private static Integer codePointOf(String name) {
		for (int codePoint = 0; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
			String candidate = Character.getName(codePoint);
			if (candidate != null && candidate.equalsIgnoreCase(name)) {
				return codePoint;
			}
		}
		return null;
	}

	private static boolean isNameChar(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == ' ';
	}

	/** A bare word: true, false, an operation such as bool(...) or uuid(...), or a string. */
	private Object unquotedOrBuiltin() {
		reader.skipWhitespace();
		int start = reader.getCursor();
		String word = reader.readUnquotedString();
		if (word.isEmpty()) {
			fail(start, EXPECTED_UNQUOTED_STRING);
			return null;
		}
		int afterWord = reader.getCursor();
		List<Object> arguments = null;
		reader.skipWhitespace();
		if (reader.canRead() && reader.peek() == '(') {
			reader.skip();
			arguments = new ArrayList<>();
			if (!elements(arguments, ')')) {
				reader.setCursor(afterWord);
				arguments = null;
			}
		} else {
			reader.setCursor(afterWord);
		}
		if (canStartNumber(word.charAt(0))) {
			fail(reader.getCursor(), INVALID_UNQUOTED_START);
			reader.setCursor(start);
			return null;
		}
		if (arguments != null) {
			Object result = operation(word, arguments);
			if (result == null) {
				reader.setCursor(start);
			}
			return result;
		}
		if (word.equalsIgnoreCase("true")) {
			return (byte) 1;
		}
		return word.equalsIgnoreCase("false") ? (byte) 0 : word;
	}

	private Object operation(String name, List<Object> arguments) {
		int at = reader.getCursor();
		if (name.equals("bool") && arguments.size() == 1) {
			if (arguments.get(0) instanceof Number number) {
				return number.doubleValue() != 0.0D ? (byte) 1 : (byte) 0;
			}
			fail(at, EXPECTED_NUMBER_OR_BOOLEAN);
			return null;
		}
		if (name.equals("uuid") && arguments.size() == 1) {
			try {
				if (arguments.get(0) instanceof String text) {
					UUID uuid = UUID.fromString(text);
					long most = uuid.getMostSignificantBits();
					long least = uuid.getLeastSignificantBits();
					return new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
				}
			} catch (IllegalArgumentException ignored) {
				// reported below
			}
			fail(at, EXPECTED_STRING_UUID);
			return null;
		}
		fail(at, NO_SUCH_OPERATION, name + "/" + arguments.size());
		return null;
	}

	/** Values separated by commas, a trailing comma allowed, up to the closing character. */
	private boolean elements(List<Object> into, char close) {
		while (true) {
			int beforeElement = reader.getCursor();
			Object element = value();
			if (element == null) {
				reader.setCursor(beforeElement);
				break;
			}
			into.add(element);
			int beforeComma = reader.getCursor();
			if (!character(',')) {
				reader.setCursor(beforeComma);
				break;
			}
		}
		return character(close);
	}

	private Map<String, Object> compound() {
		int start = reader.getCursor();
		reader.skipWhitespace();
		reader.skip();
		Map<String, Object> entries = new LinkedHashMap<>();
		while (true) {
			int beforeEntry = reader.getCursor();
			if (!entry(entries)) {
				reader.setCursor(beforeEntry);
				break;
			}
			int beforeComma = reader.getCursor();
			if (!character(',')) {
				reader.setCursor(beforeComma);
				break;
			}
		}
		if (!character('}')) {
			reader.setCursor(start);
			return null;
		}
		return entries;
	}

	private boolean entry(Map<String, Object> into) {
		reader.skipWhitespace();
		int keyStart = reader.getCursor();
		String key;
		if (reader.canRead() && (reader.peek() == '"' || reader.peek() == '\'')) {
			key = quotedString();
			if (key == null) {
				return false;
			}
		} else {
			key = reader.readUnquotedString();
			if (key.isEmpty()) {
				fail(keyStart, EXPECTED_UNQUOTED_STRING);
				return false;
			}
		}
		if (!character(':')) {
			return false;
		}
		Object value = value();
		if (value == null) {
			return false;
		}
		if (key.isEmpty()) {
			fail(reader.getCursor(), EMPTY_KEY);
			return false;
		}
		into.put(key, value);
		return true;
	}

	private Object listOrArray() {
		int start = reader.getCursor();
		reader.skipWhitespace();
		reader.skip();
		int afterBracket = reader.getCursor();
		reader.skipWhitespace();
		if (reader.canRead() && "BIL".indexOf(reader.peek()) >= 0) {
			char prefix = reader.read();
			if (character(';')) {
				Object array = array(prefix);
				if (array != null) {
					return array;
				}
				reader.setCursor(start);
				return null;
			}
		}
		reader.setCursor(afterBracket);
		List<Object> elements = new ArrayList<>();
		if (!elements(elements, ']')) {
			reader.setCursor(start);
			return null;
		}
		return elements;
	}

	/** `[B;`, `[I;` or `[L;` and whole numbers of a type the array takes. */
	private Object array(char prefix) {
		List<IntegerLiteral> literals = new ArrayList<>();
		while (true) {
			int beforeElement = reader.getCursor();
			IntegerLiteral literal = integerLiteral();
			if (literal == null) {
				reader.setCursor(beforeElement);
				break;
			}
			literals.add(literal);
			int beforeComma = reader.getCursor();
			if (!character(',')) {
				reader.setCursor(beforeComma);
				break;
			}
		}
		if (!character(']')) {
			return null;
		}
		Type defaultType = prefix == 'B' ? Type.BYTE : prefix == 'I' ? Type.INT : Type.LONG;
		long[] values = new long[literals.size()];
		for (int i = 0; i < values.length; i++) {
			IntegerLiteral literal = literals.get(i);
			Type type = literal.type == null ? defaultType : literal.type;
			if (type.ordinal() > defaultType.ordinal()) {
				fail(reader.getCursor(), INVALID_ARRAY_ELEMENT_TYPE);
				return null;
			}
			Object number = make(literal, type);
			if (number == null) {
				return null;
			}
			values[i] = ((Number) number).longValue();
		}
		if (prefix == 'B') {
			byte[] bytes = new byte[values.length];
			for (int i = 0; i < values.length; i++) {
				bytes[i] = (byte) values[i];
			}
			return bytes;
		}
		if (prefix == 'I') {
			int[] ints = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				ints[i] = (int) values[i];
			}
			return ints;
		}
		return values;
	}
}
