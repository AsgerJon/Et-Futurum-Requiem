package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.nbt.SnbtReader;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/** A compound in modern's SNBT (spec §45.1, §47.1), read into plain values. */
public final class CompoundTagArgument implements ArgumentType<Map<String, Object>> {

	private static final Collection<String> EXAMPLES = Arrays.asList("{}", "{foo:bar}");
	private static final SimpleCommandExceptionType EXPECTED_COMPOUND = new SimpleCommandExceptionType(ComponentMessage.translatable("argument.nbt.expected.compound"));

	private CompoundTagArgument() {
	}

	public static CompoundTagArgument compoundTag() {
		return new CompoundTagArgument();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getCompoundTag(CommandContext<?> context, String name) {
		return context.getArgument(name, Map.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> parse(StringReader reader) throws CommandSyntaxException {
		Object value = SnbtReader.read(reader);
		if (!(value instanceof Map)) {
			throw EXPECTED_COMPOUND.createWithContext(reader);
		}
		return (Map<String, Object>) value;
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
