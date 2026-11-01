package ganymedes01.etfuturum.command.modern.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ganymedes01.etfuturum.command.modern.ComponentMessage;
import ganymedes01.etfuturum.command.modern.ModernCommand;
import ganymedes01.etfuturum.command.modern.ModernCommandSource;
import ganymedes01.etfuturum.command.modern.arguments.BlockInput;
import ganymedes01.etfuturum.command.modern.arguments.BlockPos;
import ganymedes01.etfuturum.command.modern.arguments.BlockPosArgument;
import ganymedes01.etfuturum.command.modern.arguments.BlockPredicateArgument;
import ganymedes01.etfuturum.command.modern.arguments.BlockStateArgument;
import ganymedes01.etfuturum.command.modern.arguments.BlockTest;
import ganymedes01.etfuturum.command.modern.arguments.Vec3Argument;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Modern's /fill (spec §49.1); 1.7.10 has none. */
public final class FillCommand extends ModernCommand {

	/** The default of modern's block-modifications rule; 1.7.10 has no such rule (spec §49.3). */
	static final int LIMIT = 32768;
	static final Dynamic2CommandExceptionType TOO_BIG = new Dynamic2CommandExceptionType(
		(max, count) -> ComponentMessage.translatable("commands.fill.toobig", max, count));
	private static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(ComponentMessage.translatable("commands.fill.failed"));

	private enum Mode {REPLACE, OUTLINE, HOLLOW, DESTROY}

	/** How a mode's command reads its filter: none, the keep rule, or the argument. */
	@FunctionalInterface
	private interface Filter {
		BlockTest get(CommandContext<ModernCommandSource> context);
	}

	public FillCommand() {
		super("fill", 2);
	}

	@Override
	public void register(CommandDispatcher<ModernCommandSource> dispatcher) {
		Filter none = c -> null;
		dispatcher.register(LiteralArgumentBuilder.<ModernCommandSource>literal("fill")
			.requires(requirement())
			.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("from", BlockPosArgument.blockPos())
				.then(RequiredArgumentBuilder.<ModernCommandSource, Vec3Argument.Coordinates>argument("to", BlockPosArgument.blockPos())
					.then(withModes(RequiredArgumentBuilder.<ModernCommandSource, BlockInput>argument("block", BlockStateArgument.block()), none)
						.then(LiteralArgumentBuilder.<ModernCommandSource>literal("replace")
							.executes(c -> fill(c, Mode.REPLACE, null, false))
							.then(withModes(RequiredArgumentBuilder.<ModernCommandSource, BlockTest>argument("filter", BlockPredicateArgument.blockPredicate()),
								c -> BlockPredicateArgument.getBlockPredicate(c, "filter"))))
						.then(LiteralArgumentBuilder.<ModernCommandSource>literal("keep")
							.executes(c -> fill(c, Mode.REPLACE, (world, x, y, z) -> world.isAirBlock(x, y, z), false)))))));
	}

	/** The node runs as replace; after it outline, hollow, destroy and strict. */
	private <T extends ArgumentBuilder<ModernCommandSource, T>> T withModes(T builder, Filter filter) {
		return builder.executes(c -> fill(c, Mode.REPLACE, filter.get(c), false))
			.then(LiteralArgumentBuilder.<ModernCommandSource>literal("outline").executes(c -> fill(c, Mode.OUTLINE, filter.get(c), false)))
			.then(LiteralArgumentBuilder.<ModernCommandSource>literal("hollow").executes(c -> fill(c, Mode.HOLLOW, filter.get(c), false)))
			.then(LiteralArgumentBuilder.<ModernCommandSource>literal("destroy").executes(c -> fill(c, Mode.DESTROY, filter.get(c), false)))
			.then(LiteralArgumentBuilder.<ModernCommandSource>literal("strict").executes(c -> fill(c, Mode.REPLACE, filter.get(c), true)));
	}

	private int fill(CommandContext<ModernCommandSource> context, Mode mode, BlockTest filter, boolean strict) throws CommandSyntaxException {
		ModernCommandSource source = context.getSource();
		BlockPos from = BlockPosArgument.getLoadedBlockPos(context, "from");
		BlockPos to = BlockPosArgument.getLoadedBlockPos(context, "to");
		BlockInput block = BlockStateArgument.getBlock(context, "block");
		int minX = Math.min(from.x(), to.x());
		int minY = Math.min(from.y(), to.y());
		int minZ = Math.min(from.z(), to.z());
		int maxX = Math.max(from.x(), to.x());
		int maxY = Math.max(from.y(), to.y());
		int maxZ = Math.max(from.z(), to.z());
		long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
		if (volume > LIMIT) {
			throw TOO_BIG.create(LIMIT, volume);
		}
		World world = source.world();
		BlockInput air = BlockInput.of("minecraft:air");
		List<BlockPos> changed = new ArrayList<>();
		int count = 0;
		for (int z = minZ; z <= maxZ; z++) {
			for (int y = minY; y <= maxY; y++) {
				for (int x = minX; x <= maxX; x++) {
					if (filter != null && !filter.test(world, x, y, z)) {
						continue;
					}
					boolean affected = mode == Mode.DESTROY && world.func_147480_a(x, y, z, true);
					boolean inside = x != minX && x != maxX && y != minY && y != maxY && z != minZ && z != maxZ;
					BlockInput target = !inside ? block : mode == Mode.OUTLINE ? null : mode == Mode.HOLLOW ? air : block;
					if (target != null && target.place(world, x, y, z)) {
						if (!strict) {
							changed.add(new BlockPos(x, y, z));
						}
						count++;
					} else if (affected) {
						count++;
					}
				}
			}
		}
		// the neighbours hear of it once everything is in, as in modern
		for (BlockPos pos : changed) {
			world.notifyBlockChange(pos.x(), pos.y(), pos.z(), world.getBlock(pos.x(), pos.y(), pos.z()));
		}
		if (count == 0) {
			throw FAILED.create();
		}
		source.sendSuccess(legacyView(), true, "commands.fill.success", count);
		return count;
	}
}
