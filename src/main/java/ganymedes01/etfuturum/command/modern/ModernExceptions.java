package ganymedes01.etfuturum.command.modern;

import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

/** Brigadier's built-in parse errors with modern's text (the lang keys modern uses), spec §40.2. */
public final class ModernExceptions implements BuiltInExceptionProvider {

	private static final Dynamic2CommandExceptionType DOUBLE_TOO_LOW = tooFar("argument.double.low");
	private static final Dynamic2CommandExceptionType DOUBLE_TOO_HIGH = tooFar("argument.double.big");
	private static final Dynamic2CommandExceptionType FLOAT_TOO_LOW = tooFar("argument.float.low");
	private static final Dynamic2CommandExceptionType FLOAT_TOO_HIGH = tooFar("argument.float.big");
	private static final Dynamic2CommandExceptionType INTEGER_TOO_LOW = tooFar("argument.integer.low");
	private static final Dynamic2CommandExceptionType INTEGER_TOO_HIGH = tooFar("argument.integer.big");
	private static final Dynamic2CommandExceptionType LONG_TOO_LOW = tooFar("argument.long.low");
	private static final Dynamic2CommandExceptionType LONG_TOO_HIGH = tooFar("argument.long.big");
	private static final DynamicCommandExceptionType LITERAL_INCORRECT = dynamic("argument.literal.incorrect");
	private static final SimpleCommandExceptionType EXPECTED_START_OF_QUOTE = simple("parsing.quote.expected.start");
	private static final SimpleCommandExceptionType EXPECTED_END_OF_QUOTE = simple("parsing.quote.expected.end");
	private static final DynamicCommandExceptionType INVALID_ESCAPE = dynamic("parsing.quote.escape");
	private static final DynamicCommandExceptionType INVALID_BOOL = dynamic("parsing.bool.invalid");
	private static final DynamicCommandExceptionType INVALID_INT = dynamic("parsing.int.invalid");
	private static final SimpleCommandExceptionType EXPECTED_INT = simple("parsing.int.expected");
	private static final DynamicCommandExceptionType INVALID_LONG = dynamic("parsing.long.invalid");
	private static final SimpleCommandExceptionType EXPECTED_LONG = simple("parsing.long.expected");
	private static final DynamicCommandExceptionType INVALID_DOUBLE = dynamic("parsing.double.invalid");
	private static final SimpleCommandExceptionType EXPECTED_DOUBLE = simple("parsing.double.expected");
	private static final DynamicCommandExceptionType INVALID_FLOAT = dynamic("parsing.float.invalid");
	private static final SimpleCommandExceptionType EXPECTED_FLOAT = simple("parsing.float.expected");
	private static final SimpleCommandExceptionType EXPECTED_BOOL = simple("parsing.bool.expected");
	private static final DynamicCommandExceptionType EXPECTED_SYMBOL = dynamic("parsing.expected");
	private static final SimpleCommandExceptionType UNKNOWN_COMMAND = simple("command.unknown.command");
	private static final SimpleCommandExceptionType UNKNOWN_ARGUMENT = simple("command.unknown.argument");
	private static final SimpleCommandExceptionType EXPECTED_ARGUMENT_SEPARATOR = simple("command.expected.separator");
	private static final DynamicCommandExceptionType PARSE_EXCEPTION = dynamic("command.exception");

	/** Replaces Brigadier's English messages; both sides, once. */
	public static void install() {
		CommandSyntaxException.BUILT_IN_EXCEPTIONS = new ModernExceptions();
	}

	private static SimpleCommandExceptionType simple(String key) {
		return new SimpleCommandExceptionType(ComponentMessage.translatable(key));
	}

	private static DynamicCommandExceptionType dynamic(String key) {
		return new DynamicCommandExceptionType(value -> ComponentMessage.translatable(key, value));
	}

	/** Brigadier passes the value found and then the bound; the texts name the bound first. */
	private static Dynamic2CommandExceptionType tooFar(String key) {
		return new Dynamic2CommandExceptionType((found, bound) -> ComponentMessage.translatable(key, bound, found));
	}

	@Override
	public Dynamic2CommandExceptionType doubleTooLow() {
		return DOUBLE_TOO_LOW;
	}

	@Override
	public Dynamic2CommandExceptionType doubleTooHigh() {
		return DOUBLE_TOO_HIGH;
	}

	@Override
	public Dynamic2CommandExceptionType floatTooLow() {
		return FLOAT_TOO_LOW;
	}

	@Override
	public Dynamic2CommandExceptionType floatTooHigh() {
		return FLOAT_TOO_HIGH;
	}

	@Override
	public Dynamic2CommandExceptionType integerTooLow() {
		return INTEGER_TOO_LOW;
	}

	@Override
	public Dynamic2CommandExceptionType integerTooHigh() {
		return INTEGER_TOO_HIGH;
	}

	@Override
	public Dynamic2CommandExceptionType longTooLow() {
		return LONG_TOO_LOW;
	}

	@Override
	public Dynamic2CommandExceptionType longTooHigh() {
		return LONG_TOO_HIGH;
	}

	@Override
	public DynamicCommandExceptionType literalIncorrect() {
		return LITERAL_INCORRECT;
	}

	@Override
	public SimpleCommandExceptionType readerExpectedStartOfQuote() {
		return EXPECTED_START_OF_QUOTE;
	}

	@Override
	public SimpleCommandExceptionType readerExpectedEndOfQuote() {
		return EXPECTED_END_OF_QUOTE;
	}

	@Override
	public DynamicCommandExceptionType readerInvalidEscape() {
		return INVALID_ESCAPE;
	}

	@Override
	public DynamicCommandExceptionType readerInvalidBool() {
		return INVALID_BOOL;
	}

	@Override
	public DynamicCommandExceptionType readerInvalidInt() {
		return INVALID_INT;
	}

	@Override
	public SimpleCommandExceptionType readerExpectedInt() {
		return EXPECTED_INT;
	}

	@Override
	public DynamicCommandExceptionType readerInvalidLong() {
		return INVALID_LONG;
	}

	@Override
	public SimpleCommandExceptionType readerExpectedLong() {
		return EXPECTED_LONG;
	}

	@Override
	public DynamicCommandExceptionType readerInvalidDouble() {
		return INVALID_DOUBLE;
	}

	@Override
	public SimpleCommandExceptionType readerExpectedDouble() {
		return EXPECTED_DOUBLE;
	}

	@Override
	public DynamicCommandExceptionType readerInvalidFloat() {
		return INVALID_FLOAT;
	}

	@Override
	public SimpleCommandExceptionType readerExpectedFloat() {
		return EXPECTED_FLOAT;
	}

	@Override
	public SimpleCommandExceptionType readerExpectedBool() {
		return EXPECTED_BOOL;
	}

	@Override
	public DynamicCommandExceptionType readerExpectedSymbol() {
		return EXPECTED_SYMBOL;
	}

	@Override
	public SimpleCommandExceptionType dispatcherUnknownCommand() {
		return UNKNOWN_COMMAND;
	}

	@Override
	public SimpleCommandExceptionType dispatcherUnknownArgument() {
		return UNKNOWN_ARGUMENT;
	}

	@Override
	public SimpleCommandExceptionType dispatcherExpectedArgumentSeparator() {
		return EXPECTED_ARGUMENT_SEPARATOR;
	}

	@Override
	public DynamicCommandExceptionType dispatcherParseException() {
		return PARSE_EXCEPTION;
	}
}
