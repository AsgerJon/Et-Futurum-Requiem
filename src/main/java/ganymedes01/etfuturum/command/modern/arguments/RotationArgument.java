package ganymedes01.etfuturum.command.modern.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;

import java.util.Arrays;
import java.util.Collection;

/** A rotation (spec §51.1): yaw then pitch, each a number or `~` and a number (relative to the target's own). */
public final class RotationArgument implements ArgumentType<RotationArgument.Rotation> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "~-5 ~5");
	private static final SimpleCommandExceptionType INCOMPLETE = new SimpleCommandExceptionType(ComponentMessage.translatable("argument.rotation.incomplete"));

	/** Yaw and pitch, each absolute or relative. */
	public static final class Rotation {
		final float yaw;
		final float pitch;
		final boolean yawRelative;
		final boolean pitchRelative;

		private Rotation(float yaw, boolean yawRelative, float pitch, boolean pitchRelative) {
			this.yaw = yaw;
			this.pitch = pitch;
			this.yawRelative = yawRelative;
			this.pitchRelative = pitchRelative;
		}

		/** {yaw, pitch} against a target's own rotation. */
		public float[] apply(float ownYaw, float ownPitch) {
			return new float[]{yawRelative ? ownYaw + yaw : yaw, pitchRelative ? ownPitch + pitch : pitch};
		}
	}

	private RotationArgument() {
	}

	public static RotationArgument rotation() {
		return new RotationArgument();
	}

	public static Rotation getRotation(CommandContext<ModernCommandSource> context, String name) {
		return context.getArgument(name, Rotation.class);
	}

	@Override
	public Rotation parse(StringReader reader) throws CommandSyntaxException {
		int start = reader.getCursor();
		if (!reader.canRead()) {
			throw INCOMPLETE.createWithContext(reader);
		}
		boolean yawRelative = reader.peek() == '~';
		float yaw = read(reader, yawRelative);
		if (!reader.canRead() || reader.peek() != ' ') {
			reader.setCursor(start);
			throw INCOMPLETE.createWithContext(reader);
		}
		reader.skip();
		boolean pitchRelative = reader.canRead() && reader.peek() == '~';
		float pitch = read(reader, pitchRelative);
		return new Rotation(yaw, yawRelative, pitch, pitchRelative);
	}

	private static float read(StringReader reader, boolean relative) throws CommandSyntaxException {
		if (relative) {
			reader.skip();
			return reader.canRead() && reader.peek() != ' ' ? reader.readFloat() : 0.0F;
		}
		return reader.readFloat();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
