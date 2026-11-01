package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.Suggest;
import ganymedes01.etfuturum.command.modern.SuggestionSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A position (spec §47.1): three world coordinates (a number, or `~` and an optional number) or three local ones (`^`
 * and an optional number: left, up, forwards). With centre correction, a whole x or z without `~` is the block's
 * centre.
 */
public final class Vec3Argument implements ArgumentType<Vec3Argument.Coordinates> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5");
	static final SimpleCommandExceptionType NOT_COMPLETE = simple("argument.pos3d.incomplete");
	static final SimpleCommandExceptionType MIXED_TYPE = simple("argument.pos.mixed");
	static final SimpleCommandExceptionType EXPECTED_DOUBLE = simple("argument.pos.missing.double");
	static final SimpleCommandExceptionType EXPECTED_INT = simple("argument.pos.missing.int");

	private final boolean centreCorrect;

	private Vec3Argument(boolean centreCorrect) {
		this.centreCorrect = centreCorrect;
	}

	public static Vec3Argument vec3() {
		return new Vec3Argument(true);
	}

	public static Vec3Argument vec3(boolean centreCorrect) {
		return new Vec3Argument(centreCorrect);
	}

	public boolean centreCorrect() {
		return centreCorrect;
	}

	private static SimpleCommandExceptionType simple(String key) {
		return new SimpleCommandExceptionType(ComponentMessage.translatable(key));
	}

	public static Vec3 getVec3(CommandContext<ModernCommandSource> context, String name) {
		return context.getArgument(name, Coordinates.class).position(context.getSource());
	}

	@Override
	public Coordinates parse(StringReader reader) throws CommandSyntaxException {
		return parse(reader, centreCorrect, false);
	}

	/** With {@code integers} a coordinate without `~` must be a whole number, as for a block position (§49.1). */
	static Coordinates parse(StringReader reader, boolean centreCorrect, boolean integers) throws CommandSyntaxException {
		return reader.canRead() && reader.peek() == '^' ? Local.parse(reader) : World.parse(reader, centreCorrect, integers);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return suggest(context, builder, this::parses);
	}

	/**
	 * Modern's position suggestions: with nothing typed, the looked-at block (or ~ ~ ~) as x, x y and x y z; `^`
	 * suggests ^ ^ ^; once part is typed, the rest from the same coordinates, as long as the whole parses.
	 */
	static <S> CompletableFuture<Suggestions> suggest(CommandContext<S> context, SuggestionsBuilder builder, java.util.function.Predicate<String> parses) {
		if (!(context.getSource() instanceof SuggestionSource source)) {
			return Suggestions.empty();
		}
		String typed = builder.getRemaining();
		String[] coordinates = !typed.isEmpty() && typed.charAt(0) == '^' ? new String[]{"^", "^", "^"} : source.absoluteCoordinates();
		List<String> result = new ArrayList<>();
		if (typed.isEmpty()) {
			String full = coordinates[0] + " " + coordinates[1] + " " + coordinates[2];
			if (parses.test(full)) {
				result.add(coordinates[0]);
				result.add(coordinates[0] + " " + coordinates[1]);
				result.add(full);
			}
		} else {
			String[] fields = typed.split(" ");
			if (fields.length == 1) {
				String full = fields[0] + " " + coordinates[1] + " " + coordinates[2];
				if (parses.test(full)) {
					result.add(fields[0] + " " + coordinates[1]);
					result.add(full);
				}
			} else if (fields.length == 2) {
				String full = fields[0] + " " + fields[1] + " " + coordinates[2];
				if (parses.test(full)) {
					result.add(full);
				}
			}
		}
		return Suggest.matching(result, builder);
	}

	private boolean parses(String text) {
		try {
			StringReader reader = new StringReader(text);
			parse(reader);
			return !reader.canRead();
		} catch (CommandSyntaxException e) {
			return false;
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	/** Parsed coordinates, made into a position against the source when the command runs. */
	public interface Coordinates {
		Vec3 position(ModernCommandSource source);
	}

	/** Absolute or `~` relative to the source's position, each axis on its own. */
	private static final class World implements Coordinates {
		private final boolean[] relative;
		private final double[] values;

		private World(boolean[] relative, double[] values) {
			this.relative = relative;
			this.values = values;
		}

		@Override
		public Vec3 position(ModernCommandSource source) {
			Vec3 origin = source.position();
			return Vec3.createVectorHelper(get(0, origin.xCoord), get(1, origin.yCoord), get(2, origin.zCoord));
		}

		private double get(int axis, double origin) {
			return relative[axis] ? values[axis] + origin : values[axis];
		}

		private static World parse(StringReader reader, boolean centreCorrect, boolean integers) throws CommandSyntaxException {
			int start = reader.getCursor();
			boolean[] relative = new boolean[3];
			double[] values = new double[3];
			for (int axis = 0; axis < 3; axis++) {
				if (axis > 0) {
					if (!reader.canRead() || reader.peek() != ' ') {
						reader.setCursor(start);
						throw NOT_COMPLETE.createWithContext(reader);
					}
					reader.skip();
				}
				if (reader.canRead() && reader.peek() == '^') {
					throw MIXED_TYPE.createWithContext(reader);
				}
				if (!reader.canRead()) {
					throw (integers ? EXPECTED_INT : EXPECTED_DOUBLE).createWithContext(reader);
				}
				relative[axis] = reader.peek() == '~';
				if (relative[axis]) {
					reader.skip();
				}
				int numberStart = reader.getCursor();
				boolean hasNumber = reader.canRead() && reader.peek() != ' ';
				if (!hasNumber && !relative[axis]) {
					throw (integers ? EXPECTED_INT : EXPECTED_DOUBLE).createWithContext(reader);
				}
				double value = !hasNumber ? 0.0D : integers && !relative[axis] ? reader.readInt() : reader.readDouble();
				String number = reader.getString().substring(numberStart, reader.getCursor());
				if (!relative[axis] && centreCorrect && axis != 1 && !number.contains(".")) {
					value += 0.5D;
				}
				values[axis] = value;
			}
			return new World(relative, values);
		}
	}

	/** `^` coordinates: left, up and forwards from the source's feet, by its rotation. */
	private static final class Local implements Coordinates {
		private final double left;
		private final double up;
		private final double forwards;

		private Local(double left, double up, double forwards) {
			this.left = left;
			this.up = up;
			this.forwards = forwards;
		}

		@Override
		public Vec3 position(ModernCommandSource source) {
			Vec3 origin = source.position();
			float[] rotation = source.rotation();
			float yCos = MathHelper.cos((rotation[1] + 90.0F) * (float) Math.PI / 180.0F);
			float ySin = MathHelper.sin((rotation[1] + 90.0F) * (float) Math.PI / 180.0F);
			float xCos = MathHelper.cos(-rotation[0] * (float) Math.PI / 180.0F);
			float xSin = MathHelper.sin(-rotation[0] * (float) Math.PI / 180.0F);
			float xCosUp = MathHelper.cos((-rotation[0] + 90.0F) * (float) Math.PI / 180.0F);
			float xSinUp = MathHelper.sin((-rotation[0] + 90.0F) * (float) Math.PI / 180.0F);
			double[] forwardsVector = {yCos * xCos, xSin, ySin * xCos};
			double[] upVector = {yCos * xCosUp, xSinUp, ySin * xCosUp};
			// left = -(forwards × up)
			double[] leftVector = {
				-(forwardsVector[1] * upVector[2] - forwardsVector[2] * upVector[1]),
				-(forwardsVector[2] * upVector[0] - forwardsVector[0] * upVector[2]),
				-(forwardsVector[0] * upVector[1] - forwardsVector[1] * upVector[0])};
			return Vec3.createVectorHelper(
				origin.xCoord + forwardsVector[0] * forwards + upVector[0] * up + leftVector[0] * left,
				origin.yCoord + forwardsVector[1] * forwards + upVector[1] * up + leftVector[1] * left,
				origin.zCoord + forwardsVector[2] * forwards + upVector[2] * up + leftVector[2] * left);
		}

		private static Local parse(StringReader reader) throws CommandSyntaxException {
			int start = reader.getCursor();
			double left = read(reader, start);
			if (!reader.canRead() || reader.peek() != ' ') {
				reader.setCursor(start);
				throw NOT_COMPLETE.createWithContext(reader);
			}
			reader.skip();
			double up = read(reader, start);
			if (!reader.canRead() || reader.peek() != ' ') {
				reader.setCursor(start);
				throw NOT_COMPLETE.createWithContext(reader);
			}
			reader.skip();
			return new Local(left, up, read(reader, start));
		}

		private static double read(StringReader reader, int start) throws CommandSyntaxException {
			if (!reader.canRead()) {
				throw EXPECTED_DOUBLE.createWithContext(reader);
			}
			if (reader.peek() != '^') {
				reader.setCursor(start);
				throw MIXED_TYPE.createWithContext(reader);
			}
			reader.skip();
			return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0D;
		}
	}
}
