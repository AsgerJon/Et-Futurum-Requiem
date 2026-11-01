package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.Suggest;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** A time (spec §51.1): a number and a unit, `t` ticks, `s` seconds, `d` days, none ticks; as ticks. */
public final class TimeArgument implements ArgumentType<Integer> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0d", "0s", "0t", "0");
	private static final SimpleCommandExceptionType INVALID_UNIT = new SimpleCommandExceptionType(ComponentMessage.translatable("argument.time.invalid_unit"));
	private static final Dynamic2CommandExceptionType TOO_LOW = new Dynamic2CommandExceptionType(
		(found, minimum) -> ComponentMessage.translatable("argument.time.tick_count_too_low", minimum, found));
	private static final Map<String, Integer> UNITS = new LinkedHashMap<>();

	static {
		UNITS.put("d", 24000);
		UNITS.put("s", 20);
		UNITS.put("t", 1);
		UNITS.put("", 1);
	}

	private final int minimum;

	private TimeArgument(int minimum) {
		this.minimum = minimum;
	}

	public static TimeArgument time(int minimum) {
		return new TimeArgument(minimum);
	}

	public int minimum() {
		return minimum;
	}

	public static int getTime(CommandContext<?> context, String name) {
		return context.getArgument(name, Integer.class);
	}

	@Override
	public Integer parse(StringReader reader) throws CommandSyntaxException {
		float value = reader.readFloat();
		String unit = reader.readUnquotedString();
		Integer factor = UNITS.get(unit);
		if (factor == null) {
			throw INVALID_UNIT.createWithContext(reader);
		}
		int ticks = Math.round(value * factor);
		if (ticks < minimum) {
			throw TOO_LOW.createWithContext(reader, ticks, minimum);
		}
		return ticks;
	}

	/** The units after a number. */
	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		StringReader reader = new StringReader(builder.getRemaining());
		try {
			reader.readFloat();
		} catch (CommandSyntaxException e) {
			return builder.buildFuture();
		}
		return Suggest.matching(UNITS.keySet(), builder.createOffset(builder.getStart() + reader.getCursor()));
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
