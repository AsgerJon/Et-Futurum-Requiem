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
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * A block position (spec §49.1): coordinates as for a position, but a number without `~` is a whole number; the
 * position is the block containing the point.
 */
public final class BlockPosArgument implements ArgumentType<Vec3Argument.Coordinates> {

	private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");
	private static final SimpleCommandExceptionType NOT_LOADED = new SimpleCommandExceptionType(ComponentMessage.translatable("argument.pos.unloaded"));
	private static final SimpleCommandExceptionType OUT_OF_WORLD = new SimpleCommandExceptionType(ComponentMessage.translatable("argument.pos.outofworld"));

	private BlockPosArgument() {
	}

	public static BlockPosArgument blockPos() {
		return new BlockPosArgument();
	}

	public static BlockPos getBlockPos(CommandContext<ModernCommandSource> context, String name) {
		Vec3 point = context.getArgument(name, Vec3Argument.Coordinates.class).position(context.getSource());
		return new BlockPos(MathHelper.floor_double(point.xCoord), MathHelper.floor_double(point.yCoord), MathHelper.floor_double(point.zCoord));
	}

	/** The position, which must be in a loaded chunk and within the world's height. */
	public static BlockPos getLoadedBlockPos(CommandContext<ModernCommandSource> context, String name) throws CommandSyntaxException {
		BlockPos pos = getBlockPos(context, name);
		World world = context.getSource().world();
		if (!world.getChunkProvider().chunkExists(pos.x() >> 4, pos.z() >> 4)) {
			throw NOT_LOADED.create();
		}
		if (pos.y() < 0 || pos.y() >= world.getHeight()) {
			throw OUT_OF_WORLD.create();
		}
		return pos;
	}

	@Override
	public Vec3Argument.Coordinates parse(StringReader reader) throws CommandSyntaxException {
		return Vec3Argument.parse(reader, false, true);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return Vec3Argument.suggest(context, builder, text -> {
			try {
				StringReader reader = new StringReader(text);
				parse(reader);
				return !reader.canRead();
			} catch (CommandSyntaxException e) {
				return false;
			}
		});
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
