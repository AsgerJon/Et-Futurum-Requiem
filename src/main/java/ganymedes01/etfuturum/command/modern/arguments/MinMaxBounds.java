package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;

import java.util.function.Function;
import java.util.function.Supplier;

/** Ranges in selector options (spec §43.2): `5`, `1..5`, `..5` or `1..`; a missing end is null. */
public final class MinMaxBounds {

	static final SimpleCommandExceptionType EMPTY = new SimpleCommandExceptionType(ComponentMessage.translatable("argument.range.empty"));
	static final SimpleCommandExceptionType SWAPPED = new SimpleCommandExceptionType(ComponentMessage.translatable("argument.range.swapped"));

	private MinMaxBounds() {
	}

	/** A decimal range; also kept squared, for comparing with squared distances. */
	public static final class Doubles {
		public final Double min;
		public final Double max;
		private final Double minSqr;
		private final Double maxSqr;

		private Doubles(Double min, Double max) {
			this.min = min;
			this.max = max;
			this.minSqr = min == null ? null : min * min;
			this.maxSqr = max == null ? null : max * max;
		}

		public boolean matchesSqr(double valueSqr) {
			return (minSqr == null || minSqr <= valueSqr) && (maxSqr == null || maxSqr >= valueSqr);
		}

		public static Doubles fromReader(StringReader reader) throws CommandSyntaxException {
			int start = reader.getCursor();
			Object[] ends = read(reader, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble);
			Doubles bounds = new Doubles((Double) ends[0], (Double) ends[1]);
			if (bounds.min != null && bounds.max != null && bounds.min > bounds.max) {
				reader.setCursor(start);
				throw SWAPPED.createWithContext(reader);
			}
			return bounds;
		}
	}

	/** A whole-number range. */
	public static final class Ints {
		public final Integer min;
		public final Integer max;

		private Ints(Integer min, Integer max) {
			this.min = min;
			this.max = max;
		}

		public boolean matches(int value) {
			return (min == null || min <= value) && (max == null || max >= value);
		}

		public static Ints fromReader(StringReader reader) throws CommandSyntaxException {
			int start = reader.getCursor();
			Object[] ends = read(reader, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt);
			Ints bounds = new Ints((Integer) ends[0], (Integer) ends[1]);
			if (bounds.min != null && bounds.max != null && bounds.min > bounds.max) {
				reader.setCursor(start);
				throw SWAPPED.createWithContext(reader);
			}
			return bounds;
		}
	}

	/** Degrees; the ends may be in either order, since rotation ranges can run around. */
	public static final class FloatDegrees {
		public final Float min;
		public final Float max;

		private FloatDegrees(Float min, Float max) {
			this.min = min;
			this.max = max;
		}

		public static FloatDegrees fromReader(StringReader reader) throws CommandSyntaxException {
			Object[] ends = read(reader, Float::parseFloat, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidFloat);
			return new FloatDegrees((Float) ends[0], (Float) ends[1]);
		}
	}

	/** The two ends, {min, max}; a single number is both. Any error points at the start of the range. */
	private static <T> Object[] read(StringReader reader, Function<String, T> converter, Supplier<DynamicCommandExceptionType> invalid)
		throws CommandSyntaxException {
		if (!reader.canRead()) {
			throw EMPTY.createWithContext(reader);
		}
		int start = reader.getCursor();
		try {
			T min = readNumber(reader, converter, invalid);
			T max;
			if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
				reader.skip();
				reader.skip();
				max = readNumber(reader, converter, invalid);
			} else {
				max = min;
			}
			if (min == null && max == null) {
				throw EMPTY.createWithContext(reader);
			}
			return new Object[]{min, max};
		} catch (CommandSyntaxException e) {
			reader.setCursor(start);
			throw new CommandSyntaxException(e.getType(), e.getRawMessage(), e.getInput(), start);
		}
	}

	private static <T> T readNumber(StringReader reader, Function<String, T> converter, Supplier<DynamicCommandExceptionType> invalid)
		throws CommandSyntaxException {
		int start = reader.getCursor();
		while (reader.canRead() && isNumberChar(reader)) {
			reader.skip();
		}
		String number = reader.getString().substring(start, reader.getCursor());
		if (number.isEmpty()) {
			return null;
		}
		try {
			return converter.apply(number);
		} catch (NumberFormatException e) {
			throw invalid.get().createWithContext(reader, number);
		}
	}

	/** Digits, `-`, and `.` unless another `.` follows it (that starts `..`). */
	private static boolean isNumberChar(StringReader reader) {
		char c = reader.peek();
		if (c >= '0' && c <= '9' || c == '-') {
			return true;
		}
		return c == '.' && !(reader.canRead(2) && reader.peek(1) == '.');
	}
}
